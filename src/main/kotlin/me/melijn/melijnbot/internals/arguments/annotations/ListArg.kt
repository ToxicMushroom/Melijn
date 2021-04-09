package me.melijn.melijnbot.internals.arguments.annotations


class ListArg(
    // many args go into this variable: >cmd <@ToxicMushroom @Stanzerelli @ByteAlex> go into List<User>
    override val index: Int,
    val many: Boolean = false,
    val manyLimit: Int = 50,
    val manyStart: Int = 1,
) : CommandArg(index)