package com.kcibald.services.fronting

import com.kcibald.services.fronting.controllers.common.CommonAPIRouter
import com.uchuhimo.konf.Config
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

object FrontingServiceVerticle : CoroutineVerticle() {

    override suspend fun start() {
        val config = Config { addSpec(com.kcibald.services.fronting.controllers.Config) }
            .from.json.resource("config.json")

        val router = Router.router(vertx)
        CommonAPIRouter.routeAPIEndpoint(router, vertx, config)
        CommonAPIRouter.routeHTMLContent(router, vertx, config)

        val port = config[com.kcibald.services.fronting.controllers.Config.httpPort]
        vertx
            .createHttpServer()
            .requestHandler(router::handle)
            .listenAwait(port)

        super.start()
    }

    override suspend fun stop() {
        super.stop()
    }
}
