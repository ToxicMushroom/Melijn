package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.role.SelfRoleMode
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*

const val UNKNOWN_SELFROLEMODE_PATH = "message.unknown.selfrolemode"

class SelfRoleCommand : AbstractCommand("command.selfrole") {

    init {
        id = 37
        name = "selfRole"
        aliases = arrayOf("sr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root),
            GroupArg(root),
            SetMode(root),  //Manual, Auto | Auto will ignore selfRoleMessageIds and selfRoleChannelIds and use the internal cached ones
            SendGroup(root) // Internal cached ones are messageIds created by the >sr sendGroup <channel> command
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class SendGroup(parent: String) : AbstractCommand("$parent.sendgroup") {

        init {
            name = "sendGroup"
            aliases = arrayOf("sg")
        }

        override suspend fun execute(context: CommandContext) {

        }
    }

    class SetMode(parent: String) : AbstractCommand("$parent.setmode") {

        init {
            name = "setMode"
            aliases = arrayOf("sm")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.selfRoleModeWrapper
            if (context.args.isEmpty()) {
                val mode = wrapper.selfRoleModeCache.get(context.guildId).await().toUCC()
                val msg = context.getTranslation("$root.show")
                    .replace("%mode%", mode)
                sendMsg(context, msg)
                return
            }

            val mode = getEnumFromArgNMessage<SelfRoleMode>(context, 0, UNKNOWN_SELFROLEMODE_PATH) ?: return
            wrapper.set(context.guildId, mode)
            val msg = context.getTranslation("$root.set")
                .replace("%mode%", mode.toUCC())
            sendMsg(context, msg)
        }
    }

    class GroupArg(parent: String) : AbstractCommand("$parent.group") {

        init {
            name = "group"
            children = arrayOf(
                //Groups will have ids, the arg for add will be the displayname
                AddArg(root),     //Adds a group
                RemoveArg(root),  //Removes a group
                ListArg(root),    //Lists all roles in group
                AddIdManually(root), //Message Ids
                RemoveIdManually(root),
                SetIsSelfRoleAble(root),
                SetIsEnabled(root),
                ChangeNameArg(root)
            )
        }

        class AddArg(parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: CommandContext) {
                val name = getStringFromArgsNMessage(context, 0, 1, 64) ?: return

            }
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("r", "d", "rm", "delete")
            }

            override suspend fun execute(context: CommandContext) {

            }
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("l", "ls")
            }

            override suspend fun execute(context: CommandContext) {

            }
        }

        class ChangeNameArg(parent: String) : AbstractCommand("$parent.changename") {

            init {
                name = "changeName"
                aliases = arrayOf("cn")
            }

            override suspend fun execute(context: CommandContext) {

            }
        }


        override suspend fun execute(context: CommandContext) {
            sendSyntax(context)
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }


    //TODO("rewrite")
    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("set")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context)
                return
            }

            val pair = getEmotejiByArgsNMessage(context, 0) ?: return
            var rname: String? = null
            val id = if (pair.first == null) {
                pair.second?.let { rname = it }
                pair.second
            } else {
                pair.first?.name?.let { rname = it }
                pair.first?.id
            } ?: return
            val name = rname
            require(name != null) { "what.." }

            val role = getRoleByArgsNMessage(context, 1) ?: return

            context.daoManager.selfRoleWrapper.set(context.guildId, id, role.idLong)

            val msg = context.getTranslation("$root.success")
                .replace("%emoteji%", name)
                .replace(PLACEHOLDER_ROLE, role.name)
            sendMsg(context, msg)
        }
    }

    //TODO("rewrite")
    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }


            val pair = getEmotejiByArgsN(context, 0)
            if (context.args[0].isNumber() && pair == null) {
                val roleId = context.daoManager.selfRoleWrapper.selfRoleCache.get(context.guildId).await()
                    .getOrElse(context.args[0]) {
                        null
                    }
                context.daoManager.selfRoleWrapper.remove(context.guildId, context.args[0])

                val msg = context.getTranslation("$root.success")
                    .replace("%emoteName%", context.args[0])
                    .replace("%role%", "<@&$roleId>")

                sendMsg(context, msg)
                return
            }

            if (pair == null) return

            val id = if (pair.first == null) {
                pair.second
            } else {
                pair.first?.id
            } ?: return

            val roleId = context.daoManager.selfRoleWrapper.selfRoleCache.get(context.guildId).await()
                .getOrElse(id) {
                    null
                }

            context.daoManager.selfRoleWrapper.remove(context.guildId, id)

            val msg = context.getTranslation("$root.success")
                .replace("%emoteName%", id)
                .replace("%role%", "<@&$roleId>")
            sendMsg(context, msg)
        }

    }

    //TODO("rewrite")
    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val wrapper = context.daoManager.selfRoleWrapper
            val map = wrapper.selfRoleCache.get(context.guildId).await()

            val language = context.getLanguage()
            val msg = if (map.isNotEmpty()) {
                val title = i18n.getTranslation(language, "$root.title")
                var content = "```ini\n[emoteji] - roleId - roleName"

                for ((emoteji, roleId) in map) {
                    val role = context.guild.getRoleById(roleId)
                    if (role == null) {
                        wrapper.remove(context.guildId, emoteji)
                        continue
                    }

                    content += "\n[$emoteji] - $roleId - ${role.name}"
                }
                content += "```"
                title + content
            } else {
                i18n.getTranslation(language, "$root.empty")
            }
            sendMsg(context, msg)
        }
    }
}