package com.kcibald.services.fronting.objs.entries

import com.kcibald.services.fronting.utils.ContentTypes
import com.kcibald.services.fronting.utils.SharedObjects
import com.kcibald.services.fronting.utils.StandardAuthenticationRejectResponse
import com.kcibald.services.fronting.utils.authenticated
import com.kcibald.utils.d
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler

sealed class StaticHTMLContentEntry : HTMLContentEntry {
    abstract fun staticEntryPath(): List<Path>
}

abstract class UnsafeHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        val paths = staticEntryPath()
        logger.d { "mounting unsafe html routes by class $javaClass to paths $paths" }
        for (p in paths) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .handler(genericStaticHandler)
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(UnsafeHTMLContentEntry::class.java)
    }
}

abstract class StandardStaticHTMLContentEntry : StaticHTMLContentEntry() {
    final override fun routeHTMLContent(router: Router, sharedObjects: SharedObjects) {
        val paths = staticEntryPath()
        logger.d { "mounting standard static html content entry by class $javaClass to paths $paths" }
        for (p in paths) {
            router
                .get(p)
                .produces(ContentTypes.HTML)
                .authenticated(StandardAuthenticationRejectResponse.PAGE, sharedObjects.config, sharedObjects.jwtAuth)
                .handler(genericStaticHandler)
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(StandardStaticHTMLContentEntry::class.java)
    }
}

internal val genericStaticHandler = StaticHandler.create("static")
typealias Path = String