package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

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
        val trackManager = context.getGuildMusicPlayer().guildTrackManager
        val indexes = getIntegersFromArgsNMessage(context, 1, trackManager.trackSize()) ?: return
        val removed = trackManager.removeAt(indexes)

        var msg = context.getTranslation("$root.removed")
            .withVariable("count", removed.size.toString())
        for ((index, track) in removed) {
            msg += "\n[#${index + 1}](${track.info.uri}) - ${track.info.title}"
        }
        val queueParts = StringUtils.splitMessage(msg)
        val eb = Embedder(context)
        for (queue in queueParts) {
            eb.setDescription(queue)
            sendEmbedRsp(context, eb.build())
        }
    }

    private suspend fun getIntegersFromArgsNMessage(context: CommandContext, start: Int, end: Int): IntArray? {
        val args = context.rawArg.remove(" ").split(",")
        val ints = mutableListOf<Int>()
        try {
            for (arg in args) {
                if (arg.contains("-")) {

                    val list: List<String> = arg.split("-")
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
                            .withVariable(PLACEHOLDER_ARG, arg)
                        sendRsp(context, msg)
                        return null
                    }
                } else if (arg.isNumber()) {
                    if (!ints.contains(arg.toInt() - 1)) {
                        ints.add(arg.toInt() - 1)
                    }
                } else {
                    val msg = context.getTranslation("message.unknown.numberornumberrange")
                        .withVariable(PLACEHOLDER_ARG, arg)
                    sendRsp(context, msg)
                    return null
                }
            }

        } catch (e: NumberFormatException) {
            val msg = context.getTranslation("message.numbertobig")
                .withVariable(PLACEHOLDER_ARG, e.message ?: "/")
            sendRsp(context, msg)
            return null
        }
        for (i in ints) {
            if (i < (start - 1) || i > (end - 1)) {
                val msg = context.getTranslation("message.number.notinrange")
                    .withVariable(PLACEHOLDER_ARG, i + 1)
                    .withVariable("start", start)
                    .withVariable("end", end)
                sendRsp(context, msg)
                return null
            }
        }
        return ints.toIntArray()
    }
}

