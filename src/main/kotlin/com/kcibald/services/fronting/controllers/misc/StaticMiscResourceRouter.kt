package com.kcibald.services.fronting.controllers.misc

import com.kcibald.services.fronting.controllers.MasterConfigSpec
import com.kcibald.services.fronting.objs.entries.HTMLContentEntry
import com.kcibald.services.fronting.objs.responses.RedirectResponse
import com.kcibald.services.fronting.objs.responses.SingleFileResponse
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.services.fronting.utils.coreHandler
import com.kcibald.services.fronting.utils.responseWith
import com.uchuhimo.konf.Config
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.FaviconHandler
import java.util.concurrent.TimeUnit

object StaticMiscResourceRouter : HTMLContentEntry {
    override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        val config = sharedObjects.config

        routeFavicon(config, router)
        routeRobots(router)
    }

    private fun routeFavicon(config: Config, router: Router) {
        val redirectUrl = config[MasterConfigSpec.MiscFiles.FaviconRedirectURL]
        val isUrlEmpty = redirectUrl.isEmpty()

        val faviconRoute = router
            .get("/favicon.ico")
            .handler {
                if (isUrlEmpty) {
    //                    pass to static file handler
                    it.next()
                    return@handler
                } else {
                    it.responseWith(RedirectResponse(redirectUrl, false))
                }
            }

        if (isUrlEmpty) {
            val faviconHandler = FaviconHandler.create(
                config[MasterConfigSpec.MiscFiles.FaviconFilePath],
                TimeUnit.DAYS.toSeconds(1)
            )
            faviconRoute.handler(faviconHandler)
        }
    }

    private fun routeRobots(router: Router) {
        router
            .get("/robots.txt")
            .coreHandler {
                SingleFileResponse("static/misc/robots.txt")
            }
    }
}
