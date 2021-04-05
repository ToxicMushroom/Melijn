package me.melijn.melijnbot.internals.arguments

data class ArgumentInfo(
    val argumentInformation: CommandArg?, // argumentInformation, contains stuff like index
    val extraInformation: Any?, // extraInformation can be casted to an annotationInterface using the generic type of the argument
    val argParser: CommandArgParser<Any>? // parser for the generic type of the argument
)