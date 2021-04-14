package me.melijn.melijnbot.internals.models

data class CommandUsageInfo(
    val guildId: Long,
    val userId: Long,
    val commandId: Int,
    val time: Long)