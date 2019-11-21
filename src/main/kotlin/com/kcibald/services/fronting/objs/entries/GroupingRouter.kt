package com.kcibald.services.fronting.objs.entries

import com.kcibald.services.fronting.utils.SharedObjects
import io.vertx.ext.web.Router

abstract class GroupingRouter(
    private val fancyEntry: List<FancyEntry>? = null,
    private val apiEntries: List<APIEntry>? = null,
    private val htmlEntries: List<HTMLContentEntry>? = null
) : FancyEntry {

    override fun routeAPIEndpoint(router: Router, sharedObjects: SharedObjects) {
        if (apiEntries != null)
            for (apiEntry in apiEntries) {
                apiEntry.routeAPIEndpoint(router, sharedObjects)
            }
        if (fancyEntry != null)
            for (entry in fancyEntry) {
                entry.routeAPIEndpoint(router, sharedObjects)
            }
    }

    override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        if (htmlEntries != null)
            for (htmlEntry in htmlEntries) {
                htmlEntry.routeHTMLContent(router, sharedObjects)
            }
        if (fancyEntry != null)
            for (entry in fancyEntry) {
                entry.routeHTMLContent(router, sharedObjects)
            }
    }

}