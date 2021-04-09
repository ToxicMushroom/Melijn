package me.melijn.melijnbot.internals.arguments.annotations

class IntArg(
    override val index: Int,
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE
) : CommandArg(index)