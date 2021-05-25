package me.melijn.melijnbot.enums

val option = RegexOption.IGNORE_CASE

enum class PatternPermission(val pattern: Regex) {
    CUSTOM_COMMAND("cc\\.(\\d+)".toRegex(option)),
    REACTION_ROLE("rr\\.(?:(?!\\.).)*\\.(?:(?!\\.).)*".toRegex(option))
}