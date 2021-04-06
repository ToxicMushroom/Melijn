package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class ChannelType : ParsableEnum {
    JOIN,
    LEAVE,
    KICKED,
    BANNED,
    SELFROLE,
    VERIFICATION,
    BIRTHDAY,
    PRE_VERIFICATION_JOIN,
    PRE_VERIFICATION_LEAVE,
    BOOST,
    STARBOARD;
}