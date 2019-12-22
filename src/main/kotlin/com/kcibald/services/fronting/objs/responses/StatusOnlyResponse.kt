package com.kcibald.services.fronting.objs.responses

import com.kcibald.services.fronting.objs.responses.bouns.StatusResponseBonus

fun statusOnlyTerminationResponse(statusCode: Int) = StatusResponseBonus(statusCode) + TerminationResponse