package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.objs.APIEntry
import com.kcibald.services.fronting.EXEC_VERTICLE
import com.kcibald.services.fronting.objs.HTMLContentEntry
import io.vertx.ext.web.Router

object CommonAPIRouter : HTMLContentEntry, APIEntry {

    private val apiEntries = listOf<APIEntry>(
        Login
    )

    override fun getAPISubRouter(): Router {
        val router = Router.router(EXEC_VERTICLE)
        for (apiEntry in apiEntries) {
            router.mountSubRouter("/", apiEntry.getAPISubRouter())
        }
        return router
    }

    private val htmlEntries = listOf<HTMLContentEntry>(
        Login
    )

    override fun getHTMLSubRouter(): Router {
        val router = Router.router(EXEC_VERTICLE)
        for (htmlEntry in htmlEntries) {
            router.mountSubRouter("/", htmlEntry.getHTMLSubRouter())
        }
        return router
    }
}