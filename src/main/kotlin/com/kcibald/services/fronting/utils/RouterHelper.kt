@file:Suppress("NOTHING_TO_INLINE")

package com.kcibald.services.fronting.utils

import com.kcibald.services.fronting.handlers.AuthorizationHandler
import com.kcibald.services.fronting.objs.responses.BadRequestResponse
import com.kcibald.services.fronting.objs.responses.JsonResponse
import com.kcibald.services.fronting.objs.responses.RedirectResponse
import com.kcibald.services.fronting.objs.responses.Response
import com.uchuhimo.konf.Config
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CookieHandler
import io.vertx.kotlin.core.json.jsonObjectOf

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

private val authenticationRejectJson: JsonObject = jsonObjectOf(
    "success" to false,
    "type" to "AUTHORIZATION_MISSING_OR_INVALID"
)

object StandardAuthenticationRejectResponse {
    val PAGE = RedirectResponse("/login")
    val API = JsonResponse(authenticationRejectJson, 401)
}

fun Route.authenticated(rejectResponse: Response, config: Config, authProvider: JWTAuth): Route {
    val authorizationHandler = AuthorizationHandler(rejectResponse, config, authProvider)
    this.handler(cookieHandler::handle)
    this.handler(authorizationHandler)
    return this
}

@Suppress("NOTHING_TO_INLINE")
//for possible interpretation and better chaining
inline fun RoutingContext.responseWith(_responses: Response) = _responses.apply(this.response())
