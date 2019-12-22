package com.kcibald.services.fronting.controllers.common

import com.kcibald.objects.User
import com.kcibald.services.fronting.controllers.MasterConfigSpec.Authentication
import com.kcibald.services.fronting.objs.entries.FancyEntry
import com.kcibald.services.fronting.objs.entries.Path
import com.kcibald.services.fronting.objs.entries.UnsafeHTMLContentEntry
import com.kcibald.services.fronting.objs.responses.*
import com.kcibald.services.fronting.objs.responses.bouns.CookieAddingResponseBonus
import com.kcibald.services.fronting.objs.responses.bouns.HeaderAddingResponseBonus
import com.kcibald.services.fronting.objs.responses.bouns.ResponseTimeHeaderBonus
import com.kcibald.services.fronting.objs.responses.bouns.StatusResponseBonus
import com.kcibald.services.fronting.utils.*
import com.kcibald.services.user.AuthenticationClient
import com.kcibald.services.user.AuthenticationResult
import com.kcibald.utils.d
import com.kcibald.utils.i
import com.kcibald.utils.w
import com.wusatosi.recaptcha.RecaptchaClient
import io.vertx.core.http.Cookie
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.ext.jwt.jwtOptionsOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import org.apache.commons.validator.routines.EmailValidator
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object Login : UnsafeHTMLContentEntry(), FancyEntry {
    private val logger = LoggerFactory.getLogger(Login::class.java)

    private lateinit var COOKIE_KEY: String
    private lateinit var authenticationClient: AuthenticationClient
    private lateinit var jwtAuthProvider: JWTAuth
    private var recaptchaClient: RecaptchaClient? = null
    private lateinit var COOKIE_DOMAIN: String

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        val vertx = VertxHelper.currentVertx()
        val config = sharedObjects.config
        this.recaptchaClient = sharedObjects.recaptchaClient
        this.jwtAuthProvider = sharedObjects.jwtAuth

        logger.d { "Initializing Login API Route mounting" }
        this.COOKIE_KEY = config[Authentication.CookieKey]
        this.COOKIE_DOMAIN = config[Authentication.CookieDomain]
        this.authenticationClient = sharedObjects.getService("auth") {
            AuthenticationClient.createDefault(vertx)
        }

        router
            .post("/login")
            .consumeJson()
            .coroutineCoreHandler(::handleEvent)

        logger.i {
            data class LoginConfiguration(
                val cookieKey: String = this.COOKIE_KEY,
                val authenticationClient: AuthenticationClient = this.authenticationClient
            )
            "Mounted login API endpoint with ${LoginConfiguration()}, registered to router: $router"
        }
    }

    private val emailValidator = EmailValidator.getInstance()
    private val passwordPattern = Pattern.compile("^(?=.*?[a-z])(?=.*?[0-9]).{8,20}\$")

    private suspend fun handleEvent(context: RoutingContext): TerminateResponse {
        val (accountEmail: String, password: String, captcha: String?) = extractFields(context.jsonObject)
        logger.d { "account: $accountEmail attempts to login" }

        if (!checkArguments(accountEmail, password)) {
            logger.d { "account: $accountEmail login attempt failed on pre-check" }
            return passwordOrAccountEmailIncorrectResponse
        }

        val captchaResultWithTimeAsync = checkRecaptchaAndRecordPerfIfEnabledAsync(captcha)
        val (verificationPacked, authTime) = withProcessTimeRecording {
            authenticationClient.verifyCredential(accountEmail, password)
        }

        val (captchaResult, captchaTime) = captchaResultWithTimeAsync.await()
        return ResponseTimeHeaderBonus.fromNameAndTime("captcha", captchaTime)
            .plus(ResponseTimeHeaderBonus.fromNameAndTime("Auth", authTime))
            .plus(compileResult(accountEmail, verificationPacked.getOrThrow(), captchaResult))
    }

    private fun extractFields(requestObj: JsonObject): Triple<String, String, String?> {
        val accountEmail: String = requestObj["account"] ?: incompleteRequest()
        val password: String = requestObj["password"] ?: incompleteRequest()
        val captcha: String? = requestObj["captcha"]
        return Triple(accountEmail, password, captcha)
    }

    private fun compileResult(
        accountEmail: String,
        verification: AuthenticationResult,
        captchaResult: RecaptchaResponse
    ): TerminateResponse = when (val compiledVerification = joinVerification(captchaResult, verification)) {
        is AuthenticationResult.InvalidCredential, is AuthenticationResult.UserNotFound ->
            invalidCredentialResponse(accountEmail)
        is AuthenticationResult.Banned ->
            bannedFailureResponse(compiledVerification)
        is AuthenticationResult.Success ->
            successResponse(compiledVerification.user, captchaResult)
        is AuthenticationResult.SystemError ->
            systemErrorResponse(compiledVerification)
    }

    private fun joinVerification(
        captchaResult: RecaptchaResponse,
        verification: AuthenticationResult
    ): AuthenticationResult {
        return if (captchaResult == RecaptchaResponse.VERIFIED_INVALID)
            AuthenticationResult.InvalidCredential
        else
            verification
    }

    private fun invalidCredentialResponse(accountEmail: String): TerminateResponse {
        logger.d { "user $accountEmail was not found" }
        return passwordOrAccountEmailIncorrectResponse
    }

    private fun systemErrorResponse(verification: AuthenticationResult.SystemError): TerminateResponse {
        logger.w { "received system error from upstream service: authentication, message ${verification.message}" }
        return InternalErrorResponse
    }

    private fun successResponse(
        user: User,
        captchaResult: RecaptchaResponse
    ): TerminateResponse {
        val cookie = makeCookie(captchaResult, user)
        logger.debug("account: ${user.urlKey} successfully logged in")
        return CookieAddingResponseBonus(cookie) + EmptyTerminationResponse
    }

    private fun makeCookie(
        captchaResult: RecaptchaResponse,
        user: User
    ): Cookie {
        val expireInMinutes = calculateExpireInMinutes(captchaResult)

        val token = generateJwtToken(user, expireInMinutes)

        val cookie = Cookie.cookie(COOKIE_KEY, token)
        cookie.setSecure(true)
        cookie.setMaxAge(TimeUnit.MINUTES.toSeconds(expireInMinutes.toLong()))
        cookie.domain = COOKIE_DOMAIN

        return cookie
    }

    private fun generateJwtToken(
        user: User,
        expireInMinutes: Int
    ): String = jwtAuthProvider.generateToken(
        generateClaimsForUser(user),
        jwtOptionsOf(
            audience = listOf(user.urlKey),
            expiresInMinutes = expireInMinutes,
            issuer = "kcibald-frontend"
        )
    )

    private fun generateClaimsForUser(user: User): JsonObject {
        return jsonObjectOf(
            "username" to user.userName,
            "url_key" to user.urlKey,
            "user_avatar" to user.avatar
        )
    }

    private fun calculateExpireInMinutes(captchaResult: RecaptchaResponse): Int {
        if (captchaResult == RecaptchaResponse.VERIFICATION_FAILED)
            return 5
        else
            return TimeUnit.DAYS.toMinutes(30).toInt()
    }

    private fun checkArguments(account: String, password: String) =
        emailValidator.isValid(account) && passwordPattern.matcher(password).matches()

    private fun bannedFailureResponse(result: AuthenticationResult.Banned): TerminateResponse {
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

        return StatusResponseBonus(403)
            .plus(HeaderAddingResponseBonus("WWW-Authenticate" to "FormBased"))
            .plus(JsonResponse(body))
    }

    private val passwordOrAccountEmailIncorrectResponse =
        StatusResponseBonus(403)
            .plus(HeaderAddingResponseBonus("WWW-Authenticate" to "FormBased"))
            .plus(
                JsonResponse(
                    jsonObjectOf(
                        "success" to false,
                        "type" to "PASSWORD_OR_USERNAME_INCORRECT"
                    )
                )
            )

    private const val maximumRecaptchaTrial = 2
    private fun checkRecaptchaAndRecordPerfIfEnabledAsync(recaptchaToken: String?): Deferred<Pair<RecaptchaResponse, Long>> {
        val reClient = recaptchaClient

        if (reClient == null) {
            logger.w { "recaptcha checking has been disabled, THIS IS NOT SUITABLE FOR PRODUCTION" }
            return CompletableDeferred(RecaptchaResponse.VERIFIED_PASS to 0.toLong())
        }
        recaptchaToken ?: incompleteRequest()
        logger.d { "checking recaptcha $recaptchaToken" }
        return runWithVertxCorutinueAsync {
            val (recaptchaResultPacked, time) = withProcessTimeRecording {
                doFetchRecaptchaResult(reClient, recaptchaToken)
            }
            recaptchaResultPacked.getOrThrow() to time
        }
    }

    private suspend fun doFetchRecaptchaResult(
        reClient: RecaptchaClient,
        recaptchaToken: String
    ): RecaptchaResponse {
        for (retryCount in 1..maximumRecaptchaTrial) {
            try {
                //                it is not blocking call
                val operationName = "recaptcha check single"
                @Suppress("BlockingMethodInNonBlockingContext")
                if (withProcessTimeMonitoring(logger, operationName) { reClient.verify(recaptchaToken) }) {
                    logger.d { "recaptcha verified with ${RecaptchaResponse.VERIFIED_PASS}" }
                    return RecaptchaResponse.VERIFIED_PASS
                } else {
                    logger.d { "recaptcha verified with ${RecaptchaResponse.VERIFIED_PASS}" }
                    return RecaptchaResponse.VERIFIED_INVALID
                }
            } catch (e: IOException) {
                logger.i(e) { "IOException when validating recaptcha! $e" }
            }
        }
        logger.w {
            "recaptcha validation failed after $maximumRecaptchaTrial retries, " +
                    "return ${RecaptchaResponse.VERIFICATION_FAILED}"
        }
        return RecaptchaResponse.VERIFICATION_FAILED
    }

    private enum class RecaptchaResponse {
        VERIFIED_PASS,
        VERIFIED_INVALID,
        VERIFICATION_FAILED
    }

    override fun staticEntryPath(): List<Path> = listOf("/login")

}

