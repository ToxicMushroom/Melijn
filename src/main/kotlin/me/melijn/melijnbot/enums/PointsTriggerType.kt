package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class PointsTriggerType : ParsableEnum {
    FILTERED_MESSAGE,
    WARN,
    FAST_MESSAGE,
    MANY_CAPS,
    MANY_MENTIONS,
    UNVERIFIED_DURATION,
    HOISTING
}