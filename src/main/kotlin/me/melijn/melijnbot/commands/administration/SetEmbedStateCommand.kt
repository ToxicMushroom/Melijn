package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.boolFromStateArg
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetEmbedStateCommand : AbstractCommand("command.setembedstate") {

    init {
        id = 3
        name = "setEmbedState"
        help = "message.help.state"
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
        val state = dao.embedDisabledCache.contains(context.guildId)

        val language = context.getLanguage()
        val unReplaceMsg = i18n.getTranslation(language, "$root.currentstateresponse")
        val msg = replaceState(
            unReplaceMsg,
            state
        )
        sendMsg(context, msg)
    }

    private suspend fun setEmbedStateState(context: CommandContext) {
        val state: Boolean? = boolFromStateArg(context.args[0].toLowerCase())
        if (state == null) {
            sendSyntax(context)
            return
        }

        val dao = context.daoManager.embedDisabledWrapper
        dao.setDisabled(context.guildId, state)

        val language = context.getLanguage()
        val unReplaceMsg = i18n.getTranslation(language, "$root.set.success")
        val msg = replaceState(
            unReplaceMsg,
            state
        )
        sendMsg(context, msg)
    }


    private fun replaceState(msg: String, state: Boolean): String {
        return msg.replace("%disabledState%", if (state) "enabled" else "disabled")
    }
}