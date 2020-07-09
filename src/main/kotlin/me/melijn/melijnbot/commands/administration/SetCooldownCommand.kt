package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class SetCooldownCommand : AbstractCommand("command.setcooldown") {

    init {
        id = 17
        name = "setCooldown"
        aliases = arrayOf("scd", "sccd", "setCommandCooldown")
        children = arrayOf(
            ChannelArg(root),
            GlobalArg(root),
            InfoArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class ChannelArg(parent: String) : AbstractCommand("$parent.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context)
                return
            }

            val channel = getTextChannelByArgsNMessage(context, 0) ?: return
            val commands = getCommandIdsFromArgNMessage(context, 1) ?: return
            val cooldown = getLongFromArgNMessage(context, 2) ?: return

            val daoWrapper = context.daoManager.commandChannelCoolDownWrapper
            daoWrapper.setCooldowns(channel.guild.idLong, channel.idLong, commands, cooldown)

            val path = "$root.response1" + if (commands.size > 1) {
                ".multiple"
            } else {
                ""
            }
            val msg = context.getTranslation(path)
                .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                .withVariable("commandCount", commands.size.toString())
                .withVariable("commandNode", context.args[1])
                .withVariable("cooldown", cooldown.toString())
            sendRsp(context, msg)
        }
    }

    class GlobalArg(parent: String) : AbstractCommand("$parent.global") {

        init {
            name = "global"
            aliases = arrayOf("g")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val commands = getCommandIdsFromArgNMessage(context, 0) ?: return
            val cooldown = getLongFromArgNMessage(context, 1) ?: return

            val daoWrapper = context.daoManager.commandCooldownWrapper
            daoWrapper.setCooldowns(context.guildId, commands, cooldown)
            val path = "$root.response1" + if (commands.size > 1) {
                ".multiple"
            } else {
                ""
            }

            val msg = context.getTranslation(path)
                .withVariable("commandCount", commands.size.toString())
                .withVariable("commandNode", context.args[0])
                .withVariable("cooldown", cooldown.toString())

            sendRsp(context, msg)
        }
    }

    class InfoArg(parent: String) : AbstractCommand("$parent.info") {

        init {
            name = "info"
            aliases = arrayOf("i", "list", "ls", "l")
        }

        override suspend fun execute(context: CommandContext) {
            val map: Map<String, Long>

            val daoManager = context.daoManager
            val title: String = if (context.args.isNotEmpty()) {
                val channel = getTextChannelByArgsNMessage(context, 0) ?: return

                map = daoManager.commandChannelCoolDownWrapper.commandChannelCooldownCache.get(channel.idLong).await()
                context.getTranslation("$root.response1.title")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
            } else {
                map = daoManager.commandCooldownWrapper.commandCooldownCache.get(context.guildId).await()
                context.getTranslation("$root.response2.title")
            }

            var content = "```INI"

            for ((index, entry) in map.entries.withIndex()) {
                val cmd = context.commandList.firstOrNull { cmd -> cmd.id.toString() == entry.key }
                if (cmd != null) {
                    content += "\n$index - [${cmd.name}] - ${entry.value}"
                } else {
                    val matcher = ccTagPattern.matcher(entry.key)
                    if (matcher.find()) {
                        content += "\n$index - CustomCommand - [${matcher.group(1)}] - ${entry.value}"
                    }
                }
            }
            content += "```"

            val msg = title + content
            sendRspCodeBlock(context, msg, "INI")
        }
    }
}