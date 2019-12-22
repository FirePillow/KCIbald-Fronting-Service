package com.kcibald.services.fronting.utils

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.Charset

internal fun jsonBodyPublisher(json: JsonObject): HttpRequest.BodyPublisher =
    HttpRequest.BodyPublishers.ofString(json.encode())

internal object JsonBodyHandler : HttpResponse.BodyHandler<JsonObject> {
    override fun apply(responseInfo: HttpResponse.ResponseInfo): HttpResponse.BodySubscriber<JsonObject> {
        return HttpResponse.BodySubscribers.mapping(
            HttpResponse.BodySubscribers.ofString(Charset.defaultCharset())
        ) { s ->
            if (s.isNullOrEmpty())
                null
            else
                JsonObject(s)
        }
    }

}

fun runVertxCoroutinueContext(vertx: Vertx, block: suspend CoroutineScope.() -> Unit) =
    runBlocking { GlobalScope.async(vertx.dispatcher(), block = block).await() }
