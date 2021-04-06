package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.enums.parsable.ParsableEnum
import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.toUCC
import me.melijn.melijnbot.internals.utils.toUCSC

class EnumArgParser<E> : CommandArgParser<E>() where E : Enum<E>, E : ParsableEnum {

    override suspend fun parse(context: ICommandContext, arg: String): E? {
        val generic = this::class.java.genericSuperclass as E
        val enums = generic.javaClass.enumConstants
        for (enum in enums) {
            val matches = enum.toString().equals(arg, true) ||
                enum.toUCC().equals(arg, true) ||
                enum.toUCSC().equals(arg, true) ||
                enum.aliases().any { it.equals(arg, true) }
            if (matches) return enum
        }

        return null
    }
}