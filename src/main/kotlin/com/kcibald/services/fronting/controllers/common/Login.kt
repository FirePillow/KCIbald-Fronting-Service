package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.*
import com.kcibald.services.fronting.controllers.Config.Authentication
import com.kcibald.services.fronting.objs.entries.FancyEntry
import com.kcibald.services.fronting.objs.entries.Path
import com.kcibald.services.fronting.objs.entries.UnsafeHTMLContentEntry
import com.kcibald.services.fronting.objs.responses.EmptyResponse
import com.kcibald.services.fronting.objs.responses.InternalErrorResponse
import com.kcibald.services.fronting.objs.responses.Response
import com.kcibald.services.user.AuthenticationClient
import com.kcibald.services.user.AuthenticationResult
import com.uchuhimo.konf.Config
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.auth.jwt.JWTAuthOptions
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
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.properties.Delegates

object Login : UnsafeHTMLContentEntry(), FancyEntry {
    private val logger = LoggerFactory.getLogger(Login::class.java)

    //    should be imported from config or some sort
    private var recaptchaScoreThreshold by Delegates.notNull<Double>()
    private lateinit var COOKIE_KEY: String
    private lateinit var authenticationClient: AuthenticationClient
    private lateinit var jwtAuthProvider: JWTAuth

    override fun routeAPIEndpoint(router: Router, vertx: Vertx, configSource: Config) {
        recaptchaScoreThreshold = configSource[Authentication.RecaptchaThreshold]
        COOKIE_KEY = configSource[Authentication.CookieKey]

        authenticationClient = AuthenticationClient.createDefault(vertx)
        jwtAuthProvider = JWTAuth.create(vertx, JWTAuthOptions())

        router
            .post("/login")
            .consumeJson()
            .handler((CookieHandler.create())::handle)
            .coroutineCoreHandler(Login::loginAPI)
    }

    private val emailValidator = EmailValidator.getInstance()
    private val passwordPattern = Pattern.compile("^(?=.*?[a-z])(?=.*?[0-9]).{8,20}\$")

    private suspend fun loginAPI(context: RoutingContext): Response {
        val requestObj = context.jsonObject
        val recaptchaToken: String = requestObj["captcha"] ?: incompleteRequest()
        val account: String = requestObj["account"] ?: incompleteRequest()
        val password: String = requestObj["password"] ?: incompleteRequest()

        logger.debug("account: $account attempts to login")

//        pre-process check
        if (!emailValidator.isValid(account) || !passwordPattern.matcher(password).matches()) {
            logger.debug("account: $account login attempt failed on pre-check")
            return StandardAuthorizationFailureResponses.PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT
        }

//        might want to make recaptcha verification parell to account information processing
        var unsafeLogin = false
        for (retryCount in 1..2) {
            try {
//                it is not blocking call
                @Suppress("BlockingMethodInNonBlockingContext")
                val score = RECAPTCHA.getVerifyScore(recaptchaToken)
                if (score < recaptchaScoreThreshold) {
                    logger.info(
                        "logging attempt by account $account denied because recaptcha score is under threshold" +
                                "($recaptchaScoreThreshold > $score)"
                    )
                    return StandardAuthorizationFailureResponses.CAPTCHA_VERIFICATION_FAILED
                } else {
                    break
                }
            } catch (e: IOException) {
//            should I let user pass though or not? in case of an recaptcha-side failure
//            if strictly requiring recaptcha validity, user will not be able to login
//            should I compensate security to usability or the opposite
                logger.info("IOException when validating recaptcha! $e", e)
                if (retryCount == 2) {
                    logger.warn(
                        "Recaptcha validation failure, retried $retryCount times, letting logging attempt $account though"
                    )
//                    Might want to expire this session early (like... 5min)
                    unsafeLogin = true
                }
            }
        }

        when (val verification = authenticationClient.verifyCredential(account, password)) {
            is AuthenticationResult.InvalidCredential, is AuthenticationResult.UserNotFound ->
                return StandardAuthorizationFailureResponses.PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT
            is AuthenticationResult.Banned ->
                return bannedFailure(verification)
            is AuthenticationResult.Success -> {
                val expireInMinutes = if (unsafeLogin) 5 else TimeUnit.DAYS.toMinutes(30).toInt()

                val user = verification.user
                val claims = jsonObjectOf(
                    "username" to user.userName,
                    "url_key" to user.urlKey,
                    "user_avatar" to user.avatar
                )

                val token = jwtAuthProvider.generateToken(
                    claims, jwtOptionsOf(
                        audience = listOf(user.userName, account),
                        expiresInMinutes = expireInMinutes,
                        issuer = "kcibald-frontend"
                    )
                )
                val cookie = Cookie.cookie(COOKIE_KEY, token)
                cookie.setSecure(true)
                cookie.setMaxAge(TimeUnit.MINUTES.toSeconds(expireInMinutes.toLong()))
                cookie.domain = "kcibald.com"
                context.addCookie(cookie)
                logger.debug("account: $account successfully logged in")
                return EmptyResponse
            }
            is AuthenticationResult.SystemError -> {
                logger.warn("received system error from upstream service: authentication, message ${verification.message}")
                return InternalErrorResponse
            }
        }
    }

    private fun bannedFailure(result: AuthenticationResult.Banned): Response = object : Response {
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

    override fun staticEntryPath(): List<Path> = listOf("/login")

}

