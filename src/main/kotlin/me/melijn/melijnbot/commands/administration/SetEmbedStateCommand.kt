package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.boolFromStateArg
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax
import me.melijn.melijnbot.objects.utils.withVariable

class SetEmbedStateCommand : AbstractCommand("command.setembedstate") {

    init {
        id = 3
        name = "setEmbedState"
        aliases = arrayOf("ses")
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
            else -> sendSyntax(context)
        }
    }

    private suspend fun sendCurrentEmbedState(context: CommandContext) {
        val dao = context.daoManager.embedDisabledWrapper
        val disabled = dao.embedDisabledCache.contains(context.guildId)

        val msg = context.getTranslation("$root.currentstateresponse")
            .withVariable("disabledState", if (disabled) "disabled" else "enabled")

        sendRsp(context, msg)
    }

    private suspend fun setEmbedStateState(context: CommandContext) {
        val state: Boolean? = boolFromStateArg(context.args[0].toLowerCase())
        if (state == null) {
            sendSyntax(context)
            return
        }

        val dao = context.daoManager.embedDisabledWrapper
        dao.setDisabled(context.guildId, !state)

        val msg = context.getTranslation("$root.set.success")
            .withVariable("disabledState", if (state) "enabled" else "disabled")
        sendRsp(context, msg)
    }
}