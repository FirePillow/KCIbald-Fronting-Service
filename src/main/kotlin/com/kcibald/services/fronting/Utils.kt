package com.kcibald.services.fronting

import com.kcibald.services.fronting.objs.Response
import com.wusatosi.recaptcha.v3.RecaptchaV3Client
import io.vertx.core.http.HttpServerResponse
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ContentTypes {
    const val JSON = "application/json"
    const val HTML = "text/html"
}

internal val EXEC_VERTICLE = FrontingServiceVerticle.vertx

internal val RECAPTCHA = RecaptchaV3Client("", useRecaptchaDotNetEndPoint = true)


private object IncompleteRequestException : NoStackTraceThrowable("incomplete request")

fun incompleteRequest(): Nothing {
    throw IncompleteRequestException
}


inline fun Route.coreHandler(crossinline core: (RoutingContext) -> Response) = this.handler {
    val result = runCatching { core(it) }
    postProcess(result)
}!!

inline fun Route.coroutineCoreHandler(crossinline core: suspend (RoutingContext) -> Response) = handler {
    GlobalScope.launch(it.vertx().dispatcher()) {
        val result = runCatching { core(it) }
        postProcess(result)
    }
}!!

fun postProcess(result: Result<Response>) {
    TODO()
}

fun HttpServerResponse.end(body: JsonObject) = this.end(body.toString())

fun RoutingContext.responseWith(_responses: Response) = _responses.apply(this)

