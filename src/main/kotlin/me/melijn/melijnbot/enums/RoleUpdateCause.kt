package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class RoleUpdateCause : ParsableEnum {
    SELF_ROLE,
    FORCE_ROLE,
    JOIN_ROLE,
    UNVERIFIED_ROLE
}