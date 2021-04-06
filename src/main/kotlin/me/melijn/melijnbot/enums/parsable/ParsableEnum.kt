package me.melijn.melijnbot.enums.parsable

interface ParsableEnum {
    fun aliases(): Set<String> = emptySet()
}