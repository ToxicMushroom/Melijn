package me.melijn.melijnbot.objects.command

enum class RunCondition(val preRequired: Array<RunCondition> = emptyArray()) {
    GUILD,
    VC_BOT_ALONE_OR_USER_DJ(arrayOf(GUILD)),
    VC_BOT_OR_USER_DJ(arrayOf(GUILD)),
    PLAYING_TRACK_NOT_NULL(arrayOf(GUILD)),
    SAME_VC_BOT_ALONE_OR_USER_DJ(arrayOf(GUILD)),
    DEV_ONLY,
    BOT_ALONE_OR_USER_DJ(arrayOf(GUILD)),
    CHANNEL_NSFW(arrayOf(GUILD)),
    VOTED
}