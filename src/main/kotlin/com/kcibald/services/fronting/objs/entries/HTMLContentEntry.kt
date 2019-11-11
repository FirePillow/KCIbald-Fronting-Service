package com.kcibald.services.fronting.objs.entries

import com.uchuhimo.konf.Config
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

interface HTMLContentEntry {
    fun routeHTMLContent(router: Router, vertx: Vertx, configSource: Config)
}