package com.kcibald.services.fronting.objs.entries

import com.kcibald.services.fronting.utils.ContentTypes
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.services.fronting.utils.StandardAuthenticationRejectResponse
import com.kcibald.services.fronting.utils.authenticated
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler

sealed class StaticHTMLContentEntry : HTMLContentEntry {
    abstract fun staticEntryPath(): List<Path>
}

abstract class UnsafeHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .handler(genericStaticHandler)
        }
    }
}

abstract class StandardStaticHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .authenticated(StandardAuthenticationRejectResponse.PAGE, sharedObjects.config, sharedObjects.jwtAuth)
                .handler(genericStaticHandler)
        }
    }
}

internal val genericStaticHandler = StaticHandler.create("static")
typealias Path = String