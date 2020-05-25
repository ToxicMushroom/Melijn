package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.*

class RemoveCommand : AbstractCommand("command.remove") {

    init {
        name = "remove"
        aliases = arrayOf("deleteTrack", "removeTrack")
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL)
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }
        val trackManager = context.guildMusicPlayer.guildTrackManager
        val indexes = getIntegersFromArgsNMessage(context, 1, trackManager.trackSize()) ?: return
        val removed = trackManager.removeAt(indexes)

        var msg = context.getTranslation("$root.removed")
            .replace("%count%", removed.size.toString())
        for ((index, track) in removed) {
            msg += "\n[#${index + 1}](${track.info.uri}) - ${track.info.title}"
        }
        val queueParts = StringUtils.splitMessage(msg)
        val eb = Embedder(context)
        for (queue in queueParts) {
            eb.setDescription(queue)
            sendEmbed(context, eb.build())
        }
    }

    private suspend fun getIntegersFromArgsNMessage(context: CommandContext, start: Int, end: Int): IntArray? {
        val args = context.rawArg.remove(" ").split(",")
        val ints = mutableListOf<Int>()
        try {
            for (arg in args) {
                if (arg.contains("-")) {

                    val list: List<String> = arg.split("-".toRegex())
                    if (list.size == 2) {
                        val first = list[0]
                        val second = list[1]
                        if (first.isNumber() && second.isNumber()) {
                            val firstInt = first.toInt()
                            val secondInt = second.toInt()
                            for (i in firstInt..secondInt)
                                ints.addIfNotPresent(i - 1)
                        }
                    } else {
                        val msg = context.getTranslation("message.unknown.numberornumberrange")
                            .replace(PLACEHOLDER_ARG, arg)
                        sendMsg(context, msg)
                        return null
                    }
                } else if (arg.isNumber()) {
                    if (!ints.contains(arg.toInt() - 1)) {
                        ints.add(arg.toInt() - 1)
                    }
                } else {
                    val msg = context.getTranslation("message.unknown.numberornumberrange")
                        .replace(PLACEHOLDER_ARG, arg)
                    sendMsg(context, msg)
                    return null
                }
            }

        } catch (e: NumberFormatException) {
            val msg = context.getTranslation("message.numbertobig")
                .replace(PLACEHOLDER_ARG, e.message ?: "/")
            sendMsg(context, msg)
            return null
        }
        for (i in ints) {
            if (i < start - 1 || i > end - 1) {
                val msg = context.getTranslation("message.number.notinrange")
                    .replace(PLACEHOLDER_ARG, (i + 1).toString())
                    .replace("%start%", start.toString())
                    .replace("%end%", end.toString())
                sendMsg(context, msg)
                return null
            }
        }
        return ints.toIntArray()
    }
}

