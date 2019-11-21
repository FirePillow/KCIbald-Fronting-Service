package com.kcibald.services.fronting.objs.entries

import com.kcibald.services.fronting.utils.SharedObjects
import io.vertx.ext.web.Router

interface HTMLContentEntry {
    fun routeHTMLContent(router: Router, sharedObjects: SharedObjects)
}