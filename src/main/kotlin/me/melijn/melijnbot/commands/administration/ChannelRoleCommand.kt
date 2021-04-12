package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.enums.ChannelRoleState
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
            SetArg(root),
            RemoveArg(root),
            RemoveAtArg(root),
            ClearArg(root),
            ListArg(root)
        )
        aliases = arrayOf("cr")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class SetArg(parent: String) : AbstractCommand("$parent.set") {

        init {
            name = "set"
            aliases = arrayOf("s")
        }

        override suspend fun execute(context: ICommandContext) {
            val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
            val role = getRoleByArgsNMessage(context, 1, canInteract = true) ?: return
            val path = "message.unknown.channelrolestate"
            val state = getEnumFromArgNMessage<ChannelRoleState>(context, 2, path) ?: return

            context.daoManager.channelRoleWrapper.set(context.guildId, channel.idLong, role.idLong, state)

            val msg = context.getTranslation("$root.set")
                .withSafeVariable(PLACEHOLDER_CHANNEL, channel.name)
                .withSafeVariable(PLACEHOLDER_ROLE, role.name)
                .withSafeVarInCodeblock("state", state.toString())
            sendRsp(context, msg)
        }
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
                        "  [roleList (index. roleId - roleName - state)]\n"
                )
                for ((channelId, map) in channelRoles.toSortedMap()) {
                    for ((state, roles) in map) {
                        val channel = context.guild.getVoiceChannelById(channelId)
                        sb.append("\n$channelId - [${channel?.name ?: "deleted channel"}]:\n")
                        addChannelRoles(context, sb, state, roles)
                    }
                }
                sb.append("```")
                sendRspCodeBlock(context, sb.toString(), "INI")

            } else {
                val channel = getVoiceChannelByArgNMessage(context, 0) ?: return
                val map = wrapper.getRoleIds(context.guildId, channel.idLong)
                if (map.isEmpty()) {
                    val msg = context.getTranslation("$root.vc.nochannelroles")
                    sendRsp(context, msg.withSafeVariable(PLACEHOLDER_CHANNEL, channel.name))
                    return
                }

                val sb = StringBuilder(
                    "```ini\n" +
                        "[index]. [roleId] - [roleName] - [state]\n"
                )

                for ((state, roles) in map) {
                    addChannelRoles(context, sb, state, roles)
                }

                sb.append("```")
                sendRspCodeBlock(context, sb.toString(), "INI")
            }
        }

        private fun addChannelRoles(
            context: ICommandContext,
            sb: StringBuilder,
            state: ChannelRoleState,
            roles: List<Long>
        ) {
            for ((index, roleId) in roles.withIndex()) {
                val role = context.guild.getRoleById(roleId)
                sb.append("  ").append(index + 1).append(". [").append(roleId)
                    .append("] - ").append(role?.name ?: "deleted role")
                    .appendLine(" - [$state]")
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
            val roleIds = wrapper.getRoleIds(context.guildId, channel.idLong)
            val map = roleIds.toSortedMap()
            val roles = map.flatMap { it.value }
            val index = (getIntegerFromArgNMessage(context, 1, 1, roles.size) ?: return) - 1
            val roleId = roles[index]

            wrapper.remove(context.guildId, channel.idLong, roleId)

            var count = 0
            val (state, _) = roleIds.first {
                count += it.value.size
                index < count
            }

            val role = context.guild.getRoleById(roleId)
            val msg = context.getTranslation("$root.removed")
                .withSafeVariable(PLACEHOLDER_CHANNEL, channel.name)
                .withSafeVariable(PLACEHOLDER_ROLE, role?.name ?: "deleted role (${roleId})")
                .withSafeVariable("state", state)
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

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}