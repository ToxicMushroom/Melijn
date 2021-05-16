package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.database.audio.GainProfile
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.isPremiumUser
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.utils.withVariable
import kotlin.math.roundToInt

abstract class AbstractGainProfileCommand(root: String, val idParser: (ICommandContext) -> Long) :
    AbstractCommand(root) {

    init {
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

    inner class LoadArg(parent: String) : AbstractCommand("$parent.load") {

        init {
            name = "load"
            aliases = arrayOf("l")
            runConditions =
                arrayOf(RunCondition.VC_BOT_ALONE_OR_USER_DJ, RunCondition.PLAYING_TRACK_NOT_NULL, RunCondition.VOTED)
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.getGainProfile(idParser(context))

            val pair: Pair<String, GainProfile> = getGainProfileNMessage(context, map, 0) ?: return
            val profile = pair.second
            val floatArray = profile.toFloatArray()

            val player = context.getGuildMusicPlayer().guildTrackManager.iPlayer
            player.filters.bands = floatArray
            player.filters.commit()

            val msg = context.getTranslation("$root.loaded")
                .withSafeVariable("gainProfile", pair.first)
            sendRsp(context, msg)
        }
    }

    inner class CopyArg(parent: String) : AbstractCommand("$parent.copy") {

        init {
            name = "copy"
            aliases = arrayOf("c", "cp")
            runConditions = arrayOf(RunCondition.VOTED, RunCondition.EXPLICIT_MELIJN_PERMISSION)
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.getGainProfile(idParser(context))

            val pair = getGainProfileNMessage(context, map, 0) ?: return
            val name = pair.first
            val profile = pair.second

            val newName = getStringFromArgsNMessage(context, 0, 1, 20) ?: return

            wrapper.add(idParser(context), newName, profile.toFloatArray())

            val msg = context.getTranslation("$root.copied")
                .withSafeVariable("gainProfile1", name)
                .withSafeVariable("gainProfile2", newName)
            sendRsp(context, msg)
        }
    }

    inner class InfoArg(parent: String) : AbstractCommand("$parent.info") {

        init {
            name = "info"
            aliases = arrayOf("i")
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.getGainProfile(idParser(context))

            val pair = getGainProfileNMessage(context, map, 0) ?: return
            val name: String = pair.first
            val profile = pair.second

            val msg = "```INI\n[%name%]\n[bandId] - [spectrum] - [gain]\n".withSafeVariable("name", name) +
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

    inner class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r", "del", "d", "delete")
            runConditions = arrayOf(RunCondition.VOTED, RunCondition.EXPLICIT_MELIJN_PERMISSION)
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }


            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.getGainProfile(idParser(context))

            val profileName = getGainProfileNMessage(context, map, 0)?.first ?: return

            wrapper.remove(idParser(context), profileName)

            val msg = context.getTranslation("$root.removed")
                .withSafeVariable(PLACEHOLDER_ARG, profileName)

            sendRsp(context, msg)
        }
    }

    inner class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "l")
            runConditions = arrayOf(RunCondition.VOTED)
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.getGainProfile(idParser(context))

            val title = context.getTranslation("$root.title")
            var content = "```INI\n" +
                "[name] - [index]:\n" +
                "  [band0] - [band1] - [band2] - [band3] - [band4]\n" +
                "  [band5] - [band6] - [band7] - [band8] - [band9]\n" +
                "  [band10] - [band11] - [band12] - [band13] - [band14]\n\n"

            for ((i, name) in map.keys.sortedBy { it }.withIndex()) {
                val profile = map[name] ?: continue
                content += "[%name%] - [$i]:\n".withSafeVariable("name", name) +
                    "  ${profile.band0} - ${profile.band1} - ${profile.band2} - ${profile.band3} - ${profile.band4}" +
                    "  ${profile.band5} - ${profile.band6} - ${profile.band7} - ${profile.band8} - ${profile.band9}" +
                    "  ${profile.band10} - ${profile.band11} - ${profile.band12} - ${profile.band13} - ${profile.band14}"

            }
            content += "```"

            sendRsp(context, title + content)
        }
    }

    inner class SetArg(parent: String) : AbstractCommand("$parent.set") {

        init {
            name = "set"
            aliases = arrayOf("a", "save", "s")
            runConditions = arrayOf(RunCondition.VOTED, RunCondition.EXPLICIT_MELIJN_PERMISSION)
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 20) ?: return
            val wrapper = context.daoManager.gainProfileWrapper
            val profiles = wrapper.getProfileCount(idParser(context))
            val bands = context.getGuildMusicPlayer().guildTrackManager.iPlayer.filters.bands

            if (reachedPremiumLimitCount(
                    context, profiles,
                    PRIVATE_GAIN_PROFILES_LIMIT,
                    PREMIUM_PRIVATE_GAIN_PROFILES_LIMIT,
                    PRIVATE_GAIN_PROFILES_LIMIT_PATH,
                    PRIVATE_GAIN_PROFILES_PREMIUM_LIMIT_PATH
                )
            ) return

            wrapper.add(idParser(context), name, bands)


            val msg = context.getTranslation("$root.added")
                .withSafeVariable(PLACEHOLDER_ARG, name)

            sendRsp(context, msg)
        }

        private suspend fun reachedPremiumLimitCount(
            context: ICommandContext,
            count: Int,
            normalLimit: Int,
            premiumLimit: Int,
            normalLimitPath: String,
            premiumLimitPath: String
        ): Boolean {
            if (count >= normalLimit && !isPremiumUser(context)) {
                val replaceMap = mapOf(
                    "limit" to "$normalLimit",
                    "premiumLimit" to "$premiumLimit"
                )

                sendFeatureRequiresPremiumMessage(context, normalLimitPath, replaceMap)
                return true
            } else if (count >= premiumLimit) {
                val msg = context.getTranslation(premiumLimitPath)
                    .withVariable("limit", premiumLimit)
                sendRsp(context, msg)
                return true
            }
            return false
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}