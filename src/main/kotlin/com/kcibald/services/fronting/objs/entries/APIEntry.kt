package com.kcibald.services.fronting.objs.entries

import com.kcibald.services.fronting.utils.SharedObjects
import io.vertx.ext.web.Router

interface APIEntry {
    fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects)
}