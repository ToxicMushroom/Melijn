package me.melijn.melijnbot.internals.arguments.annotations

@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.TYPE)
annotation class UserArg(
    val retrieve: Boolean = true
)