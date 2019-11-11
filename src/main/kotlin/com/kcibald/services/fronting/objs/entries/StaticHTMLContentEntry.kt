package com.kcibald.services.fronting.objs.entries

import com.kcibald.services.fronting.ContentTypes
import com.kcibald.services.fronting.authenticated
import com.kcibald.services.fronting.objs.responses.Response
import com.uchuhimo.konf.Config
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import java.util.*

sealed class StaticHTMLContentEntry : HTMLContentEntry {
    abstract fun staticEntryPath(): List<Path>
}

abstract class UnsafeHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun routeHTMLContent(router: Router, vertx: Vertx, configSource: Config) {
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .authenticated()
                .handler(genericStaticHandler)
        }
    }
}

abstract class StandardStaticHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun routeHTMLContent(router: Router, vertx: Vertx, configSource: Config) {
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .authenticated()
                .handler(genericStaticHandler)
        }
    }
}

internal val genericStaticHandler = StaticHandler.create("static")
typealias Path = String