package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import kotlin.system.exitProcess


class RestartCommand : AbstractCommand("command.restart") {

    init {
        id = 181
        name = "restart"
        commandCategory = CommandCategory.DEVELOPER
    }

    suspend fun execute(context: ICommandContext) {
        val players = context.lavaManager.musicPlayerManager.getPlayers()
        val wrapper = context.daoManager.tracksWrapper
        wrapper.clear()

        sendRsp(context, "Are you sure you wanna restart ?")

        context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, {
            it.channel.idLong == context.channelId && it.author.idLong == context.authorId
        }, {
            if (it.message.contentRaw == "yes") {
                context.container.shuttingDown = true
                context.container.restServer?.stop()

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

                sendRsp(context, "Restarting")
                context.shardManager.shutdown()

                TaskManager.asyncAfter(3_000) {
                    exitProcess(0)
                }
            } else {
                sendRsp(context, "Alright not restarting :)")
            }
        })
    }
}