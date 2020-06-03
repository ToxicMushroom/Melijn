package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.jagtag.BirthdayMethods
import me.melijn.melijnbot.objects.jagtag.CCMethods
import me.melijn.melijnbot.objects.jagtag.DiscordMethods
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.*
import net.dv8tion.jda.api.utils.MarkdownSanitizer
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
            val pathExtra = "help.arg.${context.rawArg.toLowerCase()}.examples"
            val translation = context.getTranslation(path)
                .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
            val translationExtra = context.getTranslation(pathExtra)
            val hasExtra = translationExtra != pathExtra
            if (path == translation) {
                val msg = context.getTranslation("$root.missing")
                    .replace(PLACEHOLDER_ARG, context.rawArg)
                sendMsg(context, msg)
                return
            }

            val title = context.getTranslation("$root.embed.title")
                .replace("%argName%", context.rawArg)
            val embedder = Embedder(context)
            embedder.setTitle(title)
            embedder.setDescription(translation)
            if (hasExtra) {
                val examples = context.getTranslation("$root.embed.examples")
                embedder.addField(examples, translationExtra, false)
            }
            sendEmbed(context, embedder.build())
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val inStream = Thread.currentThread().contextClassLoader.getResourceAsStream("strings_en.properties")
                    ?: return
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
                val cool = list.joinToString("`, `", "`", "`")
                val splitOmg = StringUtils.splitMessage(cool, 800, 1024)
                for (s in splitOmg) {
                    eb.addField(title, s, false)
                }

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
            val path = "help.var.${context.rawArg.toLowerCase().remove("{", "}")}"
            val translation = context.getTranslation(path)
            if (path == translation) {
                val msg = context.getTranslation("$root.missing")
                    .replace(PLACEHOLDER_ARG, context.rawArg)
                sendMsg(context, msg)
                return
            }

            val title = context.getTranslation("$root.embed.title")
                .replace("%varName%", "{${context.rawArg.remove("{", "}")}}")
            val embedder = Embedder(context)
            embedder.setTitle(title)
            embedder.setDescription(translation)

            sendEmbed(context, embedder.build())
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: CommandContext) {
                val dList = DiscordMethods.getMethods().map { method -> method.name }
                val ccList = CCMethods.getMethods().map { method -> method.name }
                val bList = BirthdayMethods.getMethods().map { method -> method.name }
                val eb = Embedder(context)

                eb.addField("CustomCommand", ccList.joinToString("}`, `{", "`{", "}`"), false)
                eb.addField("Discord", dList.joinToString("}`, `{", "`{", "}`"), false)
                eb.addField("Birthday", bList.joinToString("}`, `{", "`{", "}`"), false)
                sendEmbed(context, eb.build())
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        val args = context.args
        if (args.isEmpty()) {
            val title = context.getTranslation("$root.embed.title")
            val description = context.getTranslation("$root.embed.description")
                .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
                .replace("%melijnMention%", if (context.isFromGuild) context.selfMember.asMention else context.selfUser.asMention)
            val embedder = Embedder(context)
            embedder.setTitle(title)
            embedder.setDescription(description)

            sendEmbed(context, embedder.build())
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

        val name = parentChildList.joinToString(" ") { cmd -> cmd.name }
        val cmdTitle = context.getTranslation("$root.cmd.title")
            .replace("%cmdName%", name)
        val cmdSyntax = context.getTranslation("$root.cmd.syntax")
        val cmdAliases = context.getTranslation("$root.cmd.aliases")
        val cmdDesc = context.getTranslation("$root.cmd.description")
        val cmdHelp = context.getTranslation("$root.cmd.help")
        val cmdArguments = context.getTranslation("$root.cmd.arguments")
        val cmdExamples = context.getTranslation("$root.cmd.examples")
        val cmdCategory = context.getTranslation("$root.cmd.category")
        val cmdHelpValue = i18n.getTranslationN(context.getLanguage(), command.help, false)
            ?.replace(PLACEHOLDER_PREFIX, context.usedPrefix)
        val cmdArgumentsValue = i18n.getTranslationN(context.getLanguage(), command.arguments, false)
            ?.replace(PLACEHOLDER_PREFIX, context.usedPrefix)
        val cmdExamplesValue = i18n.getTranslationN(context.getLanguage(), command.examples, false)
            ?.replace(PLACEHOLDER_PREFIX, context.usedPrefix)

        val embedder = Embedder(context)
        embedder.setTitle(cmdTitle)
        embedder.addField(
            cmdSyntax,
            MarkdownSanitizer.escape(
                context.getTranslation(command.syntax)
                    .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
            )
            , false
        )
        if (command.aliases.isNotEmpty()) {
            embedder.addField(cmdAliases, command.aliases.joinToString(), false)
        }

        embedder.addField(
            cmdDesc,
            context.getTranslation(command.description)
                .replace(PLACEHOLDER_PREFIX, context.usedPrefix)
            , false
        )

        cmdArgumentsValue?.let {
            var help = it.replace(PLACEHOLDER_PREFIX, context.usedPrefix)

            val matcher = Pattern.compile("%(help\\.arg\\..+)%").matcher(help)
            while (matcher.find()) {
                val og = matcher.group(0)
                val path = matcher.group(1)
                help = help.replace(og, "*" + context.getTranslation(path).replace(PLACEHOLDER_PREFIX, context.usedPrefix))
            }
            for (argumentsPart in StringUtils.splitMessage(help, splitAtLeast = 750, maxLength = 1024)) {
                embedder.addField(cmdArguments, argumentsPart, false)
            }
        }

        cmdExamplesValue?.let {
            for (examplesPart in StringUtils.splitMessage(it, splitAtLeast = 750, maxLength = 1024)) {
                embedder.addField(cmdExamples, examplesPart, false)
            }
        }

        cmdHelpValue?.let {
            for (helpPart in StringUtils.splitMessage(it, splitAtLeast = 750, maxLength = 1024)) {
                embedder.addField(cmdHelp, helpPart, false)
            }
        }

        embedder.addField(cmdCategory, parent.commandCategory.toLCC(), false)

        sendEmbed(context, embedder.build())
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
        val child = commandList.firstOrNull { child ->
            child.isCommandFor(args[argIndex])
        } ?: cmdList.last()

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
}

private fun commandListString(list: List<AbstractCommand>, category: CommandCategory): String = list
    .filter { command -> command.commandCategory == category }
    .joinToString("`, `", "`", "`") { cmd ->
        cmd.name
    }
