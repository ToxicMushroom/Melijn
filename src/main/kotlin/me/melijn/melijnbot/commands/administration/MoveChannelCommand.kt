package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_CHANNELCOMMANDSTATE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class MoveChannelCommand : AbstractCommand("command.moveChannel") {

    init {
        name = "moveChannel"
        aliases = arrayOf("mvC")
        discordPermissions = arrayOf(Permission.MANAGE_CHANNEL)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: ICommandContext) {
        val toMove = getChannelByArgsNMessage(context, 0, true) ?: return
        val position =
            getEnumFromArgNMessage<RelativePosition>(context, 1, MESSAGE_UNKNOWN_CHANNELCOMMANDSTATE) ?: return
        val target = getChannelByArgsNMessage(context, 2, true) ?: return
        val offset = when (position) {
            RelativePosition.ABOVE -> 0
            RelativePosition.BELOW -> 1
        }

        context.container.eventWaiter.waitFor(
            GuildMessageReceivedEvent::class.java,
            { context.channelId == it.channel.idLong && context.authorId == it.author.idLong },
            { event ->
                if (event.message.contentRaw.equals("yes", true)) {
                    // Check if we cannot see any channel in category and thus not see the category
                    if (target.parent?.channels?.none {
                            context.selfMember.hasPermission(it, Permission.VIEW_CHANNEL)
                        } == true
                    ) {
                        sendRsp(
                            context,
                            "Cannot move **%channel%** %relativePosition% **%target%** because I cannot see any channel in **%category%**"
                                .withVariable("channel", toMove.name)
                                .withVariable("target", target.name)
                                .withVariable("category", target.parent?.name ?: "error")
                                .withVariable("relativePosition", position.toLC())
                        )
                        return@waitFor
                    }

                    if (notEnoughPermissionsAndMessage(context, toMove, Permission.VIEW_CHANNEL)) return@waitFor

                    toMove.manager
                        .setParent(target.parent)
                        .setPosition(target.position + offset)
                        .await()

                    sendRsp(
                        context, "Moved **%channel%** %relativePosition% **%target%**"
                            .withVariable("channel", toMove.asTag)
                            .withVariable("target", target.asTag)
                            .withVariable("relativePosition", position.toLC())
                    )
                } else {
                    sendRsp(
                        context, "Okay, not movingye **%channel%**"
                            .withVariable("channel", toMove.asTag)
                    )
                }
            })
        sendRsp(
            context,
            "Are you sure you want to move **%channel%** %relativePosition% **%target%** ?\nRespond `yes` to confirm, `no` to cancel"
                .withVariable("channel", toMove.asTag)
                .withVariable("target", target.asTag)
                .withVariable("relativePosition", position.toLC())
        )


    }
}

enum class RelativePosition {
    ABOVE,
    BELOW,
}