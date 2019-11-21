package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.utils.ContentTypes
import com.kcibald.services.fronting.controllers.Config.Authentication
import com.kcibald.services.fronting.utils.coreHandler
import com.kcibald.services.fronting.objs.entries.FancyEntry
import com.kcibald.services.fronting.objs.responses.EmptyResponse
import com.kcibald.services.fronting.objs.responses.RedirectResponse
import com.uchuhimo.konf.Config
import io.vertx.core.Vertx
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object Logout : FancyEntry {

    override fun routeAPIEndpoint(router: Router, vertx: Vertx, configSource: Config) {
        val cookieKey = configSource[Authentication.CookieKey]

        router
            .post("/logout")
            .coreHandler {
                clearCookie(cookieKey, it)
                EmptyResponse
            }
    }

    override fun routeHTMLContent(router: Router, vertx: Vertx, configSource: Config) {
        val cookieKey = configSource[Authentication.CookieKey]
        val redirectURL = configSource[Authentication.RedirectURLWhenLogout]

        router
            .get("/logout")
            .produces(ContentTypes.HTML)
            .coreHandler {
                clearCookie(cookieKey, it)
                RedirectResponse(redirectURL)
            }
    }

    private fun clearCookie(cookieKey: String, context: RoutingContext) {
        val cookie = Cookie.cookie(cookieKey, "_")
        cookie.setMaxAge(-1)
        cookie.setSecure(true)
        cookie.domain = "kcibald.com"
        context.addCookie(cookie)
    }

}