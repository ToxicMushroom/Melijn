package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.scripts.Script
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendFeatureRequiresGuildPremiumMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax

class ScriptsCommand : AbstractCommand("command.scripts") {

    init {
        name = "scripts"
        aliases = arrayOf("shortcuts", "script", "shortcut")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val argRegex = "%arg(\\d+)%".toRegex()
        val lineArgRegex = "%line(\\d+)%".toRegex()
        val scriptArgRegex = "%line(\\d+)arg(\\d+)%".toRegex()
        private const val SCRIPTS_LIMIT = 5
        private const val PREMIUM_SCRIPTS_LIMIT = 25
        private const val CMD_PER_SCRIPT_LIMIT = 4
        private const val PREMIUM_CMD_PER_SCRIPT_LIMIT = 10
        private const val SCRIPTS_LIMIT_PATH = "premium.feature.scripts.limit"
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class ViewArg(parent: String) : AbstractCommand("$parent.view") {

        init {
            name = "view"
            aliases = arrayOf("vw")
        }

        override suspend fun execute(context: ICommandContext) {
            val trigger = getStringFromArgsNMessage(context, 0, 1, 128) ?: return
            val script = context.daoManager.scriptWrapper.getScripts(context.guildId)
                .firstOrNull { it.trigger.equals(trigger, true) }
            if (script == null) {
                sendRsp(
                    context,
                    "That script doesn't exist, see `%prefix%scripts list`".withSafeVarInCodeblock(
                        "prefix",
                        context.usedPrefix
                    )
                )
                return
            }

            val blub = script.trigger + script.commands.values.joinToString {
                "\"" + it.first + it.second.joinToString(" ", " ") + "\""
            }
            sendRsp(context, blub)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        override suspend fun execute(context: ICommandContext) {
            val scripts = context.daoManager.scriptWrapper.getScripts(context.guildId)
                .sortedBy { it.trigger }
            val index = getIntegerFromArgNMessage(context, 0, 1, scripts.size) ?: return
            val trigger = scripts[index].trigger
            context.daoManager.scriptWrapper.removeScript(context.guildId, trigger)

            sendRsp(context, "Removed the **%script%** script at **$index**".withSafeVariable("script", trigger))
        }
    }

    class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

        init {
            name = "remove"
            aliases = arrayOf("rm")
        }

        override suspend fun execute(context: ICommandContext) {
            val trigger = getStringFromArgsNMessage(context, 0, 1, 128) ?: return
            context.daoManager.scriptWrapper.removeScript(context.guildId, trigger)

            sendRsp(context, "Removed the **%script%** script".withSafeVariable("script", trigger))
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls", "view")
        }

        override suspend fun execute(context: ICommandContext) {
            val scripts = context.daoManager.scriptWrapper.getScripts(context.guildId)
                .sortedBy { it.trigger }

            if (scripts.isEmpty()) {
                sendRsp(context, "This server does not have any scripts")
                return
            }
            var msg = "Script list:```INI\n" +
                "# id - [trigger] - commandCount"

            var i = 1
            for ((_, trigger, commands) in scripts) {
                msg += "\n ${i++} - [$trigger] - ${commands.size} commands"
            }
            msg += "```"
            sendRsp(context, msg)
        }
    }

    class AddArg(parent: String) : AbstractCommand("$parent.add") {

        init {
            name = "add"
            aliases = arrayOf("a")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val scripts = context.daoManager.scriptWrapper.getScripts(context.guildId)
            if (scripts.size > SCRIPTS_LIMIT && !isPremiumUser(context)) {
                val replaceMap = mapOf(
                    "limit" to "$SCRIPTS_LIMIT",
                    "premiumLimit" to "$PREMIUM_SCRIPTS_LIMIT"
                )

                sendFeatureRequiresGuildPremiumMessage(context, SCRIPTS_LIMIT_PATH, replaceMap)
                return
            } else if (scripts.size >= PREMIUM_SCRIPTS_LIMIT) {
                val msg = context.getTranslation("$root.limit.total")
                    .withVariable("limit", "$PREMIUM_SCRIPTS_LIMIT")
                sendRsp(context, msg)
                return
            }

            val trigger = getStringFromArgsNMessage(context, 0, 1, 128) ?: return
            val scriptBody = mutableMapOf<Int, Pair<String, List<String>>>()

            val isPremium = isPremiumGuild(context)
            val correctLimit = if (isPremium) PREMIUM_CMD_PER_SCRIPT_LIMIT else CMD_PER_SCRIPT_LIMIT
            for ((index, arg) in context.args.drop(1).withIndex().take(correctLimit)) {
                val parts = arg.split(SPACE_PATTERN)
                val scriptArgs = mutableListOf<String>()
                var command = ""
                var scriptArgPartStarted = false
                for (scriptArg in parts) {
                    if (!scriptArgPartStarted && (argRegex.matches(scriptArg) || lineArgRegex.matches(scriptArg) ||
                            scriptArgRegex.matches(scriptArg))
                    ) {
                        scriptArgPartStarted = true
                    }
                    if (scriptArgPartStarted) {
                        scriptArgs.add(scriptArg)
                    } else {
                        command += "$scriptArg "
                    }
                }
                val scriptCommand = command.dropLast(1)
                scriptBody[index] = scriptCommand to scriptArgs
            }

            context.daoManager.scriptWrapper.addScript(
                context.guildId,
                Script(true, trigger, scriptBody, true)
            )

            sendRsp(context, "Added **%trigger%** script".withSafeVariable("trigger", trigger))
        }
    }
}