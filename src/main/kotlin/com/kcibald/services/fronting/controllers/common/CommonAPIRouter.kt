package com.kcibald.services.fronting.controllers.common

import com.kcibald.services.fronting.objs.APIEntry
import com.kcibald.services.fronting.objs.GroupingRouter
import com.kcibald.services.fronting.objs.HTMLContentEntry

object CommonAPIRouter : GroupingRouter(
    "/",
    listOf<APIEntry>(
        Login
    ),
    listOf<HTMLContentEntry>(
        Login
    )
)