package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.autopunishment.Punishment
import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_PERMISSIONTYPE
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.escapeForLog
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.utils.data.DataObject

class PunishmentCommand : AbstractCommand("command.punishment") {

    init {
        id = 125
        name = "punishment"
        children = arrayOf(
            AddArg(root),
            ListArg(root),
            RemoveArg(root),
            SetReason(root),
            SetExtra(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

    class SetExtra(parent: String) : AbstractCommand("$parent.setextra") {

        init {
            name = "setExtra"
            aliases = arrayOf("se")
            children = arrayOf(
                BanArg(root),
                MuteArg(root),
                SoftBanArg(root)
//                ,
//                AddRoleArg(root),
//                RemoveRoleArg(root)
            )
        }

        class DurationArg(parent: String, private val pType: PunishmentType) : AbstractCommand("$parent.duration") {

            init {
                name = "duration"
                aliases = arrayOf("d")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 2) {
                    sendSyntax(context)
                    return
                }

                val name = context.args[0]
                val item = getPunishmentNMessage(context, 0, pType) ?: return
                val wrapper = context.daoManager.punishmentWrapper

                val seconds = getDurationByArgsNMessage(context, 1, context.args.size)
                    ?: return
                item.extraMap = item.extraMap.put("duration", seconds)

                wrapper.put(context.guildId, item)
                val extra = if (seconds == 0L) ".infinite" else ""

                val msg = context.getTranslation("$root.set$extra")
                    .withVariable("name", name)
                    .withVariable("duration", "$seconds")
                sendRsp(context, msg)
            }
        }

        class RoleArg(parent: String, private val pType: PunishmentType) : AbstractCommand("$parent.role") {

            init {
                name = "role"
                aliases = arrayOf("r")
            }

            override suspend fun execute(context: CommandContext) {
                if (context.args.size < 2) {
                    sendSyntax(context)
                    return
                }

                val name = context.args[0]

                val item = getPunishmentNMessage(context, 0, pType) ?: return
                val role = getRoleByArgsNMessage(context, 1, canInteract = true) ?: return
                val wrapper = context.daoManager.punishmentWrapper

                item.extraMap = item.extraMap.put("role", role.idLong)

                wrapper.put(context.guildId, item)

                val msg = context.getTranslation("$root.set")
                    .withVariable("name", name)
                    .withVariable("role", role.name)
                sendRsp(context, msg)
            }
        }

        class AddRoleArg(parent: String) : AbstractCommand("$parent.addrole") {

            init {
                name = "addRole"
                aliases = arrayOf("ar")
                children = arrayOf(
                    DurationArg(root, PunishmentType.ADDROLE),
                    RoleArg(root, PunishmentType.ADDROLE)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }
        }

        class RemoveRoleArg(parent: String) : AbstractCommand("$parent.removerole") {

            init {
                name = "removeRole"
                aliases = arrayOf("rr")
                DurationArg(root, PunishmentType.REMOVEROLE)
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }
        }

        class BanArg(parent: String) : AbstractCommand("$parent.ban") {

            init {
                name = "ban"
                children = arrayOf(
                    DurationArg(root, PunishmentType.BAN),
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

                    val name = context.args[0]
                    val item = getPunishmentNMessage(context, 0, PunishmentType.BAN) ?: return
                    val wrapper = context.daoManager.punishmentWrapper

                    val days = getLongFromArgNMessage(context, 0, 7) ?: return
                    item.extraMap = item.extraMap.put("delDeys", days)

                    wrapper.put(context.guildId, item)

                    val msg = context.getTranslation("$root.deldays")
                        .withVariable("name", name)
                        .withVariable("deldays", "$days")
                    sendRsp(context, msg)
                }
            }
        }

        class MuteArg(parent: String) : AbstractCommand("$parent.mute") {

            init {
                name = "mute"
                children = arrayOf(
                    DurationArg(root, PunishmentType.MUTE)
                )
            }

            override suspend fun execute(context: CommandContext) {
                sendSyntax(context)
            }
        }

        class SoftBanArg(parent: String) : AbstractCommand("$parent.softban") {

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
                    val days = getLongFromArgNMessage(context, 1, 7) ?: return
                    item.extraMap = item.extraMap.put("delDays", days)

                    wrapper.put(context.guildId, item)


                    val msg = context.getTranslation("$root.deldays")
                        .withVariable("name", item.name)
                        .withVariable("deldays", "$days")
                    sendRsp(context, msg)
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
                    .withVariable("name", item.name)
                    .withVariable("type", item.punishmentType.toUCC())
                    .withVariable("reason", reason)
                sendRsp(context, msg)
                return
            }

            val wrapper = context.daoManager.punishmentWrapper
            val reason = context.fullArg.removeFirst(item.name).trim()

            item.reason = reason
            wrapper.put(context.guildId, item)

            val msg = context.getTranslation("$root.set")
                .withVariable("name", item.name)
                .withVariable("type", item.punishmentType.toUCC())
                .withVariable("reason", reason)
            sendRsp(context, msg)
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
            val reason = context.fullArg
                .removeFirst(context.args[0]).trim()
                .removeFirst(context.args[1]).trim()
            val punishment = Punishment(name, punishmentType, DataObject.empty(), reason)

            val wrapper = context.daoManager.punishmentWrapper
            wrapper.put(context.guildId, punishment)

            val msg = context.getTranslation("$root.added")
                .withVariable("name", name)
                .withVariable("type", punishmentType.toUCC())
                .withVariable("reason", escapeForLog(reason))
            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.punishmentWrapper
            var list = wrapper.getList(context.guildId)
            val msg: String
            list = if (context.args.isEmpty()) {
                msg = context.getTranslation("$root.title")
                list
            } else {
                val punishmentType = getEnumFromArgNMessage<PunishmentType>(context, 0, MESSAGE_UNKNOWN_PERMISSIONTYPE)
                    ?: return
                msg = context.getTranslation("$root.typedtitle")
                    .withVariable("type", punishmentType.toUCC())
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
            sendRsp(context, msg + content)
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
                .withVariable("name", item.name)
                .withVariable("type", item.punishmentType.toUCC())
            sendRsp(context, msg)
        }
    }
}

suspend fun getPunishmentNMessage(context: CommandContext, position: Int, punishmentType: PunishmentType? = null): Punishment? {
    val name = context.args[position]
    val wrapper = context.daoManager.punishmentWrapper
    val list = wrapper.getList(context.guildId)

    val item = list.filter { (pName) ->
        pName == name
    }.getOrNull(0)

    if (item == null || (punishmentType != null && item.punishmentType != punishmentType)) {
        val extra = if (punishmentType == null) "" else ".typed"
        val msg = context.getTranslation("command.punishment.nomatch$extra")
            .withVariable(PLACEHOLDER_ARG, name)
            .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            .withVariable("type", item?.punishmentType?.toUCC() ?: "error")
        sendRsp(context, msg)
    }
    return item
}