package me.melijn.melijnbot.commands.music

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.*

class GainProfileCommand : AbstractCommand("command.gainprofile") {

    init {
        id = 142
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
    }

    class LoadArg(parent: String) : AbstractCommand("$parent.load") {

        init {
            name = "load"
            aliases = arrayOf("l")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class CopyArg(parent: String) : AbstractCommand("$parent.copy") {

        init {
            name = "copy"
            aliases = arrayOf("c", "cp")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class InfoArg(parent: String) : AbstractCommand("$parent.info") {

        init {
            name = "info"
            aliases = arrayOf("i")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val profileName = if (context.args[0].isPositiveNumber()) {
                val index = getIntegerFromArgNMessage(context, 0, 0, map.size - 1) ?: return
                map[map.keys.sortedBy { it }[index]]
            } else {
                val name = getStringFromArgsNMessage(context, 0, 1, 20) ?: return
                if (map.keys.contains(name)) map[name] else {
                    val msg = context.getTranslation("$root.notfound")
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }
            } ?: throw IllegalArgumentException("no this is bad code smh")


        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm", "r", "del", "d", "delete")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }


            val wrapper = context.daoManager.gainProfileWrapper
            val map = wrapper.gainProfileCache.get(context.guildId).await()

            val profileName = if (context.args[0].isPositiveNumber()) {
                val index = getIntegerFromArgNMessage(context, 0, 0, map.size - 1) ?: return
                map.keys.sortedBy { it }[index]
            } else {
                val name = getStringFromArgsNMessage(context, 0, 1, 20) ?: return
                if (map.keys.contains(name)) name else {
                    val msg = context.getTranslation("$root.notfound")
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }
            }

            wrapper.remove(context.guildId, profileName)

            val msg = context.getTranslation("$root.removed")
                .replace(PLACEHOLDER_ARG, profileName)

            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "l")
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

            sendMsg(context, title + content)
        }
    }

    class SetArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "set"
            aliases = arrayOf("a", "save", "s")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 20) ?: return
            val wrapper = context.daoManager.gainProfileWrapper
            wrapper.add(context.guildId, name, context.guildMusicPlayer.guildTrackManager.iPlayer.bands)

            val msg = context.getTranslation("$root.added")
                .replace(PLACEHOLDER_ARG, name)

            sendMsg(context, msg)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}