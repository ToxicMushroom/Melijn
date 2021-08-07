package me.melijn.melijnbot.enums

val option = RegexOption.IGNORE_CASE

enum class PatternPermission(val pattern: Regex) {
    CUSTOM_COMMAND("cc\\.(\\d+)".toRegex(option)), // cc.id
    REACTION_ROLE("rr\\.(?:(?!\\.).)*\\.(?:(?!\\.).)*".toRegex(option)), // rr.group.role
    REACTION_ROLE_GROUP("rr\\.(?:(?!\\.).)*".toRegex(option)) // rr.group.role
}