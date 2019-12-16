package com.kcibald.services.fronting.utils

import com.kcibald.services.fronting.handlers.AuthorizationHandler
import com.kcibald.services.fronting.objs.responses.*
import com.kcibald.services.fronting.objs.responses.bouns.StatusResponseBonus
import com.kcibald.utils.d
import com.kcibald.utils.t
import com.kcibald.utils.w
import com.uchuhimo.konf.Config
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.auth.jwt.JWTAuth
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.kotlin.core.json.jsonObjectOf

private object RouterHelper

private val logger = LoggerFactory.getLogger(RouterHelper.javaClass)

const val JSON_CONTEXT_KEY = "_json_parsed"

fun Route.checkJsonIntegrity() = handler {
    logger.t { "Checking json body integrity" }
    // RoutingContext.bodyAsJson is nullable as well
    val result = runCatching { it.bodyAsJson }.getOrNull()

    if (result == null) {
        logger.d { "parse json failed (exception or empty body), reject request with Bad Request Response" }
        it.responseWith(BadRequestResponse)
    } else {
        it.put(JSON_CONTEXT_KEY, result)
        logger.t { "body json parse success, context key $JSON_CONTEXT_KEY with json body done" }
        it.next()
    }
}!!

//    let it fail if checked json is not present, it should fail in integration test
inline val RoutingContext.jsonObject: JsonObject
    get() = this[JSON_CONTEXT_KEY]!!

private val nonUploadBodyHandler = BodyHandler.create(false)
fun Route.consumeJson(bodyHandler: BodyHandler = nonUploadBodyHandler): Route {
    this.consumes(ContentTypes.JSON)
    this.handler(bodyHandler)
    this.checkJsonIntegrity()
    return this
}

private val authenticationRejectJson: JsonObject = jsonObjectOf(
    "success" to false,
    "type" to "AUTHORIZATION_MISSING_OR_INVALID"
)

object StandardAuthenticationRejectResponse {
    val PAGE = RedirectResponse("/login")
    val API = StatusResponseBonus(401) + JsonResponse(authenticationRejectJson)
}

fun Route.authenticated(rejectResponse: TerminateResponse, config: Config, authProvider: JWTAuth): Route {
    val authorizationHandler = AuthorizationHandler(rejectResponse, config, authProvider)
    this.handler(authorizationHandler)
    return this
}

@Suppress("NOTHING_TO_INLINE")
//for possible interpretation and better chaining
inline fun RoutingContext.responseWith(_responses: Response) = _responses.apply(this.response())

fun Route.coreHandler(core: (RoutingContext) -> TerminateResponse) = this.handler {
    val (_, time) = withProcessTimeRecording {
        logger.t { "Accepted and start processing request through $core, request: ${it.request()}" }
        val result = runCatching { core(it) }
        val response = normalize(result)
        logger.t { "normalized result as $response" }
        it.responseWith(response)
    }
    logger.d { "core handling took $time" }
    it.response().putHeader("X-Time", time.toString())
}!!

fun Route.coroutineCoreHandler(core: suspend (RoutingContext) -> TerminateResponse) = handler {
    launchWithVertxCorutinue(it.vertx()) {
        val (_, time) = withProcessTimeRecording {
            logger.t { "Accepted and start processing request through $core(coroutine), request: ${it.request()}" }
            val result = runCatching { core(it) }
            val response = normalize(result)
            logger.t { "normalized result as $response" }
            it.responseWith(response)
        }
        logger.d { "core handling took $time" }
        it.response().putHeader("X-Time", time.toString())
    }
}!!

private fun normalize(result: Result<TerminateResponse>): TerminateResponse {
    if (result.isSuccess) {
        logger.t { "handler is success, normalize as is" }
        return result.getOrNull()!!
    } else {
        val exception = result.exceptionOrNull()!!
        logger.t(exception) { "handler failed with exception, exception: $exception" }
        when (exception) {
            is IncompleteRequestException -> {
                logger.d { "bad request as IncompleteRequestException has received" }
                return BadRequestResponse
            }
            else -> {
                logger.w(exception) { "unexpected error from core handler" }
                return InternalErrorResponse
            }
        }
    }
}