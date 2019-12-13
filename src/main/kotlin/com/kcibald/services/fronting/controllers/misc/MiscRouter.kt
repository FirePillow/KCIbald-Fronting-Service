package com.kcibald.services.fronting.controllers.misc

import com.kcibald.services.fronting.objs.entries.GroupingRouter

object MiscRouter: GroupingRouter(
    htmlEntries = listOf(StaticMiscResourceRouter)
)