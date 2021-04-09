package me.melijn.melijnbot.internals.arguments.annotations

import me.melijn.melijnbot.internals.arguments.ArgumentMode

abstract class CommandArg(
    open val index: Int,
    val optional: Boolean = false,
    val mode: ArgumentMode = ArgumentMode.POSITIONAL,
    val canBeAttachment: Boolean = false,
    val flag: String = ""
)