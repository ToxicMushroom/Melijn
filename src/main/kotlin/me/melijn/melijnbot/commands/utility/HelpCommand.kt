package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
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
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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

        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        val args = context.args
        val language = context.getLanguage()
        if (args.isEmpty()) {
            val part = if (context.isFromGuild) "server" else "pm"
            val response = i18n.getTranslation(language, "$root.response1.$part")
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
            help = help.replace(og, context.getTranslation(path))
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
