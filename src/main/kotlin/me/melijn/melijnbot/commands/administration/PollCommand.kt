package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.commands.utility.T2eCommand.Companion.letter
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsN
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspAwaitEL
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission

class PollCommand : AbstractCommand("command.poll") {

    init {
        id = 126
        name = "poll"
        children = arrayOf(
            AddArg(root),
            AddTimedArg(root)
        )
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ADD_REACTION)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
        return
    }


    class AddTimedArg(parent: String) : AbstractCommand("$parent.addtimed") {

        init {
            name = "addTimed"
        }

        //>poll addTimed *d [textChannel] "question?" "a" "B"
        //>poll addTimed *d "question?" "a" "B"
        suspend fun execute(context: ICommandContext) {
            if (context.args.size < 4) {
                sendSyntax(context)
                return
            }
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
        }

        //>poll add [textChannel] "question?" "a" "B"
        //>poll add "question?" "a" "B"
        suspend fun execute(context: ICommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context)
                return
            }
            val textChannel = getTextChannelByArgsN(context, 0)
            val args = if (textChannel != null) {
                context.args.subList(1, context.args.size)
            } else {
                context.args
            }
            if (args.size > 10) {
                val msg = context.getTranslation("$root.tomanyarguments")
                    .withVariable("max", 10)
                    .withVariable("size", args.size)
                sendRsp(context, msg)
                return
            }

            val absoluteChannel = textChannel ?: context.textChannel
            val answerList = args.subList(1, args.size)
            val answers =
                answerList.withIndex().joinToString("\n") { "${getEmoji((it.index + 1).toString())} ${it.value}" }
            val msg = sendRspAwaitEL(absoluteChannel, context.daoManager, "**${args[0]}**\n\n" + answers).firstOrNull()
                ?: return
            for (i in answerList.indices) {
                getEmojiraw(i + 1)?.let { msg.addReaction(it).queue() }
            }
        }

        private fun getEmoji(it: String): String {
            return when (it) {
                "1" -> letter("one")
                "2" -> letter("two")
                "3" -> letter("three")
                "4" -> letter("four")
                "5" -> letter("five")
                "6" -> letter("six")
                "7" -> letter("seven")
                "8" -> letter("eight")
                "9" -> letter("nine")
                "10" -> letter("keycap_ten")
                else -> it
            }
        }

        private fun getEmojiraw(it: Int): String? {
            return when (it) {
                1 -> "\u0031\u20E3"
                2 -> "\u0032\u20E3"
                3 -> "\u0033\u20E3"
                4 -> "\u0034\u20E3"
                5 -> "\u0035\u20E3"
                6 -> "\u0036\u20E3"
                7 -> "\u0037\u20E3"
                8 -> "\u0038\u20E3"
                9 -> "\u0039\u20E3"
                10 -> "\uD83D\uDD1F"
                else -> null
            }
        }
    }
}
