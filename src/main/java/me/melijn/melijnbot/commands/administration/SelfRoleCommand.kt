package me.melijn.melijnbot.commands.administration

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getEmoteByArgsNMessage
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

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
        sendSyntax(context, syntax)
    }

    class AddArg(root: String) : AbstractCommand("$root.add") {

        init {
            name = "set"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.size < 2) {
                sendSyntax(context, syntax)
                return
            }
            val role = getRoleByArgsNMessage(context, 0) ?: return
            val emote = getEmoteByArgsNMessage(context, 1) ?: return

            context.daoManager.selfRoleWrapper.set(context.getGuildId(), role.idLong, emote.idLong)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
            sendMsg(context, msg)
        }
    }

    class RemoveArg(root: String) : AbstractCommand("$root.remove") {

        init {
            name = "remove"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context, syntax)
                return
            }
            val role = getRoleByArgsNMessage(context, 0) ?: return

            context.daoManager.selfRoleWrapper.remove(context.getGuildId(), role.idLong)

            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, "$root.success")
            sendMsg(context, msg)
        }
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            val map = context.daoManager.selfRoleWrapper.selfRoleCache.get(context.getGuildId()).await()

            val language = context.getLanguage()
            val msg = if (map.isNotEmpty()) {
                val title = i18n.getTranslation(language, "$root.title")
                var content = "```ini\n[roleId] - emoteId"

                for ((roleId, emoteId) in map) {
                    content += "\n[$roleId] - $emoteId"
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