package me.melijn.melijnbot.internals.arguments

annotation class CommandArg(

    val index: Int,
    val optional: Boolean = false,

    val mode: ArgumentMode = ArgumentMode.POSITIONAL,

    val canBeAttachment: Boolean = false,

    // many args go into this variable: >cmd <@ToxicMushroom @Stanzerelli @ByteAlex> go into List<User>
    val many: Boolean = false,
    val manyLimit: Int = 50,
    val manyStart: Int = 1,


    val flag: String = ""
) {

}

annotation class IntArg(
    val min: Int = Int.MIN_VALUE,
    val max: Int = Int.MAX_VALUE
)

