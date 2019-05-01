package com.kcibald.services.fronting.objs

import io.vertx.ext.web.Router

interface HTMLContentEntry {
    fun getHTMLSubRouter(): Router
}