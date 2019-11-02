package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission

class SetSlowModeCommand : AbstractCommand("command.setslowmode") {

    init {
        id = 78
        name = "setSlowMode"
        aliases = arrayOf("ss")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        val msg: String = if (context.args.isEmpty()) {
            val channel = context.getTextChannel()
            val slowMode = channel.slowmode

            if (slowMode == 0) {
                i18n.getTranslation(context, "$root.show.unset")
                    .replace("%channel%", channel.asTag)
            } else {
                i18n.getTranslation(context, "$root.show.set")
                    .replace("%channel%", channel.asTag)
                    .replace("%slowMode%", slowMode.toString())
            }
        } else if (context.args.size == 1) {
            var channel = getTextChannelByArgsN(context, 0, true)
            val number = getIntegerFromArgN(context, 0, 0, 21600)
            if (channel != null) {
                val slowMode = channel.slowmode

                if (slowMode == 0) {
                    i18n.getTranslation(context, "$root.show.unset")
                        .replace("%channel%", channel.asTag)
                } else {
                    i18n.getTranslation(context, "$root.show.set")
                        .replace("%channel%", channel.asTag)
                        .replace("%slowMode%", slowMode.toString())
                }
            } else if (number != null) {
                channel = context.getTextChannel()
                if (notEnoughPermissionsAndNMessage(context, channel, Permission.MANAGE_CHANNEL)) return
                channel.manager.setSlowmode(number).queue()

                if (number == 0) {
                    i18n.getTranslation(context, "$root.unset")
                        .replace("%channel%", channel.asTag)
                } else {
                    i18n.getTranslation(context, "$root.set")
                        .replace("%channel%", channel.asTag)
                        .replace("%slowMode%", number.toString())
                }
            } else {
                sendSyntax(context)
                return
            }
        } else {
            val channel = getTextChannelByArgsNMessage(context, 0, true) ?: return
            val number = getIntegerFromArgNMessage(context, 1, 0, 21600) ?: return
            if (notEnoughPermissionsAndNMessage(context, channel, Permission.MANAGE_CHANNEL)) return
            channel.manager.setSlowmode(number).queue()
            if (number == 0) {
                i18n.getTranslation(context, "$root.unset")
                    .replace("%channel%", channel.asTag)
            } else {
                i18n.getTranslation(context, "$root.set")
                    .replace("%channel%", channel.asTag)
                    .replace("%slowMode%", number.toString())
            }

        }
        sendMsg(context, msg)
    }
}