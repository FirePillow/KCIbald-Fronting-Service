package com.kcibald.services.fronting.controllers.post

import com.kcibald.services.fronting.objs.entries.GroupingRouter

object PostRouter : GroupingRouter(
    apiEntries = listOf(DescribePost)
)