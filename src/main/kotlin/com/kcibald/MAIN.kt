package com.kcibald

import com.kcibald.services.fronting.FrontingServiceVerticle
import io.vertx.core.Vertx

fun main() {
    Vertx
        .vertx()
        .deployVerticle(FrontingServiceVerticle)
}
