package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_CHANNEL
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class ChannelRoleCommand : AbstractCommand("command.channelrole") {

    init {
        name = "channelRole"
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            ClearArg(root),
            ListArg(root)
        )
        aliases = arrayOf("cr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val wrapper = context.daoManager.channelRoleWrapper
            if (context.args.isEmpty()) {
                val channelRoles = wrapper.getChannelRoles(context.guildId)
                if (channelRoles.isEmpty()) {
                    val msg = context.getTranslation("$root.guild.nochannelroles")
                    sendRsp(context, msg)
                    return
                }


                val sb = StringBuilder(
                    "```ini\n" +
                        "[channelId] - [channelName]:\n" +
                        "  [roleList (index. roleId - roleName)]\n"
                )
                for ((channelId, roleIds) in channelRoles.toSortedMap()) {
                    val channel = context.guild.getVoiceChannelById(channelId)
                    sb.append("\n$channelId - [${channel?.name ?: "deleted channel"}]:\n")
                    for ((index, roleId) in roleIds.sorted().withIndex()) {
                        val role = context.guild.getRoleById(roleId)
                        sb.append("  ").append(index + 1).append(". [").append(roleId)
                            .append("] - ").appendLine(role?.name ?: "deleted role")

                    }
                }
                sb.append("```")
                sendRspCodeBlock(context, sb.toString(), "INI")

            } else {
                val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
                val roleIds = wrapper.getRoleIds(context.guildId, channel.idLong)

                if (roleIds.isEmpty()) {
                    val msg = context.getTranslation("$root.vc.nochannelroles")
                    sendRsp(context, msg.withSafeVariable(PLACEHOLDER_CHANNEL, channel.name))
                    return
                }

                val sb = StringBuilder(
                    "```ini\n" +
                        "[index]. [roleId] - [roleName]\n"
                )

                for ((index, roleId) in roleIds.sorted().withIndex()) {
                    val role = context.guild.getRoleById(roleId)
                    sb.append(index + 1).append(". [").append(roleId)
                        .append("] - ").appendLine(role?.name ?: "deleted role")

                }

                sb.append("```")
                sendRspCodeBlock(context, sb.toString(), "INI")
            }
        }
    }

    class ClearArg(parent: String) : AbstractCommand("$parent.clear") {

        init {
            name = "clear"
            aliases = arrayOf("c")
        }

        override suspend fun execute(context: ICommandContext) {
            if (argSizeCheckFailed(context, 1)) return
            val channelId = if (DISCORD_ID.matches(context.args[0])) {
                context.args[0].toLong()
            } else {
                getVoiceChannelByArgNMessage(context, 0)?.idLong ?: return
            }

            val vc = context.guild.getVoiceChannelById(channelId)

            val wrapper = context.daoManager.channelRoleWrapper
            wrapper.clear(context.guildId, channelId)

            val msg = context.getTranslation("$root.cleared")
                .withSafeVariable(PLACEHOLDER_CHANNEL, vc?.name ?: "deleted channel (${channelId})")
            sendRsp(context, msg)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        override suspend fun execute(context: ICommandContext) {
            val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
            val wrapper = context.daoManager.channelRoleWrapper
            val roles = wrapper.getRoleIds(context.guildId, channel.idLong)

            val index = getIntegerFromArgNMessage(context, 1, 1, roles.size) ?: return
            val roleId = roles[index]

            wrapper.remove(context.guildId, channel.idLong, roleId)
            val role = context.guild.getRoleById(roleId)

            val msg = context.getTranslation("$root.removed")
                .withSafeVariable(PLACEHOLDER_CHANNEL, channel.name)
                .withSafeVariable(PLACEHOLDER_ROLE, role?.name ?: "deleted role (${roleId})")
            sendRsp(context, msg)
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm")
        }

        override suspend fun execute(context: ICommandContext) {
            val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1) ?: return

            context.daoManager.channelRoleWrapper.remove(context.guildId, channel.idLong, role.idLong)

            val msg = context.getTranslation("$root.removed")
                .withSafeVariable(PLACEHOLDER_CHANNEL, channel.name)
                .withSafeVariable(PLACEHOLDER_ROLE, role.name)
            sendRsp(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: ICommandContext) {
            val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1, canInteract = true) ?: return

            context.daoManager.channelRoleWrapper.add(context.guildId, channel.idLong, role.idLong)

            val msg = context.getTranslation("$root.added")
                .withSafeVariable(PLACEHOLDER_CHANNEL, channel.name)
                .withSafeVariable(PLACEHOLDER_ROLE, role.name)
            sendRsp(context, msg)
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}