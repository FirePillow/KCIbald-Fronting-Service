package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.*
import com.kcibald.services.fronting.objs.*
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.get
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import org.apache.commons.validator.routines.EmailValidator
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.regex.Pattern

object Login : StaticHTMLContentEntry(), FancyEntry {
    private val logger = LoggerFactory.getLogger(Login::class.java)
    private val recaptchaScoreThreshold: Double = 0.7

    override fun getAPISubRouter(vertx: Vertx): Router {
        val router = Router.router(vertx)
        router
            .post("/login")
            .consumeJson()
            .coroutineCoreHandler(Login::loginAPI)
        return router
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
            return AuthorizationFailureResponses.PASSWORD_OR_ACCOUNT_EMAIL_INCORRECT
        }

//        might want to make recaptcha verification parell to account information processing
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
                    return AuthorizationFailureResponses.CAPTCHA_VERIFICATION_FAILED
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
                }
            }
        }

//        TODO: user verification magic
//        TODO: cookie magic

        logger.debug("account: $account successfully logged in")
        return EmptyResponse
    }

    private enum class AuthorizationFailureResponses(val type: String) : Response {
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

