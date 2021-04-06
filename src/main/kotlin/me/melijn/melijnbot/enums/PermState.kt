package me.melijn.melijnbot.enums

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class PermState(
    val past: String
): ParsableEnum {
    ALLOW("Allowed"),
    DEFAULT("Reset"),
    DENY("Denied");

    override fun aliases(): Set<String> = setOf(past)
}