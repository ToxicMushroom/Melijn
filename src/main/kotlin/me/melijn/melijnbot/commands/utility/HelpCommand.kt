package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.jagtag.DiscordMethods
import me.melijn.melijnbot.objects.utils.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern


class HelpCommand : AbstractCommand("command.help") {

    init {
        id = 6
        name = "help"
        aliases = arrayOf("commands", "command", "cmds", "cmd")
        children = arrayOf(
            ListArg(root),
            VariableArg(root),
            ArgArg(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    class ArgArg(parent: String) : AbstractCommand("$parent.argument") {

        init {
            name = "argument"
            aliases = arrayOf("args", "arg", "arguments")
            children = arrayOf(
                ListArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val path = "help.arg.${context.rawArg.toLowerCase()}"
            val pathExtra = "help.arg.${context.rawArg.toLowerCase()}.help"
            val translation = context.getTranslation(path)
            val translationExtra = context.getTranslation(path)
            val hasExtra = translationExtra != pathExtra
            if (path == translation) {
                sendSyntax(context)
                return
            }

            val msg = if (hasExtra) {
                translation + "\n" + translationExtra
            } else {
                translation
            }
            sendMsg(context, msg)
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val inStream = Thread.currentThread().contextClassLoader.getResourceAsStream("strings_en.properties") ?: return
                val ir = InputStreamReader(inStream)
                val list = mutableListOf<String>()
                val pattern = Pattern.compile(".*%help\\.arg\\.(([a-zA-Z]+|\\.){1,5})%.*")
                for (line in BufferedReader(ir).lines()) {
                    val matcher = pattern.matcher(line)
                    if (matcher.find()) {
                        val s = matcher.group(1)
                        if (!list.contains(s))
                            list.add(s)
                    }
                }
                val eb = Embedder(context)
                val title = context.getTranslation("$root.title")
                eb.addField(title, list.joinToString("`, `", "`", "`"), false)
                sendEmbed(context, eb.build())
            }
        }
    }

    class VariableArg(parent: String) : AbstractCommand("$parent.variable") {

        init {
            name = "variable"
            aliases = arrayOf("variables", "vars", "var")
            children = arrayOf(
                ListArg(root)
            )
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val path = "help.var.${context.rawArg}"
            val translation = context.getTranslation(path)
            if (path == translation) {
                sendSyntax(context)
                return
            }

            sendMsg(context, translation)
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val dList = DiscordMethods.getMethods().map { method -> method.name }
                val ccList = DiscordMethods.getMethods().map { method -> method.name }
                val eb = Embedder(context)

                eb.addField("CustomCommand", ccList.joinToString("}`, `{", "`{", "}`"), false)
                eb.addField("Discord", dList.joinToString("}`, `{", "`{", "}`"), false)
                sendEmbed(context, eb.build())
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        val args = context.args
        if (args.isEmpty()) {
            val part = if (context.isFromGuild) "server" else "pm"
            val response = context.getTranslation("$root.response1.$part")
            val msg = replaceArgs(response, if (context.isFromGuild) context.guildId else -1L, context.usedPrefix)
            sendMsg(context, msg)
            return
        }
        val commandList = context.commandList
        val parent = commandList.firstOrNull() { cmd -> cmd.isCommandFor(args[0]) }
        if (parent == null) {
            sendSyntax(context)
            return
        }
        val parentChildList = getCorrectChildElseParent(context, mutableListOf(parent), args)
        val command = parentChildList.last()

        var msg = context.getTranslation("$root.response3.line1")
        msg += context.getTranslation("$root.response3.line2")
        if (command.aliases.isNotEmpty()) {
            msg += context.getTranslation("$root.response3.line3")
        }
        msg += context.getTranslation("$root.response3.line4")
        if (command.help != context.getTranslation(command.help)) {
            msg += context.getTranslation("$root.response3.line5")
        }
        msg += context.getTranslation("$root.response3.line6")
        msg = replaceCmdVars(msg, context, parentChildList)

        sendMsg(context, msg)
    }

    //Converts ("ping", "pong", "dunste") into a list of (PingCommand, PongArg, DunsteArg) if the args are matching an existing parent child sequence
    private fun getCorrectChildElseParent(
        context: CommandContext,
        cmdList: MutableList<AbstractCommand>,
        args: List<String>,
        argIndex: Int = 1
    ): MutableList<AbstractCommand> {
        if (argIndex >= args.size) return cmdList
        val commandList = cmdList.last().children
        val child = commandList.firstOrNull { child -> child.isCommandFor(args[argIndex]) } ?: cmdList.last()
        return if (child == cmdList.last()) {
            cmdList
        } else {
            cmdList.add(child)
            getCorrectChildElseParent(context, cmdList, args, argIndex + 1)
        }
    }

    class ListArg(root: String) : AbstractCommand("$root.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: CommandContext) {
            val category = getEnumFromArgN<CommandCategory>(context, 0)

            val commandList = context.commandList
                .filter { cmd -> category == null || cmd.commandCategory == category }
                .sortedBy { cmd -> cmd.name }

            val title = context.getTranslation("$root.title")

            //Alphabetical order
            val categoryPathMap = mapOf(
                Pair(CommandCategory.ADMINISTRATION, "$root.field2.title"),
                Pair(CommandCategory.ANIMAL, "$root.field6.title"),
                Pair(CommandCategory.ANIME, "$root.field7.title"),
                Pair(CommandCategory.IMAGE, "$root.field5.title"),
                Pair(CommandCategory.MODERATION, "$root.field3.title"),
                Pair(CommandCategory.MUSIC, "$root.field4.title"),
                Pair(CommandCategory.SUPPORTER, "$root.field8.title"),
                Pair(CommandCategory.UTILITY, "$root.field1.title")
            ).filter { entry ->
                entry.key == category || category == null
            }

            val commandAmount = context.getTranslation("$root.footer")
                .replace("%cmdCount%", commandList.size.toString())

            val eb = Embedder(context)
            eb.setTitle(title, "https://melijn.com/commands")

            categoryPathMap.forEach { entry ->
                eb.addField(context.getTranslation(entry.value), commandListString(commandList, entry.key), false)
            }

            eb.setFooter(commandAmount, null)

            sendEmbed(context, eb.build())
        }
    }


    private suspend fun replaceCmdVars(msg: String, context: CommandContext, parentChildList: List<AbstractCommand>): String {
        val command = parentChildList.last()
        val name = parentChildList.joinToString(" ") { cmd -> cmd.name }
        var help = context.getTranslation(command.help)

        val matcher = Pattern.compile("%(help\\.arg\\..+)%").matcher(help)
        while (matcher.find()) {
            val og = matcher.group(0)
            val path = matcher.group(1)
            help = help.replace(og, "*" + context.getTranslation(path))
        }

        return msg
            .replace("%cmdName%", name)
            .replace("%cmdSyntax%", context.getTranslation(command.syntax))
            .replace("%cmdAliases%", command.aliases.joinToString())
            .replace("%cmdDescription%", context.getTranslation(command.description))
            .replace("%cmdHelp%", help)
            .replace("%cmdCategory%", command.commandCategory.toLCC())
            .replace("%prefix%", context.usedPrefix)
    }


    private fun replaceArgs(string: String, guildId: Long, usedPrefix: String): String = string
        .replace("%guildId%", guildId.toString())
        .replace("%prefix%", usedPrefix)

}

private fun commandListString(list: List<AbstractCommand>, category: CommandCategory): String = list
    .filter { command -> command.commandCategory == category }
    .joinToString("`, `", "`", "`") { cmd ->
        cmd.name
    }
