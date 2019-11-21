package com.kcibald.services.fronting.controllers

import com.uchuhimo.konf.ConfigSpec

object MasterConfigSpec: ConfigSpec("") {
    val httpPort by optional(8080, "http_port")
    object Authentication: ConfigSpec("authentication") {
        val CookieKey by optional("_kb_aut", "cookie_key")
        val RecaptchaThreshold by optional(0.7, "login_recaptcha_threshold", "should be less than 1")
        val RedirectURLWhenLogout by optional("/login", "redirect_url_when_logout")
    }
}