package com.kcibald.services.fronting.utils

import io.vertx.core.Context
import io.vertx.core.Vertx

object VertxHelper {
    internal const val sharedObjVertxContextKey = "_global_shared_obj"

    fun currentVertx(): Vertx = currentContext.owner()

    fun sharedObject(): SharedObjects = currentContext.get<SharedObjects>(sharedObjVertxContextKey)
        ?: throw Error("shared object not initialized")

    private val currentContext: Context
        get() = Vertx.currentContext() ?: throw Error("not in a vertx thread")
}
