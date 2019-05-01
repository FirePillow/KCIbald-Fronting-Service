package com.kcibald.services.fronting

import com.kcibald.services.fronting.objs.BadRequestResponse
import com.kcibald.services.fronting.objs.fail
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler

object RouterHelper {
    fun routes(router: Router) {
        router.errorHandler(400) {

        }
    }
}

private const val JSON_CONTEXT_KEY = "_json_parsed"

fun Route.checkJsonIntegrity() = handler {
    val result = runCatching {
        it.bodyAsJson
    }
    if (result.isFailure) {
//            TODO: log
        it.fail(BadRequestResponse)
    } else {
        it.put(JSON_CONTEXT_KEY, result.getOrNull())
        it.next()
    }
}!!

//    let it fail if checked json is not present, it should fail in integration test
val RoutingContext.jsonObject: JsonObject
    get() = this[JSON_CONTEXT_KEY]!!

fun Route.consumeJson(): Route {
    this.consumes(ContentTypes.JSON)
    this.handler(BodyHandler.create(false))
    this.checkJsonIntegrity()
    return this
}