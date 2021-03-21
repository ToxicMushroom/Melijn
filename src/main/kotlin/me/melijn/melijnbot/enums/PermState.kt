package me.melijn.melijnbot.enums

enum class PermState(
    val past: String
) {
    ALLOW("Allowed"),
    DEFAULT("Reset"),
    DENY("Denied");
}