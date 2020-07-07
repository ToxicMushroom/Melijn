package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax

class T2eCommand : AbstractCommand("command.t2e") {

    init {
        id = 127
        name = "t2e"
        children = arrayOf(
            NoSpaceArg(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    class NoSpaceArg(parent: String) : AbstractCommand("$parent.nospace") {

        init {
            name = "noSpace"
        }

        override suspend fun execute(context: CommandContext) {
            t2e(context, false)
        }
    }

    override suspend fun execute(context: CommandContext) {
        t2e(context, true)
    }

    companion object {
        suspend fun t2e(context: CommandContext, spaces: Boolean) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            var output = ""
            for (c in context.rawArg.toCharArray()) {
                val s = c.toString()
                output += when (s) {
                    "a" -> regional("a")
                    "b" -> regional("b")
                    "c" -> regional("c")
                    "d" -> regional("d")
                    "e" -> regional("e")
                    "f" -> regional("f")
                    "g" -> regional("g")
                    "h" -> regional("h")
                    "i" -> regional("i")
                    "j" -> regional("j")
                    "k" -> regional("k")
                    "l" -> regional("l")
                    "m" -> regional("m")
                    "n" -> regional("n")
                    "o" -> regional("o")
                    "p" -> regional("p")
                    "q" -> regional("q")
                    "r" -> regional("r")
                    "s" -> regional("s")
                    "t" -> regional("t")
                    "u" -> regional("u")
                    "v" -> regional("v")
                    "w" -> regional("w")
                    "x" -> regional("x")
                    "y" -> regional("y")
                    "z" -> regional("z")
                    "1" -> letter("one")
                    "2" -> letter("two")
                    "3" -> letter("three")
                    "4" -> letter("four")
                    "5" -> letter("five")
                    "6" -> letter("six")
                    "7" -> letter("seven")
                    "8" -> letter("eight")
                    "9" -> letter("nine")
                    "0" -> letter("zero")
                    else -> s
                } + if (spaces) " " else ""
            }

            sendRsp(context, output)
        }

        private fun regional(input: String): String = ":regional_indicator_$input:"
        private fun letter(input: String): String = ":$input:"
    }


}
