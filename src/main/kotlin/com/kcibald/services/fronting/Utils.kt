@file:Suppress("NOTHING_TO_INLINE")

package com.kcibald.services.fronting

import com.kcibald.services.fronting.objs.responses.BadRequestResponse
import com.kcibald.services.fronting.objs.responses.InternalErrorResponse
import com.kcibald.services.fronting.objs.responses.Response
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object ContentTypes {
    const val JSON = "application/json"
    const val HTML = "text/html"
}

internal inline val EXEC_VERTX
    get() = FrontingServiceVerticle.vertx

internal inline fun launchVertxCorutinue(
    vertxOverride: Vertx = EXEC_VERTX,
    noinline block: suspend CoroutineScope.() -> Unit
) {
    GlobalScope.launch(vertxOverride.dispatcher(), block = block)
}

internal val RECAPTCHA = RecaptchaV3Client("", useRecaptchaDotNetEndPoint = true)

object IncompleteRequestException : NoStackTraceThrowable("incomplete request")

inline fun incompleteRequest(): Nothing {
    throw IncompleteRequestException
}

fun Route.coreHandler(core: (RoutingContext) -> Response) = this.handler {
    val result = runCatching { core(it) }
    val response = normalize(result)
    it.responseWith(response)
}!!

fun Route.coroutineCoreHandler(core: suspend (RoutingContext) -> Response) = handler {
    launchVertxCorutinue(it.vertx()) {
        val result = runCatching { core(it) }
        val response = normalize(result)
        it.responseWith(response)
    }
}!!

private val coreHandlerAcceptorLogger = LoggerFactory.getLogger(RouterHelper.javaClass)

private inline fun normalize(result: Result<Response>): Response {
    @Suppress("LiftReturnOrAssignment")
    if (result.isSuccess) {
        return result.getOrNull()!!
    } else {
        return when (val exception = result.exceptionOrNull()!!) {
            is IncompleteRequestException -> BadRequestResponse
            else -> {
                coreHandlerAcceptorLogger.warn("unexpected error from core handler", exception)
                InternalErrorResponse
            }
        }
    }
}

//TODO: json formatter config
inline fun JsonObject.formatToString(): String = this.toString()

inline fun HttpServerResponse.end(body: JsonObject) = this.end(body.toString())
