package com.kcibald.services.fronting.objs.entries

import io.vertx.ext.web.Router

interface APIEntry {
    fun routeAPIEndpoint(router: Router)
}