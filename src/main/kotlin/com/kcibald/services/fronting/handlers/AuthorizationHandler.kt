package com.kcibald.services.fronting.handlers

import com.kcibald.services.fronting.controllers.MasterConfigSpec.Authentication
import com.kcibald.services.fronting.objs.responses.Response
import com.kcibald.services.fronting.utils.responseWith
import com.kcibald.services.fronting.utils.username
import com.kcibald.utils.d
import com.uchuhimo.konf.Config
import io.vertx.core.Handler
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.core.json.jsonObjectOf

class AuthorizationHandler(
    private val response: Response,
    configSource: Config,
    private val authProvider: AuthProvider
) : Handler<RoutingContext> {

    private val cookieKey = configSource[Authentication.CookieKey]
    private val logger = LoggerFactory.getLogger(AuthorizationHandler::class.java)

    override fun handle(event: RoutingContext) {
        val cookie = event.getCookie(cookieKey)
        if (cookie == null) {
            event.responseWith(response)
            return
        }
        authProvider.authenticate(jsonObjectOf("jwt" to cookie)) {
            if (it.succeeded()) {
                val user = it.result()!!
                event.setUser(user)
                logger.d { "authentication check success, user ${event.username}" }
                event.next()
            } else {
                val cause = it.cause()
                logger.d(cause) {
                    "denied request, because insufficient authentication, exception: ${cause.javaClass}, ${cause.message}"
                }
                event.responseWith(response)
            }
        }
    }
}