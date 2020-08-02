package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.database.audio.GainProfile
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.getBooleanFromArgNMessage
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable


class BassBoostCommand : AbstractCommand("command.bassboost") {

    init {
        id = 195
        name = "bassBoost"
        aliases = arrayOf("bb")
        children = arrayOf(SelectArg(root))
        runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        commandCategory = CommandCategory.MUSIC
    }


    override suspend fun execute(context: CommandContext) {
        val state = getBooleanFromArgNMessage(context, 0) ?: return

        if (state) {
            val id = customBass[context.guildId] ?: 0
            val gp = bassProfiles[id]

            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            player.setBands(gp.toFloatArray())

            val mode = context.getTranslation("$root." + when (id) {
                0 -> "normal"
                1 -> "light"
                2 -> "harder"
                3 -> "hard"
                else -> return
            })

            val msg = context.getTranslation("$root.enabled")
                .withVariable("mode", mode)
            sendRsp(context, msg)

        } else {

            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            player.setBands(FloatArray(15) { 0.0f })

            val msg = context.getTranslation("$root.disabled")
            sendRsp(context, msg)
        }
    }

    class SelectArg(val parent: String) : AbstractCommand("$parent.select") {

        init {
            name = "select"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                val id = customBass[context.guildId] ?: 0
                val mode = context.getTranslation("$parent." + when (id) {
                    0 -> "normal"
                    1 -> "light"
                    2 -> "harder"
                    3 -> "hard"
                    else -> return
                })

                val msg = context.getTranslation("$root.show.selected")
                    .withVariable("mode", mode)
                sendRsp(context, msg)

            } else {
                val selection = getStringFromArgsNMessage(context, 0, 1, 10) ?: return
                val mode = context.getTranslation("$parent." + when (selection.toLowerCase()) {
                    "normal" -> {
                        customBass.remove(context.guildId)
                        "normal"
                    }
                    "light" -> {
                        customBass[context.guildId] = 1
                        "light"
                    }
                    "harder" -> {
                        customBass[context.guildId] = 2
                        "harder"
                    }
                    "hard" -> {
                        customBass[context.guildId] = 3
                        "hard"
                    }
                    else -> {
                        sendSyntax(context)
                        return
                    }
                })

                val msg = context.getTranslation("$root.set.selected")
                    .withVariable("mode", mode)
                sendRsp(context, msg)
            }
        }
    }

    companion object {
        val customBass = mutableMapOf<Long, Int>()
        val bassProfiles = listOf(
            GainProfile(0.25f, 0.15f), // 0 - normal
            GainProfile(0.20f, 0.10f), // 1 - light
            GainProfile(0.35f, 0.20f), // 2 - harder
            GainProfile(0.50f, 0.25f)  // 3 - hard
        )
    }


}
