package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.autopunishment.Punishment
import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_PERMISSIONTYPE
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.utils.data.DataObject

class PunishmentCommand : AbstractCommand("command.punishment") {

    init {
        id = 125
        name = "punishment"
        aliases = arrayOf("punish")
        children = arrayOf(
            AddArg(root),
            ListArg(root),
            RemoveArg(root),
            SetReason(root),
            SetExtra(root)
        )
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class SetExtra(parent: String) : AbstractCommand("$parent.setextra") {

        init {
            name = "setExtra"
            children = arrayOf(
                BanArg(root),
                MuteArg(root),
                SoftBanArg(root)
            )
        }

        class BanArg(parent: String) : AbstractCommand("$parent.ban") {

            init {
                name = "ban"
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context)
                    return
                }

                val name = context.args[0]
                val wrapper = context.daoManager.punishmentWrapper
                val list = wrapper.punishmentCache.get(context.guildId).await()

                val item = list.filter { (pName) -> pName == name }.getOrNull(0)
                if (item == null || item.punishmentType != PunishmentType.BAN) {
                    val msg = context.getTranslation("$root.nomatch")
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }

                when (context.args[1].toLowerCase()) {
                    "duration" -> {
                        val seconds = getLongFromArgNMessage(context, 3, 0) ?: return
                        item.extraMap = item.extraMap.put("duration", seconds)

                        wrapper.put(context.guildId, item)
                        val extra = if (seconds == 0L) ".infinite" else ""
                        val msg = context.getTranslation("$root.setduration$extra")
                            .replace("%name%", name)
                            .replace("%duration%", seconds.toString())
                        sendMsg(context, msg)
                    }
                    "delDays" -> {
                        val days = getLongFromArgNMessage(context, 0, 7) ?: return
                        item.extraMap = item.extraMap.put("delDeys", days)

                        wrapper.put(context.guildId, item)
                    }
                    else -> {
                        sendSyntax(context)
                    }
                }
            }
        }

        class MuteArg(parent: String) : AbstractCommand("$parent.ban") {

            init {
                name = "mute"
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context)
                    return
                }

                val name = context.args[0]
                val wrapper = context.daoManager.punishmentWrapper
                val list = wrapper.punishmentCache.get(context.guildId).await()

                val item = list.filter { (pName) -> pName == name }.getOrNull(0)
                if (item == null || item.punishmentType != PunishmentType.MUTE) {
                    val msg = context.getTranslation("$root.nomatch")
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }

                when (context.args[1].toLowerCase()) {
                    "duration" -> {
                        val seconds = getLongFromArgNMessage(context, 3, 0) ?: return
                        item.extraMap = item.extraMap.put("duration", seconds)

                        wrapper.put(context.guildId, item)
                        val extra = if (seconds == 0L) ".infinite" else ""
                        val msg = context.getTranslation("$root.setduration$extra")
                            .replace("%name%", name)
                            .replace("%duration%", seconds.toString())
                        sendMsg(context, msg)
                    }
                    else -> {
                        sendSyntax(context)
                    }
                }
            }
        }

        class SoftBanArg(parent: String) : AbstractCommand("$parent.ban") {

            init {
                name = "softBan"
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 3) {
                    sendSyntax(context)
                    return
                }

                val name = context.args[0]
                val wrapper = context.daoManager.punishmentWrapper
                val list = wrapper.punishmentCache.get(context.guildId).await()

                val item = list.filter { (pName) -> pName == name }.getOrNull(0)
                if (item == null || item.punishmentType != PunishmentType.SOFTBAN) {
                    val msg = context.getTranslation("$root.nomatch")
                        .replace(PLACEHOLDER_ARG, name)
                    sendMsg(context, msg)
                    return
                }

                when (context.args[1].toLowerCase()) {
                    "delDays" -> {
                        val days = getLongFromArgNMessage(context, 0, 7) ?: return
                        item.extraMap = item.extraMap.put("delDeys", days)

                        wrapper.put(context.guildId, item)
                    }
                    else -> {
                        sendSyntax(context)
                    }
                }
            }
        }

        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }

    class SetReason(parent: String) : AbstractCommand("$parent.setreason") {

        init {
            name = "setReason"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val name = context.args[0]
            val reason = context.rawArg.removeFirst(name).trim()
            val wrapper = context.daoManager.punishmentWrapper
            val list = wrapper.punishmentCache.get(context.guildId).await()

            val item = list.filter { (pName) -> pName == name }.getOrNull(0)
            if (item == null) {
                val msg = context.getTranslation("$root.nomatch")
                    .replace(PLACEHOLDER_ARG, name)
                sendMsg(context, msg)
                return
            }

            item.reason = reason
            wrapper.put(context.guildId, item)

            val msg = context.getTranslation("$root.set")
                .replace("%name%", name)
                .replace("%type%", item.punishmentType.toUCSC())
                .replace("%reason%", reason)
            sendMsg(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("put", "insert")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 3) {
                sendSyntax(context)
                return
            }
            val punishmentType = getEnumFromArgNMessage<PunishmentType>(context, 0, MESSAGE_UNKNOWN_PERMISSIONTYPE)
                ?: return
            val name = context.args[1]
            val reason = context.rawArg
                .removeFirst(context.args[0]).trim()
                .removeFirst(context.args[1]).trim()
            val punishment = Punishment(name, punishmentType, DataObject.empty(), reason)

            val wrapper = context.daoManager.punishmentWrapper
            wrapper.put(context.guildId, punishment)

            val msg = context.getTranslation("$root.added")
                .replace("%name%", name)
                .replace("%type%", punishmentType.toUCSC())
                .replace("%reason%", escapeForLog(reason))
            sendMsg(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.punishmentWrapper
            var list = wrapper.punishmentCache.get(context.guildId).await()
            list = if (context.args.isEmpty()) {
                list
            } else {
                val punishmentType = getEnumFromArgNMessage<PunishmentType>(context, 0, MESSAGE_UNKNOWN_PERMISSIONTYPE)
                    ?: return
                list.filter { punishment ->
                    punishment.punishmentType == punishmentType
                }
            }

            val msg = context.getTranslation("$root.title")
            var content = "```INI"
            for ((name, punishmentType) in list.sortedBy { perm -> perm.punishmentType }) {
                content += "[${name}] - ${punishmentType.toUCSC()}"
            }
            content += "```"
            sendMsg(context, msg + content)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = context.args[0]
            val wrapper = context.daoManager.punishmentWrapper
            val list = wrapper.punishmentCache.get(context.guildId).await()

            val item = list.filter { (pName) -> pName == name }.getOrNull(0)
            if (item == null) {
                val msg = context.getTranslation("$root.nomatch")
                    .replace(PLACEHOLDER_ARG, name)
                sendMsg(context, msg)
                return
            }

            wrapper.remove(context.guildId, name)
            val msg = context.getTranslation("$root.removed")
                .replace("%name%", name)
                .replace("%type%", item.punishmentType.toUCSC())
            sendMsg(context, msg)
        }
    }
}