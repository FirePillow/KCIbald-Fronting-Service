package com.kcibald.services.fronting.objs.entries

import io.vertx.ext.web.Router

abstract class GroupingRouter(
    private val fancyEntry: List<FancyEntry>? = null,
    private val apiEntries: List<APIEntry>? = null,
    private val htmlEntries: List<HTMLContentEntry>? = null
) : FancyEntry {

    override fun routeAPIEndpoint(router: Router) {
        if (apiEntries != null)
            for (apiEntry in apiEntries) {
                apiEntry.routeAPIEndpoint(router)
            }
        if (fancyEntry != null)
            for (entry in fancyEntry) {
                entry.routeAPIEndpoint(router)
            }
    }

    override fun routeHTMLContent(router: Router) {
        if (htmlEntries != null)
            for (htmlEntry in htmlEntries) {
                htmlEntry.routeHTMLContent(router)
            }
        if (fancyEntry != null)
            for (entry in fancyEntry) {
                entry.routeHTMLContent(router)
            }
    }

    fun routeAll(router: Router) {
        routeAPIEndpoint(router)
        routeHTMLContent(router)
    }

}