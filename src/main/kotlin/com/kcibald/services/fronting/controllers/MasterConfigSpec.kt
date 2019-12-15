package com.kcibald.services.fronting.controllers

import com.uchuhimo.konf.ConfigSpec

object MasterConfigSpec : ConfigSpec("") {
    val VertxHttpServerConfig by optional(mapOf<String, Any>(), "vertx_http_server_config")
    val RecaptchaSiteKey by required<String>("recaptcha_site_key")

    object Authentication : ConfigSpec("authentication") {
        val CookieKey by optional("_kb_aut", "cookie_key")
        val JwtAuthConfig by required<Map<String, *>>("jwt_auth", "see vertx jwtAuth config")
        val RecaptchaThreshold by optional(0.7, "login_recaptcha_threshold", "should be less than 1")
        val CookieDomain by optional(".kcibald.com", "cookie_domain")
    }

    val CorsDomainPattern by optional(".*\\.kcibald.com\$", "cors domain matching")

    object MiscFiles: ConfigSpec("misc_file") {
        val FaviconRedirectURL by optional("", "favicon_redirect_url")
        val FaviconFilePath by optional(
            "static/misc/favicon.ico",
            "favicon_file_path",
            "fall back for favicon redirct url, will use only if favicon redirect url is empty"
        )
    }
}