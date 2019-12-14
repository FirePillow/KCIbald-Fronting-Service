package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.controllers.MasterConfigSpec.Authentication
import com.kcibald.services.fronting.objs.entries.FancyEntry
import com.kcibald.services.fronting.objs.responses.EmptyResponse
import com.kcibald.services.fronting.objs.responses.RedirectResponse
import com.kcibald.services.fronting.utils.*
import com.kcibald.utils.d
import com.kcibald.utils.i
import io.vertx.core.http.Cookie
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object Logout : FancyEntry {

    private val logger = LoggerFactory.getLogger(Logout::class.java)

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        val cookieKey = sharedObjects.config[Authentication.CookieKey]

//        there is cookie handler attached when .authenticated handler is used
        router
            .post("/logout")
            .authenticated(StandardAuthenticationRejectResponse.API, sharedObjects.config, sharedObjects.jwtAuth)
            .coreHandler {
                clearCookie(cookieKey, it)
                EmptyResponse
            }

        logger.i { "Registries /logout (api) to router $router, config: cookieKey=$cookieKey" }
    }

    override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        val cookieKey = sharedObjects.config[Authentication.CookieKey]
        val redirectURL = "/login"

//        there is cookie handler attached when .authenticated handler is used
        router
            .get("/logout")
            .produces(ContentTypes.HTML)
            .authenticated(StandardAuthenticationRejectResponse.PAGE, sharedObjects.config, sharedObjects.jwtAuth)
            .coreHandler {
                clearCookie(cookieKey, it)
                RedirectResponse(redirectURL)
            }

        logger.i { "Registries /logout (HTML) to router $router, config: cookieKey=$cookieKey, redirectURL=$redirectURL" }
    }

    private fun clearCookie(cookieKey: String, context: RoutingContext) {
        logger.d { "Clear login cookie (key: $cookieKey) for user: ${context.user()?.principal()}" }
        val cookie = Cookie.cookie(cookieKey, "")
        cookie.setMaxAge(-1)
        cookie.setSecure(true)
        context.addCookie(cookie)
    }

}