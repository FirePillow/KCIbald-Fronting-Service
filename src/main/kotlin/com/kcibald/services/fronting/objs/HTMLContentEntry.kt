package com.kcibald.services.fronting.objs

import io.vertx.core.Vertx
import io.vertx.ext.web.Router

interface HTMLContentEntry {
    fun getHTMLSubRouter(vertx: Vertx): Router
}