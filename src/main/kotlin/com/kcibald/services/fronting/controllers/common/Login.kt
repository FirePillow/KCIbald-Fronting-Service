package com.kcibald.services.fronting.controllers.common

import com.kcibald.objects.User
import com.kcibald.services.fronting.controllers.MasterConfigSpec.Authentication
import com.kcibald.services.fronting.objs.entries.FancyEntry
import com.kcibald.services.fronting.objs.entries.Path
import com.kcibald.services.fronting.objs.entries.UnsafeHTMLContentEntry
import com.kcibald.services.fronting.objs.responses.EmptyResponse
import com.kcibald.services.fronting.objs.responses.InternalErrorResponse
import com.kcibald.services.fronting.objs.responses.Response
import com.kcibald.services.fronting.utils.*
import com.kcibald.services.user.AuthenticationClient
import com.kcibald.services.user.AuthenticationResult
import com.kcibald.utils.d
import com.kcibald.utils.w
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.jwt.jwtOptionsOf
import org.apache.commons.validator.routines.EmailValidator
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.properties.Delegates

object Login : UnsafeHTMLContentEntry(), FancyEntry {
    private val logger = LoggerFactory.getLogger(Login::class.java)

    private var recaptchaScoreThreshold by Delegates.notNull<Double>()
    private lateinit var COOKIE_KEY: String
    private lateinit var authenticationClient: AuthenticationClient
    private lateinit var jwtAuthProvider: JWTAuth
    private var recaptchaClient: RecaptchaV3Client? = null

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        val vertx = sharedObjects.vertx
        val config = sharedObjects.config
        this.recaptchaClient = sharedObjects.recaptchaClient
        this.jwtAuthProvider = sharedObjects.jwtAuth

        this.recaptchaScoreThreshold = config[Authentication.RecaptchaThreshold]
        this.COOKIE_KEY = config[Authentication.CookieKey]
        this.authenticationClient = AuthenticationClient.createDefault(vertx)

        router
            .post("/login")
            .consumeJson()
            .handler((CookieHandler.create())::handle)
            .coroutineCoreHandler(::handleEvent)
    }

    private val emailValidator = EmailValidator.getInstance()
    private val passwordPattern = Pattern.compile("^(?=.*?[a-z])(?=.*?[0-9]).{8,20}\$")

    private suspend fun handleEvent(context: RoutingContext): Response {
        val requestObj = context.jsonObject

        val accountEmail: String = requestObj["account"] ?: incompleteRequest()
        val password: String = requestObj["password"] ?: incompleteRequest()

        logger.d { "account: $accountEmail attempts to login" }

        if (checkArguments(accountEmail, password)) {
            logger.d { "account: $accountEmail login attempt failed on pre-check" }
            return StandardAuthorizationFailureResponses.PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT
        }

        val captchaResult = checkRecaptchaIfEnabled(requestObj)
        logger.d { "recaptcha checking for account: $accountEmail, is $captchaResult" }
        if (captchaResult == RecaptchaResponse.VERIFIED_INVALID) {
            return StandardAuthorizationFailureResponses.PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT
        }

        return verifyCredit(accountEmail, password, captchaResult, context)
    }

    private suspend fun verifyCredit(
        accountEmail: String,
        password: String,
        captchaResult: RecaptchaResponse,
        context: RoutingContext
    ): Response =
        when (val verification = authenticationClient.verifyCredential(accountEmail, password)) {
            is AuthenticationResult.InvalidCredential, is AuthenticationResult.UserNotFound ->
                userNotFoundResponse(accountEmail)
            is AuthenticationResult.Banned ->
                bannedFailureResponse(verification)
            is AuthenticationResult.Success ->
                successResponse(captchaResult, verification.user, accountEmail, context)
            is AuthenticationResult.SystemError ->
                systemErrorResponse(verification)
        }

    private fun userNotFoundResponse(accountEmail: String): StandardAuthorizationFailureResponses {
        logger.d { "user $accountEmail was not found" }
        return StandardAuthorizationFailureResponses.PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT
    }

    private fun systemErrorResponse(verification: AuthenticationResult.SystemError): InternalErrorResponse {
        logger.w { "received system error from upstream service: authentication, message ${verification.message}" }
        return InternalErrorResponse
    }

    private fun successResponse(
        captchaResult: RecaptchaResponse,
        user: User,
        accountEmail: String,
        context: RoutingContext
    ): EmptyResponse {
        val cookie = makeCookie(captchaResult, user, accountEmail)
        context.addCookie(cookie)

        logger.debug("account: ${user.urlKey} successfully logged in")
        return EmptyResponse
    }

    private fun makeCookie(
        captchaResult: RecaptchaResponse,
        user: User,
        accountEmail: String
    ): Cookie {
        val expireInMinutes = calcuateExpireInMinutes(captchaResult)

        val token = generateJwtToken(user, accountEmail, expireInMinutes)

        val cookie = Cookie.cookie(COOKIE_KEY, token)
        cookie.setSecure(true)
        cookie.setMaxAge(TimeUnit.MINUTES.toSeconds(expireInMinutes.toLong()))
        cookie.domain = "kcibald.com"

        return cookie
    }

    private fun generateJwtToken(
        user: User,
        accountEmail: String,
        expireInMinutes: Int
    ): String = jwtAuthProvider.generateToken(
        generateCliaimsForUser(user),
        jwtOptionsOf(
            audience = listOf(user.urlKey, user.userName, accountEmail),
            expiresInMinutes = expireInMinutes,
            issuer = "kcibald-frontend"
        )
    )

    private fun generateCliaimsForUser(user: User): JsonObject {
        return jsonObjectOf(
            "username" to user.userName,
            "url_key" to user.urlKey,
            "user_avatar" to user.avatar
        )
    }

    private fun calcuateExpireInMinutes(captchaResult: RecaptchaResponse): Int {
        val expireInMinutes =
            if (captchaResult == RecaptchaResponse.VERIFICATION_FAILED)
                5
            else
                TimeUnit.DAYS.toMinutes(30).toInt()
        return expireInMinutes
    }

    private fun checkArguments(account: String, password: String) =
        !emailValidator.isValid(account) || !passwordPattern.matcher(password).matches()

    private fun bannedFailureResponse(result: AuthenticationResult.Banned): Response = object : Response {
        override fun apply(response: HttpServerResponse) {
            val timeToUnban = if (result.duration < 0) -1 else result.timeBanned + result.duration
            val body = json {
                obj(
                    "success" to false,
                    "type" to "USER_BANNED",
                    "banned_type" to obj(
                        "message" to result.message,
                        "time_of_unban" to timeToUnban
                    )
                )
            }
            response
                .setStatusCode(403)
                .putHeader("WWW-Authenticate", "FormBased")
                .end(body)
        }
    }

    private enum class StandardAuthorizationFailureResponses(val type: String) : Response {
        PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT("PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT"),
        CAPTCHA_VERIFICATION_FAILED("CAPTCHA_VERIFICATION_FAILED");

        override fun apply(response: HttpServerResponse) {
            val body = json {
                obj(
                    "success" to false,
                    "type" to type
                )
            }
            response
                .setStatusCode(401)
                .putHeader("WWW-Authenticate", "FormBased")
                .end(body)
        }
    }

    private const val maximumRecaptchaTrial = 2

    private suspend fun checkRecaptchaIfEnabled(requestObj: JsonObject): RecaptchaResponse {
        val reclient = recaptchaClient

        if (reclient == null) {
            logger.warn("recaptcha checking has been disabled, THIS IS NOT SUITABLE FOR PRODUCTION")
            return RecaptchaResponse.VERIFIED_PASS
        }

        val recaptchaToken: String = requestObj["captcha"] ?: incompleteRequest()
        logger.d { "checking recaptcha $recaptchaToken" }
        for (retryCount in 1..maximumRecaptchaTrial) {
            try {
//                it is not blocking call
                @Suppress("BlockingMethodInNonBlockingContext")
                val score = reclient.getVerifyScore(recaptchaToken)
                return if (score < recaptchaScoreThreshold) {
                    RecaptchaResponse.VERIFIED_INVALID
                } else {
                    RecaptchaResponse.VERIFIED_PASS
                }
            } catch (e: IOException) {
                logger.info("IOException when validating recaptcha! $e", e)
            }
        }
        logger.warn("recaptcha validation failed after $maximumRecaptchaTrial retries")
        return RecaptchaResponse.VERIFICATION_FAILED
    }

    private enum class RecaptchaResponse {
        VERIFIED_PASS,
        VERIFIED_INVALID,
        VERIFICATION_FAILED
    }

    override fun staticEntryPath(): List<Path> = listOf("/login")

}

