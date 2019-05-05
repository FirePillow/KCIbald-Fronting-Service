package com.kcibald.services.fronting.objs

import com.kcibald.services.fronting.ContentTypes
import com.kcibald.services.fronting.EXEC_VERTX
import com.kcibald.services.fronting.authenticated
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler

abstract class StaticHTMLContentEntry : HTMLContentEntry {
    abstract fun staticEntryPath(): List<Path>

    override fun getHTMLSubRouter(vertx: Vertx): Router {
        val router = Router.router(vertx)
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .handler(genericStaticHandler)
        }
        return router
    }
}

abstract class AuthorizedHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun getHTMLSubRouter(vertx: Vertx): Router {
        val router = Router.router(EXEC_VERTX)
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .authenticated()
                .handler(genericStaticHandler)
        }
        return router
    }
}

internal val genericStaticHandler = StaticHandler.create("static")
typealias Path = String