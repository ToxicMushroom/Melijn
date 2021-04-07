package me.melijn.melijnbot.internals.arguments

class Arg<T>(val value: T?) {

    var nullReason: ArgNullReason? = null

    // Optional arguments can be missing and thus null
    var isMissing: Boolean = nullReason == ArgNullReason.MISSING

    // Nullable arguments can be "null"
    var isNull: Boolean = nullReason == ArgNullReason.NULL_TEXT

    // If argument had a parsing error can be null
    var isParsingError: Boolean = nullReason == ArgNullReason.PARSING_ERROR

    val valueX: T
        get() = value
            ?: throw IllegalStateException("tried getting valueX without correctly checking isNull, value was null")
}