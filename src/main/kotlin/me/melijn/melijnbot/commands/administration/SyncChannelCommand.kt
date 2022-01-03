package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.await
import me.melijn.melijnbot.internals.utils.getChannelByArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendMelijnMissingChannelPermissionMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Category
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class SyncChannelCommand : AbstractCommand("command.syncchannel") {

    init {
        name = "syncChannel"
        aliases = arrayOf("syncC")
        discordPermissions = arrayOf(Permission.MANAGE_PERMISSIONS)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val guildChannels = (if (context.args.isEmpty()) {
            listOf(context.textChannel)
        } else {
            (0 until context.args.size).map {
                getChannelByArgsNMessage(context, it, true)
                    ?: return
            }
        }).flatMap {
            if (it.parent == null && it.type == ChannelType.CATEGORY) {
                (it as Category).channels
            } else if (it.parent != null) listOf(it)
            else emptyList()
        }

        val guildChannelsStr = guildChannels.joinToString(", ") { it.asMention }

        context.container.eventWaiter.waitFor(
            GuildMessageReceivedEvent::class.java,
            { context.channelId == it.channel.idLong && context.authorId == it.author.idLong },
            { event ->
                if (!event.message.contentRaw.equals("yes", true)) {
                    sendRsp(context, "Okay, not syncing these channels.")
                    return@waitFor
                }
                guildChannels.forEach { guildChannel ->
                    if (!context.selfMember.canSync(guildChannel)) {
                        sendMelijnMissingChannelPermissionMessage(
                            context,
                            listOf(Permission.MANAGE_PERMISSIONS),
                            guildChannel
                        )
                        return@waitFor
                    }
                }

                guildChannels.forEach { guildChannel ->
                    guildChannel.manager.sync().await()
                }

                sendRsp(context, "Synced **%channel%**".withVariable("channel", guildChannelsStr))
            })
        sendRsp(
            context,
            "Are you sure you want to sync **%channel%** ?\nRespond `yes` to confirm, `no` to cancel"
                .withVariable("channel", guildChannelsStr)
        )
    }
}