package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.internals.utils.asTag
import me.melijn.melijnbot.internals.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.internals.utils.getTextChannelByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
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

    suspend fun execute(context: ICommandContext) {
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }
        val role = getRoleByArgsNMessage(context, 0) ?: return
        val immuneChannel = getTextChannelByArgsNMessage(context, 1) ?: return

        val message = context.getTranslation("$root.confirmation")
            .withVariable(PLACEHOLDER_ROLE, role.name)
            .withVariable(PLACEHOLDER_CHANNEL, immuneChannel.asTag)

        sendRsp(context, message)

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
                                .reason("(limitRoleToChannel) ${context.author.asTag}")
                                .queue()
                        } else failed++
                    }
                    if (role.hasPermission(channel, Permission.VIEW_CHANNEL)) {
                        if (context.selfMember.hasPermission(channel, Permission.MANAGE_CHANNEL)) {
                            channel.putPermissionOverride(role)
                                .deny(Permission.VIEW_CHANNEL)
                                .reason("(limitRoleToChannel) ${context.author.asTag}")
                                .queue()
                        } else failed++
                    }
                }

                val msg = (
                    if (failed == 0) context.getTranslation("$root.finished")
                    else context.getTranslation("$root.finished.failed")
                    )
                    .withVariable(PLACEHOLDER_ROLE, role.name)
                    .withVariable(PLACEHOLDER_CHANNEL, immuneChannel.asTag)
                    .withVariable("failed", "$failed")
                sendRsp(context, msg)
            } else {
                val msg = context.getTranslation("$root.cancelled")
                sendRsp(context, msg)
            }
        })
    }
}