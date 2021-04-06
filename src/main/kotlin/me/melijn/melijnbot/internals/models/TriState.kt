package me.melijn.melijnbot.internals.models

import me.melijn.melijnbot.enums.parsable.ParsableEnum

enum class TriState(val aliases: Set<String>) : ParsableEnum {
    TRUE(setOf("yes", "enable", "enabled", "allow", "allowed")),
    DEFAULT(setOf("neutral", "normal")),
    FALSE(setOf("no", "disable", "disabled", "deny", "denied"));

    fun index(): Int = values().indexOf(this)
    override fun aliases(): Set<String> = aliases
}