package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*

class SetCooldownCommand : AbstractCommand("command.setcooldown") {

    init {
        id = 17
        name = "setCooldown"
        aliases = arrayOf("scd", "sccd", "setCommandCooldown")
        children = arrayOf(ChannelArg(root), GlobalArg(root), InfoArg(root))
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context, syntax)
    }

    class ChannelArg(parentRoot: String) : AbstractCommand("$parentRoot.channel") {

        init {
            name = "channel"
            aliases = arrayOf("c")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context, syntax)
                return
            }
            val channel = getTextChannelByArgsNMessage(context, 0) ?: return
            val commands = getCommandIdsFromArgNMessage(context, 1) ?: return
            val cooldown = getLongFromArgNMessage(context, 2) ?: return

            val daoWrapper = context.daoManager.commandChannelCoolDownWrapper
            daoWrapper.setCooldowns(channel.guild.idLong, channel.idLong, commands, cooldown)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.response1")
                .replace(PLACEHOLDER_CHANNEL, "#${channel.name}")
                .replace("%commandCount%", commands.size.toString())
                .replace("%commandNode%", context.args[1])
                .replace("%s%", if (commands.size > 1) "s" else "")
                .replace("%cooldown%", cooldown.toString())
            sendMsg(context, msg)
        }
    }

    class GlobalArg(parentRoot: String) : AbstractCommand("$parentRoot.global") {

        init {
            name = "global"
            aliases = arrayOf("g")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context, syntax)
                return
            }
            val commands = getCommandIdsFromArgNMessage(context, 0) ?: return
            val cooldown = getLongFromArgNMessage(context, 1) ?: return

            val daoWrapper = context.daoManager.commandCooldownWrapper
            daoWrapper.setCooldowns(context.getGuildId(), commands, cooldown)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.response1")
                .replace("%commandCount%", commands.size.toString())
                .replace("%commandNode%", context.args[1])
                .replace("%cooldown%", cooldown.toString())
                .replace("%s%", if (commands.size > 1) "s" else "")

            sendMsg(context, msg)
        }
    }

    class InfoArg(parentRoot: String) : AbstractCommand("$parentRoot.info") {

        init {
            name = "info"
            aliases = arrayOf("i", "list", "ls", "l")
        }

        override suspend fun execute(context: CommandContext) {
            val map: Map<String, Long>

            val language = context.getLanguage()
            val daoManager = context.daoManager
            val title: String = if (context.args.isNotEmpty()) {
                val channel = getTextChannelByArgsNMessage(context, 0) ?: return
                map = daoManager.commandChannelCoolDownWrapper.commandChannelCooldownCache.get(channel.idLong).await()
                i18n.getTranslation(language, "$root.response1.title")
                    .replace("%channel%", "#${channel.name}")
            } else {
                map = daoManager.commandCooldownWrapper.commandCooldownCache.get(context.getGuildId()).await()
                i18n.getTranslation(language, "$root.response2.title")
            }

            var content = "```INI"

            for ((index, entry) in map.entries.withIndex()) {
                val cmd = context.getCommands().firstOrNull { cmd -> cmd.id.toString() == entry.key }
                if (cmd != null) {
                    content += "$index - [${cmd.name}]"
                } else {
                    val matcher = ccTagPattern.matcher(entry.key)
                    if (matcher.find()) {
                        content += "$index - CustomCommand - ${matcher.group(1)}}"
                    }
                }
            }
            content += "```"

            val msg = title + content
            sendMsgCodeBlock(context, msg, "INI")
        }
    }
}