package me.melijn.melijnbot.commands.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.utils.MarkdownSanitizer

class MoveCommand : AbstractCommand("command.move") {

    init {
        id = 155
        name = "move"
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        val player = context.guildMusicPlayer
        val trackManager = player.guildTrackManager
        if (context.args.size < 2) {
            sendSyntax(context)
            return
        }

        val index1 = (getIntegerFromArgNMessage(context, 0, 1, trackManager.trackSize()) ?: return) - 1
        val index2 = (getIntegerFromArgNMessage(context, 1, 1, trackManager.trackSize()) ?: return) - 1

        if (index1 == index2) {
            val msg = context.getTranslation("$root.sameindex")
            sendMsg(context, msg)
            return
        }

        val trackList = trackManager.tracks.toList().toMutableList()
        val track = trackList[index1]
        trackList.removeAt(index1)
        val lastPart = trackList.subList(index2, trackList.size)
        val firstPart = mutableListOf<AudioTrack>()
        firstPart.addAll(trackList.subList(0, index2))
        firstPart.add(track)
        firstPart.addAll(lastPart)

        trackManager.tracks.clear()
        firstPart.forEach { trck -> trackManager.tracks.offer(trck) }

        val msg = context.getTranslation("$root.moved")
            .replace("%pos1%", "$index1")
            .replace("%pos2%", "$index2")
            .replace("%track%", MarkdownSanitizer.escape(track.info.title))

        sendMsg(context, msg)
    }
}