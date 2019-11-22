package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.controllers.MasterConfigSpec.Authentication
import com.kcibald.services.fronting.objs.entries.FancyEntry
import com.kcibald.services.fronting.objs.responses.EmptyResponse
import com.kcibald.services.fronting.objs.responses.RedirectResponse
import com.kcibald.services.fronting.utils.ContentTypes
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.services.fronting.utils.coreHandler
import io.vertx.ext.web.Cookie
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object Logout : FancyEntry {

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        val cookieKey = sharedObjects.config[Authentication.CookieKey]

        router
            .post("/logout")
            .coreHandler {
                clearCookie(cookieKey, it)
                EmptyResponse
            }
    }

    override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        val cookieKey = sharedObjects.config[Authentication.CookieKey]
        val redirectURL = "/login"

        router
            .get("/logout")
            .produces(ContentTypes.HTML)
            .coreHandler {
                clearCookie(cookieKey, it)
                RedirectResponse(redirectURL)
            }
    }

    private fun clearCookie(cookieKey: String, context: RoutingContext) {
        val cookie = Cookie.cookie(cookieKey, "")
        cookie.setMaxAge(-1)
        cookie.setSecure(true)
        cookie.domain = "kcibald.com"
        context.addCookie(cookie)
    }

}