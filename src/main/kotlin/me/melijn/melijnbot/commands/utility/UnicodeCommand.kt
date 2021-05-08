package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax


class UnicodeCommand : AbstractCommand("command.unicode") {

    init {
        id = 12
        name = "unicode"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val args = context.rawArg
        if (args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val builder = StringBuilder()
        args.codePoints().forEachOrdered { code ->
            val chars = Character.toChars(code)
            if (chars.size > 1) {
                val hex0 = StringBuilder(Integer.toHexString(chars[0].toInt()).uppercase())
                val hex1 = StringBuilder(Integer.toHexString(chars[1].toInt()).uppercase())
                while (hex0.length < 4)
                    hex0.insert(0, "0")
                while (hex1.length < 4)
                    hex1.insert(0, "0")
                builder.append("`\\u").append(hex0).append("\\u").append(hex1).append("`   ")
            } else {
                val hex = StringBuilder(Integer.toHexString(code).uppercase())
                while (hex.length < 4)
                    hex.insert(0, "0")
                builder.append("`\\u").append(hex).append("`   ")
            }
            builder.append(String(chars)).append("   _").append(Character.getName(code)).append("_\n")
        }
        sendRsp(context, builder.toString().take(2000))
    }
}