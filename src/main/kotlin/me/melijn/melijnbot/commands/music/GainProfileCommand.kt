package me.melijn.melijnbot.commands.music

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.audio.GainProfile
import me.melijn.melijnbot.internals.command.*
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.math.roundToInt

class GainProfileCommand : AbstractCommand("command.gainprofile") {

    init {
        id = 141
        name = "gainProfile"
        aliases = arrayOf("gp")
        children = arrayOf(
            SetArg(root),
            RemoveArg(root),
            LoadArg(root),
            ListArg(root),
            InfoArg(root),
            CopyArg(root)
        )
        commandCategory = CommandCategory.MUSIC
    }

    class LoadArg(parent: String) : AbstractCommand("$parent.load") {

        init {
            name = "load"
            aliases = arrayOf("l")
            runConditions = arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val pair: Pair<String, GainProfile> = getGainProfileNMessage(context, map, 0) ?: return
            val profile = pair.second
            val floatArray = profile.toFloatArray()

            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            player.setBands(floatArray)

            val msg = context.getTranslation("$root.loaded")
                .withVariable("gainProfile", pair.first)
            sendRsp(context, msg)
        }
    }

    class CopyArg(parent: String) : AbstractCommand("$parent.copy") {

        init {
            name = "copy"
            aliases = arrayOf("c", "cp")
            permissionRequired = true
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val pair = getGainProfileNMessage(context, map, 0) ?: return
            val name = pair.first
            val profile = pair.second

            val newName = getStringFromArgsNMessage(context, 0, 1, 20) ?: return

            wrapper.add(context.guildId, newName, profile.toFloatArray())

            val msg = context.getTranslation("$context.copied")
                .withVariable("gainProfile1", name)
                .withVariable("gainProfile2", newName)
            sendRsp(context, msg)
        }
    }

    class InfoArg(parent: String) : AbstractCommand("$parent.info") {

        init {
            name = "info"
            aliases = arrayOf("i")
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val pair = getGainProfileNMessage(context, map, 0) ?: return
            val name: String = pair.first
            val profile = pair.second

            val msg = "```INI\n[$name]\n[bandId] - [spectrum] - [gain]\n" +
                "0 - [25 Hz] - ${((profile.band0 + 0.25f) * 500).roundToInt()}" +
                "1 - [40 Hz] - ${((profile.band1 + 0.25f) * 500).roundToInt()}" +
                "2 - [63 Hz] - ${((profile.band2 + 0.25f) * 500).roundToInt()}" +
                "3 - [100 Hz] - ${((profile.band3 + 0.25f) * 500).roundToInt()}" +
                "4 - [160 Hz] - ${((profile.band4 + 0.25f) * 500).roundToInt()}" +
                "5 - [250 Hz] - ${((profile.band5 + 0.25f) * 500).roundToInt()}" +
                "6 - [400 Hz] - ${((profile.band6 + 0.25f) * 500).roundToInt()}" +
                "7 - [630 Hz] - ${((profile.band7 + 0.25f) * 500).roundToInt()}" +
                "8 - [1 kHz] - ${((profile.band8 + 0.25f) * 500).roundToInt()}" +
                "9 - [1.6 kHz] - ${((profile.band9 + 0.25f) * 500).roundToInt()}" +
                "10 - [2.5 kHz] - ${((profile.band10 + 0.25f) * 500).roundToInt()}" +
                "11 - [4 kHz] - ${((profile.band11 + 0.25f) * 500).roundToInt()}" +
                "12 - [6.3 kHz] - ${((profile.band12 + 0.25f) * 500).roundToInt()}" +
                "13 - [10 kHz] - ${((profile.band13 + 0.25f) * 500).roundToInt()}" +
                "14 - [16 kHz] - ${((profile.band14 + 0.25f) * 500).roundToInt()}" +
                "```"

            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r", "del", "d", "delete")
            permissionRequired = true
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }


            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val profileName = getGainProfileNMessage(context, map, 0)?.first ?: return

            wrapper.remove(context.guildId, profileName)

            val msg = context.getTranslation("$root.removed")
                .withVariable(PLACEHOLDER_ARG, profileName)

            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "l")
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val title = context.getTranslation("$root.title")
            var content = "```INI\n" +
                "[name] - [index]:\n" +
                "  [band0] - [band1] - [band2] - [band3] - [band4]\n" +
                "  [band5] - [band6] - [band7] - [band8] - [band9]\n" +
                "  [band10] - [band11] - [band12] - [band13] - [band14]\n\n"

            for ((i, name) in map.keys.sortedBy { it }.withIndex()) {
                val profile = map[name] ?: continue
                content += "[$name] - [$i]:\n" +
                    "  ${profile.band0} - ${profile.band1} - ${profile.band2} - ${profile.band3} - ${profile.band4}" +
                    "  ${profile.band5} - ${profile.band6} - ${profile.band7} - ${profile.band8} - ${profile.band9}" +
                    "  ${profile.band10} - ${profile.band11} - ${profile.band12} - ${profile.band13} - ${profile.band14}"

            }
            content += "```"

            sendRsp(context, title + content)
        }
    }

    class SetArg(parent: String) : AbstractCommand("$parent.set") {

        init {
            name = "set"
            aliases = arrayOf("a", "save", "s")
            permissionRequired = true
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 20) ?: return
            val wrapper = context.daoManager.gainProfileWrapper
            context.getGuildMusicPlayer().guildTrackManager.iPlayer.bands.let { wrapper.add(context.guildId, name, it) }

            val msg = context.getTranslation("$root.added")
                .withVariable(PLACEHOLDER_ARG, name)

            sendRsp(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}

suspend fun getGainProfileNMessage(context: CommandContext, map: Map<String, GainProfile>, index: Int): Pair<String, GainProfile>? {
    val name: String
    val profileName = if (context.args[index].isPositiveNumber()) {
        val profileIndex = getIntegerFromArgNMessage(context, index, 0, map.size - 1) ?: return null
        name = map.keys.sortedBy { it }[profileIndex]
        map[name]
    } else {
        name = getStringFromArgsNMessage(context, index, 1, 20) ?: return null
        if (map.keys.contains(name)) {
            map[name]
        } else {
            val msg = context.getTranslation("${context.commandOrder.first().root}.notfound")
                .withVariable(PLACEHOLDER_ARG, name)
                .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
            return null
        }
    } ?: throw IllegalArgumentException("no this is bad code smh")

    return Pair(name, profileName)
}