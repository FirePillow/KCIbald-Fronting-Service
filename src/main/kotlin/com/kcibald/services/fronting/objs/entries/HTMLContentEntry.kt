package com.kcibald.services.fronting.objs.entries

import io.vertx.ext.web.Router

interface HTMLContentEntry {
    fun routeHTMLContent(router: Router)
}