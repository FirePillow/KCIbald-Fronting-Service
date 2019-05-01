package com.kcibald.services.fronting

import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.VirtualHostHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle

object FrontingServiceVerticle: CoroutineVerticle() {

    override suspend fun start() {
        val router = Router.router(vertx)

        vertx.createHttpServer().requestHandler(router::handle).listen()
        super.start()
    }

    override suspend fun stop() {
        super.stop()
    }
}
