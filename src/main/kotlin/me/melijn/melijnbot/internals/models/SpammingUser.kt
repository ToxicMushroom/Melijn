package me.melijn.melijnbot.internals.models

data class SpammingUser(
    val userId: Long,
    val startTime: Long,
    var count: Short
)