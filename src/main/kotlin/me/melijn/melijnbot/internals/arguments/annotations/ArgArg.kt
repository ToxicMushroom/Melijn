package me.melijn.melijnbot.internals.arguments.annotations

class ArgArg(
    index: Int,
    val unsetable: Boolean = false, // this references to "null" not kotlin nullability
    val errorable: Boolean = false
) : CommandArg(index)
