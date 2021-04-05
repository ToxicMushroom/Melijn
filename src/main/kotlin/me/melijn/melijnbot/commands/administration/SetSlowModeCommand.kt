package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.Permission

class SetSlowModeCommand : AbstractCommand("command.setslowmode") {

    init {
        id = 78
        name = "setSlowMode"
        aliases = arrayOf("ssm")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        val msg: String = if (context.args.isEmpty()) {
            val channel = context.textChannel
            val slowMode = channel.slowmode

            if (slowMode == 0) {
                context.getTranslation("$root.show.unset")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
            } else {
                context.getTranslation("$root.show.set")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                    .withVariable("slowMode", slowMode.toString())
            }
        } else if (context.args.size == 1) {
            var channel = getTextChannelByArgsN(context, 0, true)
            val number = getIntegerFromArgN(context, 0, 0, 21600)
            if (channel != null) {
                val slowMode = channel.slowmode

                if (slowMode == 0) {
                    context.getTranslation("$root.show.unset")
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                } else {
                    context.getTranslation("$root.show.set")
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                        .withVariable("slowMode", slowMode.toString())
                }
            } else if (number != null) {
                channel = context.textChannel
                if (notEnoughPermissionsAndMessage(context, channel, Permission.MANAGE_CHANNEL)) return
                channel.manager.setSlowmode(number).queue()

                if (number == 0) {
                    context.getTranslation("$root.unset")
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                } else {
                    context.getTranslation("$root.set")
                        .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                        .withVariable("slowMode", number.toString())
                }
            } else {
                sendSyntax(context)
                return
            }
        } else {
            val channel = getTextChannelByArgsNMessage(context, 0, true) ?: return
            val number = getIntegerFromArgNMessage(context, 1, 0, 21600) ?: return
            if (notEnoughPermissionsAndMessage(context, channel, Permission.MANAGE_CHANNEL)) return
            channel.manager.setSlowmode(number).queue()

            if (number == 0) {
                context.getTranslation("$root.unset")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
            } else {
                context.getTranslation("$root.set")
                    .withVariable(PLACEHOLDER_CHANNEL, channel.asTag)
                    .withVariable("slowMode", number.toString())
            }

        }
        sendRsp(context, msg)
    }
}