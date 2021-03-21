package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.internals.models.ModularMessage

data class PaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val messageId: Long,
    val messageList: List<String>,
    var currentPage: Int
)

data class ModularPaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val messageId: Long,
    val messageList: List<ModularMessage>,
    var currentPage: Int
)