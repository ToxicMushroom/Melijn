package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.services.voice.VOICE_SAFE
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import kotlin.system.exitProcess


class RestartCommand : AbstractCommand("command.restart") {

    init {
        id = 181
        name = "restart"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {

        val players = context.lavaManager.musicPlayerManager.getPlayers()
        val wrapper = context.daoManager.tracksWrapper

        sendMsg(context, "Are you sure you wanna restart ?")

        context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
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

                    VOICE_SAFE.acquire()
                    trackManager.stopAndDestroy()
                    VOICE_SAFE.release()
                }

                sendMsg(context, "Restarting")

                context.taskManager.asyncAfter(3_000) {
                    exitProcess(0)
                }
            } else {
                sendMsg(context, "Alright not restarting :)")
            }
        })
    }
}