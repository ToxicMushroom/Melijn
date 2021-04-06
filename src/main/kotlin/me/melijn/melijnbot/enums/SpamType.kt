package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class SpamType : ParsableEnum {
    FAST_MESSAGE,
    MANY_CAPS,
    MANY_MENTIONS
}