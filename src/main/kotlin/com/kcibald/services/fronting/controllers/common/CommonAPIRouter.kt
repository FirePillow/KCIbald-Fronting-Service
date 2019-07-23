package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.objs.entries.GroupingRouter

object CommonAPIRouter : GroupingRouter(
    fancyEntry = listOf(Login),
    apiEntries = listOf(Logout)
)