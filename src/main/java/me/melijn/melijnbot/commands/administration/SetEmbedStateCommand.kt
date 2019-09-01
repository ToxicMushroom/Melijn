package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.boolFromStateArg
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetEmbedStateCommand : AbstractCommand("command.setembedstate") {

    init {
        id = 3
        name = "setEmbedState"
        help = Translateable("message.help.state")
        syntax = Translateable("$root.syntax")
        aliases = arrayOf("ses")
        description = Translateable("$root.description")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        when {
            context.args.isEmpty() -> {
                sendCurrentEmbedState(context)
            }
            context.args.size == 1 -> {
                setEmbedStateState(context)
            }
            else -> sendSyntax(context, "$root.syntax")
        }
    }

    private fun sendCurrentEmbedState(context: CommandContext) {
        val dao = context.daoManager.embedDisabledWrapper
        val state = dao.embedDisabledCache.contains(context.getGuildId())

        sendMsg(context, replaceState(
                Translateable("$root.currentstateresponse").string(context),
                state
        ))
    }

    private fun setEmbedStateState(context: CommandContext) {
        var disabledState: Boolean? = boolFromStateArg(context.args[0].toLowerCase())
        if (disabledState == null) {
            sendSyntax(context, syntax)
            return
        }
        disabledState = !disabledState

        val dao = context.daoManager.embedDisabledWrapper
        dao.setDisabled(context.getGuildId(), disabledState)

        sendMsg(context, replaceState(
                Translateable("$root.set.success").string(context),
                disabledState
        ))
    }



    private fun replaceState(msg: String, disabledState: Boolean): String {
        return msg.replace("%disabledState%", if (disabledState) "disabled" else "enabled")
    }
}