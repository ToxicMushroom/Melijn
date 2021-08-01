package me.melijn.melijnbot.internals.models

data class SpammingUser(
    val userId: Long,
    val startTime: Long,
    var count: Short,
    var responses: Int // 0b00 lsb = short burst spam response
)