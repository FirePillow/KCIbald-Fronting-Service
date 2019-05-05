@file:Suppress("NOTHING_TO_INLINE")

package com.kcibald.services.fronting

import com.kcibald.services.fronting.objs.BadRequestResponse
import com.kcibald.services.fronting.objs.responseWith
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler

object RouterHelper

const val JSON_CONTEXT_KEY = "_json_parsed"

inline fun Route.checkJsonIntegrity() = handler {
    val result = runCatching {
        it.bodyAsJson
    }
    if (result.isFailure) {
//            TODO: log
        it.responseWith(BadRequestResponse)
    } else {
        it.put(JSON_CONTEXT_KEY, result.getOrNull())
        it.next()
    }
}!!

//    let it fail if checked json is not present, it should fail in integration test
inline val RoutingContext.jsonObject: JsonObject
    get() = this[JSON_CONTEXT_KEY]!!

inline fun Route.consumeJson(): Route {
    this.consumes(ContentTypes.JSON)
    this.handler(BodyHandler.create(false))
    this.checkJsonIntegrity()
    return this
}

private val cookieHandler = CookieHandler.create()

fun Route.authenticated(): Route {
    this.handler(cookieHandler::handle)
//    TODO: Session store
//    TODO: Session handler
//    TODO: RedirectAuthHandler
//    TODO: Auth provider
    TODO()
}