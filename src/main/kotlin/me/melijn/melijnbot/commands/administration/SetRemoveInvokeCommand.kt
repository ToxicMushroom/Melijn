package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.asTag
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable

class SetRemoveInvokeCommand : AbstractCommand("command.setremoveinvoke") {

    init {
        id = 188
        name = "setRemoveInvoke"
        aliases = arrayOf("sri", "setRemoveInvokes")
        children = arrayOf(GlobalArg(root))
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val channel = getTextChannelByArgsNMessage(context, 0) ?: return
        val wrapper = context.daoManager.removeInvokeWrapper

        if (context.args.size == 1) {
            val map = wrapper.removeInvokeCache.get(context.guildId).await()
            val msg = if (map.containsKey(channel.idLong)) {
                context.getTranslation("$root.show.set")
                    .withVariable("channel", channel.asTag)
                    .withVariable("seconds", map[channel.idLong] ?: 1)

            } else {

                context.getTranslation("$root.show.unset")
                    .withVariable("channel", channel.asTag)
            }

            sendRsp(context, msg)

        } else {
            if (context.args[1] == "null") {
                wrapper.remove(context.guildId, channel.idLong)

                val msg = context.getTranslation("$root.unset")
                    .withVariable("channel", channel.asTag)
                sendRsp(context, msg)

            } else {
                val seconds = getIntegerFromArgNMessage(context, 1, 1, 300) ?: return
                wrapper.set(context.guildId, channel.idLong, seconds)

                val msg = context.getTranslation("$root.set")
                    .withVariable("channel", channel.asTag)
                    .withVariable("seconds", seconds)
                sendRsp(context, msg)

            }
        }
    }

    class GlobalArg(parent: String) : AbstractCommand("$parent.global") {

        init {
            name = "global"
            aliases = arrayOf("g")
        }

        override suspend fun execute(context: CommandContext) {
            val guildId = context.guildId
            val wrapper = context.daoManager.removeInvokeWrapper

            if (context.args.isEmpty()) {
                val map = wrapper.removeInvokeCache.get(context.guildId).await()
                val msg = if (map.containsKey(guildId)) {
                    context.getTranslation("$root.show.set")
                        .withVariable("seconds", map[guildId] ?: 1)

                } else {
                    context.getTranslation("$root.show.unset")

                }

                sendRsp(context, msg)

            } else {
                if (context.args[0] == "null") {
                    wrapper.remove(context.guildId, guildId)

                    val msg = context.getTranslation("$root.unset")
                    sendRsp(context, msg)

                } else {
                    val seconds = getIntegerFromArgNMessage(context, 0, 1, 300) ?: return
                    wrapper.set(context.guildId, guildId, seconds)

                    val msg = context.getTranslation("$root.set")
                        .withVariable("seconds", seconds)
                    sendRsp(context, msg)

                }
            }
        }
    }
}