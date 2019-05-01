package com.kcibald.services.fronting.objs

import io.vertx.ext.web.Router

interface APIEntry {
    fun getAPISubRouter(): Router
}