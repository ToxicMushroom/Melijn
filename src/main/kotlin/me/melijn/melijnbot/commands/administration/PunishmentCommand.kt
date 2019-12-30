package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.autopunishment.Punishment
import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PREFIX_PLACE_HOLDER
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
                children = arrayOf(
                    DurationArg(root),
                    DelDaysArg(root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class DurationArg(parent: String) : AbstractCommand("$parent.duration") {

                init {
                    name = "duration"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }

                    val name = context.args[0]
                    val item = getPunishmentNMessage(context, 0, PunishmentType.BAN) ?: return
                    val wrapper = context.daoManager.punishmentWrapper

                    val seconds = getDurationByArgsNMessage(context, 1, context.args.size)
                        ?: return
                    item.extraMap = item.extraMap.put("duration", seconds)

                    wrapper.put(context.guildId, item)
                    val extra = if (seconds == 0L) ".infinite" else ""

                    val msg = context.getTranslation("$root.set$extra")
                        .replace("%name%", name)
                        .replace("%duration%", "$seconds")
                    sendMsg(context, msg)
                }
            }

            class DelDaysArg(parent: String) : AbstractCommand("$parent.deldays") {

                init {
                    name = "delDays"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }

                    val name = context.args[0]
                    val item = getPunishmentNMessage(context, 0, PunishmentType.BAN) ?: return
                    val wrapper = context.daoManager.punishmentWrapper

                    val days = getLongFromArgNMessage(context, 0, 7) ?: return
                    item.extraMap = item.extraMap.put("delDeys", days)

                    wrapper.put(context.guildId, item)

                    val msg = context.getTranslation("$root.deldays")
                        .replace("%name%", name)
                        .replace("%deldays%", "$days")
                    sendMsg(context, msg)
                }
            }
        }

        class MuteArg(parent: String) : AbstractCommand("$parent.ban") {

            init {
                name = "mute"
                children = arrayOf(
                    DurationArg(parent)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class DurationArg(parent: String) : AbstractCommand("$parent.duration") {

                init {
                    name = "duration"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }

                    val name = context.args[0]
                    val item = getPunishmentNMessage(context, 0, PunishmentType.MUTE) ?: return
                    val wrapper = context.daoManager.punishmentWrapper

                    val seconds = getDurationByArgsNMessage(context, 1, context.args.size)
                        ?: return
                    item.extraMap = item.extraMap.put("duration", seconds)

                    wrapper.put(context.guildId, item)
                    val extra = if (seconds == 0L) ".infinite" else ""

                    val msg = context.getTranslation("$root.set$extra")
                        .replace("%name%", name)
                        .replace("%duration%", "$seconds")
                    sendMsg(context, msg)
                }
            }
        }

        class SoftBanArg(parent: String) : AbstractCommand("$parent.ban") {

            init {
                name = "softBan"
                children = arrayOf(
                    DelDaysArg(root)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }

            class DelDaysArg(parent: String) : AbstractCommand("$parent.deldays") {

                init {
                    name = "delDays"
                }

                override suspend fun execute(context: CommandContext) {
                    if (context.args.size < 2) {
                        sendSyntax(context)
                        return
                    }

                    val item = getPunishmentNMessage(context, 0, PunishmentType.SOFTBAN) ?: return
                    val wrapper = context.daoManager.punishmentWrapper
                    val days = getLongFromArgNMessage(context, 0, 7) ?: return
                    item.extraMap = item.extraMap.put("delDays", days)

                    wrapper.put(context.guildId, item)


                    val msg = context.getTranslation("$root.deldays")
                        .replace("%name%", name)
                        .replace("%deldays%", "$days")
                    sendMsg(context, msg)
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
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val item = getPunishmentNMessage(context, 0) ?: return
            if (context.args.size == 1) {
                val reason = item.reason
                val msg = context.getTranslation("$root.show")
                    .replace("%name%", item.name)
                    .replace("%type%", item.punishmentType.toUCC())
                    .replace("%reason%", reason)
                sendMsg(context, msg)
                return
            }

            val wrapper = context.daoManager.punishmentWrapper
            val reason = context.rawArg.removeFirst(name).trim()

            item.reason = reason
            wrapper.put(context.guildId, item)

            val msg = context.getTranslation("$root.set")
                .replace("%name%", item.name)
                .replace("%type%", item.punishmentType.toUCC())
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
                .replace("%type%", punishmentType.toUCC())
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
            val msg: String
            list = if (context.args.isEmpty()) {
                msg = context.getTranslation("$root.title")
                list
            } else {
                val punishmentType = getEnumFromArgNMessage<PunishmentType>(context, 0, MESSAGE_UNKNOWN_PERMISSIONTYPE)
                    ?: return
                msg = context.getTranslation("$root.typedtitle")
                    .replace("%type%", punishmentType.toUCC())
                list.filter { punishment ->
                    punishment.punishmentType == punishmentType
                }
            }


            var content = "```INI\n[name] - [type]\n  {[extra data]}\n"
            for ((name, punishmentType, d) in list.sortedBy { perm -> perm.punishmentType }) {
                content += "\n[${name}] - ${punishmentType.toUCC()}"
                content += "\n  " + d.toString().replace("\n", "\n  ")
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

            val item = getPunishmentNMessage(context, 0) ?: return
            val wrapper = context.daoManager.punishmentWrapper

            wrapper.remove(context.guildId, name)
            val msg = context.getTranslation("$root.removed")
                .replace("%name%", item.name)
                .replace("%type%", item.punishmentType.toUCC())
            sendMsg(context, msg)
        }
    }
}

suspend fun getPunishmentNMessage(context: CommandContext, position: Int, punishmentType: PunishmentType? = null): Punishment? {
    val name = context.args[position]
    val wrapper = context.daoManager.punishmentWrapper
    val list = wrapper.punishmentCache.get(context.guildId).await()

    val item = list.filter { (pName) ->
        pName == name
    }.getOrNull(0)

    if (item == null || (punishmentType != null && item.punishmentType != punishmentType)) {
        val extra = if (punishmentType == null) "" else ".typed"
        val msg = context.getTranslation("${context.commandOrder.first().root}.nomatch$extra")
            .replace(PLACEHOLDER_ARG, name)
            .replace(PREFIX_PLACE_HOLDER, context.usedPrefix)
            .replace("%type%", item?.punishmentType?.toUCC() ?: "error")
        sendMsg(context, msg)
    }
    return item
}