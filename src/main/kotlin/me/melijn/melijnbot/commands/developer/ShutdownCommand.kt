package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ShutdownCommand : AbstractCommand("command.shutdown") {

    init {
        id = 123
        name = "shutdown"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val players = context.lavaManager.musicPlayerManager.getPlayers()
        val wrapper = context.daoManager.tracksWrapper
        wrapper.clear()

        sendRsp(context, "Are you sure you wanna shutdown ?")

        context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, {
            it.channel.idLong == context.channelId && it.author.idLong == context.authorId
        }, {
            if (it.message.contentRaw == "yes") {
                context.container.shuttingDown = true

                for ((guildId, player) in HashMap(players)) {
                    val guild = context.shardManager.getGuildById(guildId) ?: continue
                    val channel = context.lavaManager.getConnectedChannel(guild) ?: continue
                    val trackManager = player.guildTrackManager
                    val pTrack = trackManager.playingTrack ?: continue

                    pTrack.position = trackManager.iPlayer.trackPosition

                    wrapper.put(guildId, context.selfUser.idLong, pTrack, trackManager.tracks)
                    wrapper.addChannel(guildId, channel.idLong)

                    trackManager.stopAndDestroy()
                }

                sendRsp(context, "Detached all listeners, saved queues. Ready for termination.")
            } else {
                sendRsp(context, "Okay, not shutting down :p")
            }
        })
    }
}