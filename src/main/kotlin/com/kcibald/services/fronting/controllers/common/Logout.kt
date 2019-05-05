package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.coreHandler
import com.kcibald.services.fronting.objs.APIEntry
import com.kcibald.services.fronting.objs.Response
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object Logout : APIEntry {
    override fun getAPISubRouter(vertx: Vertx): Router {
        val router = Router.router(vertx)
        router
            .post("/logout")
            .coreHandler(Logout::logoutAPI)
        return router
    }

    private fun logoutAPI(context: RoutingContext): Response {
        TODO()
    }

}