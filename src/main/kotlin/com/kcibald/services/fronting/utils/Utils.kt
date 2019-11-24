@file:Suppress("NOTHING_TO_INLINE")

package com.kcibald.services.fronting.utils

import io.vertx.core.Vertx
import io.vertx.core.impl.NoStackTraceThrowable
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ContentTypes {
    const val JSON = "application/json"
    const val HTML = "text/html"
}

internal inline fun launchVertxCorutinue(
    vertx: Vertx,
    noinline block: suspend CoroutineScope.() -> Unit
) = GlobalScope.launch(vertx.dispatcher(), block = block)

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