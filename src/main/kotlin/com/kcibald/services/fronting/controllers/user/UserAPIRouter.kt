package com.kcibald.services.fronting.controllers.user

import com.kcibald.services.fronting.objs.entries.GroupingRouter

object UserAPIRouter: GroupingRouter(
    apiEntries = listOf(DescribeUserAPI)
)