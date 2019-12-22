package com.kcibald.services.fronting.objs.responses.bouns

import com.kcibald.services.fronting.objs.responses.ResponseBonus

object ResponseTimeHeaderBonus {
    fun fromNameAndTime(name: String, time: Int): ResponseBonus = HeaderAddingResponseBonus(
        "X-$name-Time" to time.toString()
    )

    fun fromNameAndTime(name: String, time: Long): ResponseBonus = HeaderAddingResponseBonus(
        "X-$name-Time" to time.toString()
    )
}