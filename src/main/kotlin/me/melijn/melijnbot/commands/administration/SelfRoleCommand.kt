package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*

class SelfRoleCommand : AbstractCommand("command.selfrole") {

    init {
        id = 37
        name = "selfRole"
        aliases = arrayOf("sr")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }

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