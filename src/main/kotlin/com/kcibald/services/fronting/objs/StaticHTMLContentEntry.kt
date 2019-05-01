package com.kcibald.services.fronting.objs

import com.kcibald.services.fronting.ContentTypes
import com.kcibald.services.fronting.EXEC_VERTICLE
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler

interface StaticHTMLContentEntry : HTMLContentEntry {
    fun staticEntryPath(): List<Path>

    override fun getHTMLSubRouter(): Router {
        val router = Router.router(EXEC_VERTICLE)
        for (p in staticEntryPath()) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .handler(genericStaticHandler)
        }
        return router
    }
}

internal val genericStaticHandler = StaticHandler.create("static")
typealias Path = String