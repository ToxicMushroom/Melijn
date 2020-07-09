package me.melijn.melijnbot.internals.command

enum class CommandCategory(val runCondition: Array<RunCondition> = emptyArray()) {
    DEVELOPER(arrayOf(RunCondition.DEV_ONLY)),
    MUSIC(arrayOf(RunCondition.GUILD)),
    ANIMAL,
    ANIME,
    IMAGE,
    UTILITY,
    MODERATION(arrayOf(RunCondition.GUILD)),
    ADMINISTRATION(arrayOf(RunCondition.GUILD))
}