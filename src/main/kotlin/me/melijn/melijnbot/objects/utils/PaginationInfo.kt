package me.melijn.melijnbot.objects.utils

data class PaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val messageId: Long,
    val messageList: List<String>,
    var currentPage: Int
)