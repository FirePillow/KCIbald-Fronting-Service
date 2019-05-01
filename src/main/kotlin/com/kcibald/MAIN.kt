package com.kcibald

import com.kcibald.services.fronting.FrontingServiceVerticle
import io.vertx.core.Vertx
import io.vertx.kotlin.coroutines.awaitResult

fun main() {
    val vertx = Vertx.vertx()!!
    vertx.deployVerticle(FrontingServiceVerticle)
}
