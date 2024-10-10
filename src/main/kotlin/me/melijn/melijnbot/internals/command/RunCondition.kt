package me.melijn.melijnbot.internals.command

enum class RunCondition(val preRequired: Array<RunCondition> = emptyArray()) {
    GUILD,
    DEV_ONLY,
    CHANNEL_NSFW(arrayOf(GUILD)),
    USER_SUPPORTER,
    GUILD_SUPPORTER(arrayOf(GUILD)),
    EXPLICIT_MELIJN_PERMISSION(arrayOf(GUILD))
}