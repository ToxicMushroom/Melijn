package me.melijn.melijnbot.commands.administration

import io.ktor.client.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class SyncChannelCommand : AbstractCommand("command.moveandsyncchannel") {

    init {
        name = "syncChannel"
        aliases = arrayOf("syncC")
        discordPermissions = arrayOf(Permission.MANAGE_CHANNEL)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val textChannel = if (context.args.isEmpty()) {
            context.textChannel
        } else {
            getTextChannelByArgsNMessage(context, 0, true)
                ?: return
        }

        context.container.eventWaiter.waitFor(
            GuildMessageReceivedEvent::class.java,
            { context.channelId == it.channel.idLong && context.authorId == it.author.idLong },
            { event ->
                if (!event.message.contentRaw.equals("yes", true)) {
                    sendRsp(
                        context, "Okay, not syncing **%channel%**"
                            .withVariable("channel", textChannel.asTag)
                    )
                    return@waitFor
                }
                if (context.selfMember.canSync(textChannel)) {
                    if (textChannel.parent == null) {
                        sendRsp(
                            context,
                            "I cannot sync **%channel%** because I don't have permission to do so"
                                .withVariable("channel", textChannel.asTag)
                        )
                        return@waitFor
                    }
                    textChannel.manager.sync().await()
                    sendRsp(
                        context, "Synced **%channel%** with it's parent category **%category%**"
                            .withVariable("channel", textChannel.asTag)
                            .withVariable("category", textChannel.parent?.name ?: "error")
                    )
                }
            })
        sendRsp(
            context,
            "Are you sure you want to sync **%channel%** with it's parent category **%category%** ?\nRespond `yes` to confirm, `no` to cancel"
                .withVariable("channel", textChannel.asTag)
                .withVariable("category", textChannel.parent?.name ?: "error")
        )
    }
}