package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.notEnoughPermissionsAndMessage
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission

class ClearChannelCommand : AbstractCommand("command.clearchannel") {

    init {
        id = 40
        name = "clearChannel"
        aliases = arrayOf("cChannel")
        discordPermissions = arrayOf(
            Permission.MANAGE_CHANNEL
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        when {
            context.args[0] == "confirm" -> {
                if (context.args.size > 1) {
                    sendSyntax(context)
                    return
                }
                if (notEnoughPermissionsAndMessage(context, context.textChannel, Permission.MANAGE_CHANNEL)) return
                val textChannel = context.textChannel
                val copy = textChannel.createCopy().reason("(clearChannel) ${context.author.asTag}").await() // TODO: fix permision manage_channel missing
                copy.manager.setPosition(textChannel.position)

                textChannel.delete().reason("(clearChannel) ${context.author.asTag}").queue()
            }
            context.args.size > 1 && context.args[1] == "confirm" -> {
                val textChannel = getTextChannelByArgsNMessage(context, 0) ?: return
                if (notEnoughPermissionsAndMessage(context, textChannel, Permission.MANAGE_CHANNEL)) return
                val copy = textChannel.createCopy().reason("(clearChannel) ${context.author.asTag}").await() // TODO: fix permission manage_channel missing
                copy.manager.setPosition(textChannel.position)

                textChannel.delete().reason("(clearChannel) ${context.author.asTag}").queue()
            }
            else -> {
                val msg = context.getTranslation("$root.notconfirm")
                    .withVariable("syntax", context.getTranslation(syntax)
                        .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                    )
                sendRsp(context, msg)
            }
        }
    }
}