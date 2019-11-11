package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.coreHandler
import com.kcibald.services.fronting.objs.entries.APIEntry
import com.kcibald.services.fronting.objs.responses.Response
import com.uchuhimo.konf.Config
import io.vertx.core.Vertx
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext

object Logout : APIEntry {

    override fun routeAPIEndpoint(router: Router, vertx: Vertx, configSource: Config) {
        router
            .post("/logout")
            .coreHandler(Logout::logoutAPI)
    }

    private fun logoutAPI(context: RoutingContext): Response {
        context
        TODO()
    }

}