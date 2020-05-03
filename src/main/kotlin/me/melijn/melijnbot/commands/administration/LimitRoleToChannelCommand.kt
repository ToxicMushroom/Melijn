package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class LimitRoleToChannelCommand : AbstractCommand("command.limitroletochannel") {

    init {
        id = 161
        name = "limitRoleToChannel"
        aliases = arrayOf("lrtc")
        discordPermissions = arrayOf(Permission.MANAGE_CHANNEL)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }
        val role = getRoleByArgsNMessage(context, 0) ?: return
        val immuneChannel = getTextChannelByArgsNMessage(context, 1) ?: return

        val message = context.getTranslation("$root.confirmation")
            .replace(PLACEHOLDER_ROLE, role.name)
            .replace(PLACEHOLDER_CHANNEL, immuneChannel.asTag)

        sendMsg(context, message)

        context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
            it.channel.idLong == context.channelId && it.author.idLong == context.authorId
        }, {
            if (it.message.contentRaw == "yes") {
                val channels = context.guild.channels
                var failed = 0
                for (channel in channels) {
                    if (channel.idLong == immuneChannel.idLong) {
                        if (context.selfMember.hasPermission(channel, Permission.MANAGE_CHANNEL)) {
                            channel.putPermissionOverride(role)
                                .grant(Permission.VIEW_CHANNEL)
                                .reason("limitRoleToChannel")
                                .queue()
                        } else failed++
                    }
                    if (role.hasPermission(channel, Permission.VIEW_CHANNEL)) {
                        if (context.selfMember.hasPermission(channel, Permission.MANAGE_CHANNEL)) {
                            channel.putPermissionOverride(role)
                                .deny(Permission.VIEW_CHANNEL)
                                .reason("limitRoleToChannel")
                                .queue()
                        } else failed++
                    }
                }

                val msg = (
                    if (failed == 0) context.getTranslation("$root.finished")
                    else context.getTranslation("$root.finished.failed")
                    )
                    .replace(PLACEHOLDER_ROLE, role.name)
                    .replace(PLACEHOLDER_CHANNEL, immuneChannel.asTag)
                    .replace("%failed%", "$failed")
                sendMsg(context, msg)
            } else {
                val msg = context.getTranslation("$root.cancelled")
                sendMsg(context, msg)
            }
        })
    }
}