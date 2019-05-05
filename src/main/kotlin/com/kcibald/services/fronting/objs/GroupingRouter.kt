package com.kcibald.services.fronting.objs

import io.vertx.core.Vertx
import io.vertx.ext.web.Router

abstract class GroupingRouter(
    val mountPath: String,
    val apiEntries: List<APIEntry>,
    val htmlEntries: List<HTMLContentEntry>
) : FancyEntry {

    constructor(mountPath: String, fancyEntry: List<FancyEntry>) : this(mountPath, fancyEntry, fancyEntry)

    final override fun getAPISubRouter(vertx: Vertx): Router {
        val router = Router.router(vertx)
        for (apiEntry in apiEntries) {
            router.mountSubRouter(mountPath, apiEntry.getAPISubRouter(vertx))
        }
        return router
    }

    final override fun getHTMLSubRouter(vertx: Vertx): Router {
        val router = Router.router(vertx)
        for (htmlEntry in htmlEntries) {
            router.mountSubRouter(mountPath, htmlEntry.getHTMLSubRouter(vertx))
        }
        return router
    }

}