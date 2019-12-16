@file:Suppress("NOTHING_TO_INLINE")

package com.kcibald.services.fronting.utils

import com.kcibald.utils.d
import io.vertx.core.Vertx
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.Logger
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

object ContentTypes {
    const val JSON = "application/json"
    const val HTML = "text/html"
}

internal inline fun launchWithVertxCorutinue(
    vertx: Vertx = VertxHelper.currentVertx(),
    noinline block: suspend CoroutineScope.() -> Unit
) = GlobalScope.launch(vertx.dispatcher(), block = block)

internal inline fun <T> runWithVertxCorutinueAsync(
    vertx: Vertx = VertxHelper.currentVertx(),
    noinline block: suspend CoroutineScope.() -> T
) = GlobalScope.async(vertx.dispatcher(), block = block)

object IncompleteRequestException : NoStackTraceThrowable("incomplete request")

inline fun incompleteRequest(): Nothing {
    throw IncompleteRequestException
}

inline fun JsonObject.formatToString(): String = this.encode()

internal val RoutingContext.username: String
    get() = this.user().principal().getString("username")

internal val RoutingContext.userUrlKey: String
    get() = this.user().principal().getString("url_key")

internal val RoutingContext.avatarKey: String
    get() = this.user().principal().getString("user_avatar")

internal inline fun <OUT> withProcessTimeMonitoring(logger: Logger, block: () -> OUT): OUT =
    withProcessTimeMonitoring(logger, "", block)

internal inline fun <OUT> withProcessTimeMonitoring(logger: Logger, operationName: String, block: () -> OUT): OUT {
    val enabled = logger.isDebugEnabled
    val tStart = if (enabled) System.currentTimeMillis() else 0
    return try {
        block()
    } finally {
        if (enabled) {
            logger.d { "$operationName took ${System.currentTimeMillis() - tStart}" }
        }
    }
}

internal inline fun <OUT> withProcessTimeRecording(block: () -> OUT): Pair<Result<OUT>, Long> {
    val tStart = System.currentTimeMillis()
    val result = runCatching(block)
    val tTaken = System.currentTimeMillis() - tStart
    return result to tTaken
}
