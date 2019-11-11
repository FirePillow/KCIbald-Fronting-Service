package com.kcibald.services.fronting.objs.entries

import com.uchuhimo.konf.Config
import io.vertx.core.Vertx
import io.vertx.ext.web.Router

abstract class GroupingRouter(
    private val fancyEntry: List<FancyEntry>? = null,
    private val apiEntries: List<APIEntry>? = null,
    private val htmlEntries: List<HTMLContentEntry>? = null
) : FancyEntry {

    override fun routeAPIEndpoint(router: Router, vertx: Vertx, configSource: Config) {
        if (apiEntries != null)
            for (apiEntry in apiEntries) {
                apiEntry.routeAPIEndpoint(router, vertx, configSource)
            }
        if (fancyEntry != null)
            for (entry in fancyEntry) {
                entry.routeAPIEndpoint(router, vertx, configSource)
            }
    }

    override fun routeHTMLContent(router: Router, vertx: Vertx, configSource: Config) {
        if (htmlEntries != null)
            for (htmlEntry in htmlEntries) {
                htmlEntry.routeHTMLContent(router, vertx, configSource)
            }
        if (fancyEntry != null)
            for (entry in fancyEntry) {
                entry.routeHTMLContent(router, vertx, configSource)
            }
    }

}