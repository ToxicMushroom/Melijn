package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.database.scripts.Script
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.utils.SPACE_PATTERN
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable

class ScriptsCommand : AbstractCommand("command.scripts") {

    init {
        name = "scripts"
        aliases = arrayOf("shortcuts", "script", "shortcut")
        children = arrayOf(
            AddArg(root),
            RemoveArg(root),
            ListArg(root)
        )
        runConditions = arrayOf(RunCondition.GUILD_SUPPORTER)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        val argRegex = "%arg(\\d+)%".toRegex()
        val lineArgRegex = "%line(\\d+)%".toRegex()
        val scriptArgRegex = "%line(\\d+)arg(\\d+)%".toRegex()
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
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

            if (scripts.isEmpty()) {
                sendRsp(context, "This server does not have any scripts")
                return
            }
            var msg = "Script list:"
            for ((_, trigger, commands) in scripts) {
                msg += "\n ◦ $trigger - ${commands.size} commands"
            }
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

            val trigger = getStringFromArgsNMessage(context, 0, 1, 128) ?: return
            val scriptBody = mutableMapOf<Int, Pair<String, List<String>>>()
            for ((index, arg) in context.args.drop(1).withIndex()) {
                val parts = arg.split(SPACE_PATTERN)
                val scriptArgs = mutableListOf<String>()
                var command = ""
                for (scriptArg in parts) {
                    if (argRegex.matches(scriptArg) || lineArgRegex.matches(scriptArg) ||
                        scriptArgRegex.matches(scriptArg)
                    ) {
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