package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.internals.models.ModularMessage

data class FetchingPaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val messageId: Long,
    val fetcher: suspend ((Int) -> String),
    var pages: Int,
    var currentPage: Int
)

data class StoragePaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val messageId: Long,
    val storage: List<String>,
    var currentPage: Int
)

data class ModularFetchingPaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val messageId: Long,
    val fetcher: suspend ((Int) -> ModularMessage),
    var pages: Int,
    val currentPage: Int
)

data class ModularStoragePaginationInfo(
    val guildId: Long,
    val channelId: Long,
    val authorId: Long,
    val messageId: Long,
    val storage: List<ModularMessage>,
    var currentPage: Int
)