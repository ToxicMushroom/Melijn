package me.melijn.melijnbot.internals.models

import me.melijn.melijnbot.enums.Alignment

data class Cell(
    val value: String,
    val alignment: Alignment = Alignment.LEFT
)

