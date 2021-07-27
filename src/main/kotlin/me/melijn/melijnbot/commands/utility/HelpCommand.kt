package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.jagtag.BirthdayMethods
import me.melijn.melijnbot.internals.jagtag.CCMethods
import me.melijn.melijnbot.internals.jagtag.DiscordMethods
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.getSyntax
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern

// webhook chain test
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

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val path = "help.arg.${context.rawArg.lowercase()}"
            val pathExtra = "help.arg.${context.rawArg.lowercase()}.examples"
            val translation = context.getTranslation(path)
                .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            val translationExtra = context.getTranslation(pathExtra)
            val hasExtra = translationExtra != pathExtra
            if (path == translation) {
                val msg = context.getTranslation("$root.missing")
                    .withSafeVariable(PLACEHOLDER_ARG, context.rawArg)
                sendRsp(context, msg)
                return
            }

            val title = context.getTranslation("$root.embed.title")
                .withSafeVariable("argName", context.rawArg)

            val embedder = Embedder(context)
                .setTitle(title)
                .setDescription(translation)

            if (hasExtra) {
                val examples = context.getTranslation("$root.embed.examples")
                embedder.addField(examples, translationExtra, false)
            }

            sendEmbedRsp(context, embedder.build())
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: ICommandContext) {
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

                sendEmbedRsp(context, eb.build())
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

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }
            val path = "help.var.${context.rawArg.lowercase().remove("{", "}")}"
            val translation = context.getTranslation(path)
            if (path == translation) {
                val msg = context.getTranslation("$root.missing")
                    .withSafeVariable(PLACEHOLDER_ARG, context.rawArg)
                sendRsp(context, msg)
                return
            }

            val title = context.getTranslation("$root.embed.title")
                .withSafeVariable("varName", "{${context.rawArg.remove("{", "}")}}")

            val embedder = Embedder(context)
                .setTitle(title)
                .setDescription(translation)

            sendEmbedRsp(context, embedder.build())
        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: ICommandContext) {
                val dList = DiscordMethods.getMethods().map { method -> method.name }
                val ccList = CCMethods.getMethods().map { method -> method.name }
                val bList = BirthdayMethods.getMethods().map { method -> method.name }

                val eb = Embedder(context)
                    .addField("CustomCommand", ccList.joinToString("}`, `{", "`{", "}`"), false)
                    .addField("Discord", dList.joinToString("}`, `{", "`{", "}`"), false)
                    .addField("Birthday", bList.joinToString("}`, `{", "`{", "}`"), false)

                sendEmbedRsp(context, eb.build())
            }
        }
    }

    override suspend fun execute(context: ICommandContext) {
        val args = context.args
        if (args.isEmpty()) {
            val daoManager = context.daoManager
            var prefixes = (if (context.isFromGuild) {
                daoManager.guildPrefixWrapper.getPrefixes(context.guildId) +
                    daoManager.userPrefixWrapper.getPrefixes(context.authorId)
            } else {
                daoManager.userPrefixWrapper.getPrefixes(context.authorId)
            }).sortedBy { it.length }

            if (prefixes.isEmpty()) {
                prefixes = listOf(context.container.settings.botInfo.prefix)
            }
            val title = context.getTranslation("$root.embed.title")
            val description = context.getTranslation("$root.embed.description")
                .withSafeVariable("serverPrefix", prefixes.first())
                .withSafeVariable(PLACEHOLDER_PREFIX, prefixes.first())
                .withVariable(
                    "melijnMention",
                    if (context.isFromGuild) context.selfMember.asMention else context.selfUser.asMention
                )

            val embedder = Embedder(context)
                .setTitle(title)
                .setDescription(description)
                .setFooter("@${context.selfUser.asTag} is always a valid prefix")

            val messageBuilder = MessageBuilder()
                .setEmbeds(embedder.build())
                .setActionRows(
                    ActionRow.of(
                        Button.primary("help_list", "command list")
                    )
                )
            sendRsp(context, messageBuilder.build())
            return
        }
        val commandList = context.commandList
        val parent = commandList.firstOrNull { cmd -> cmd.isCommandFor(args[0]) }
        if (parent == null) {
            sendSyntax(context)
            return
        }
        val parentChildList = getCorrectChildElseParent(context, mutableListOf(parent), args)
        val command = parentChildList.last()

        val name = parentChildList.joinToString(" ") { cmd -> cmd.name }
        val cmdTitle = context.getTranslation("$root.cmd.title")
            .withVariable("cmdName", name)
        val cmdSyntax = context.getTranslation("$root.cmd.syntax")
        val cmdAliases = context.getTranslation("$root.cmd.aliases")
        val cmdDesc = context.getTranslation("$root.cmd.description")
        val cmdHelp = context.getTranslation("$root.cmd.help")
        val cmdArguments = context.getTranslation("$root.cmd.arguments")
        val cmdExamples = context.getTranslation("$root.cmd.examples")
        val cmdCategory = context.getTranslation("$root.cmd.category")
        val cmdHelpValue = i18n.getTranslationN(context.getLanguage(), command.help, false)
            ?.withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
        val cmdArgumentsValue = i18n.getTranslationN(context.getLanguage(), command.arguments, false)
            ?.withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
        val cmdExamplesValue = i18n.getTranslationN(context.getLanguage(), command.examples, false)
            ?.withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)

        val embedder = Embedder(context)
            .setTitle(
                cmdTitle,
                "https://melijn.com/commands?q=${parent.name}&c=${parent.commandCategory.toString().lowercase()}"
            )
            .setDescription(
                context.getTranslation(command.description)
                    .withSafeVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            )
            .addField(
                cmdSyntax,
                "**" +
                    getSyntax(context, command.syntax)
                        .escapeMarkdown()
                        .withSafeVarInCodeblock(PLACEHOLDER_PREFIX, context.usedPrefix, true)
                    + "**",
                false
            )

        if (command.aliases.isNotEmpty()) {
            embedder.addField(cmdAliases, command.aliases.joinToString(), false)
        }

        cmdArgumentsValue?.let {
            var help = it.withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)

            val matcher = Pattern.compile("%(help\\.arg\\..+)%").matcher(help)
            while (matcher.find()) {
                val og = matcher.group(0)
                val path = matcher.group(1)
                help = help.replace(
                    og,
                    " ⁎ " + context.getTranslation(path).withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                )
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

        if (command.children.isNotEmpty()) {

            val parts = StringUtils.splitMessage(command.children.joinToString("\n") {
                val subDesc = runBlocking { context.getTranslation(it.description) }
                " 🔹 **$name ${it.name}** - $subDesc"
            }, splitAtLeast = 750, maxLength = 1024)
            for ((index, part) in parts.withIndex()) {
                embedder.addField(
                    (if (index > 0) "More " else "") + "SubCommands",
                    part,
                    false
                )
            }
        }
        embedder.setFooter("$cmdCategory: " + parent.commandCategory.toUCC())
        sendEmbedRsp(context, embedder.build())
    }

    //Converts ("ping", "pong", "dunste") into a list of (PingCommand, PongArg, DunsteArg) if the args are matching an existing parent child sequence
    private fun getCorrectChildElseParent(
        context: ICommandContext,
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

        companion object {
            fun getHelpListMessage(
                container: Container,
                textChannel: TextChannel?,
                user: User,

                category: CommandCategory?
            ): EmbedBuilder {
                val root = "command.help.list"
                val commandList = container.commandSet
                    .filter { cmd -> category == null || cmd.commandCategory == category }
                    .sortedBy { cmd -> cmd.name }

                val title = i18n.getTranslation("en", "$root.title")

                val categoryPathMap = mutableMapOf(
                    Pair(CommandCategory.ADMINISTRATION, "$root.field2.title"),
                    Pair(CommandCategory.ANIMAL, "$root.field6.title"),
                    Pair(CommandCategory.ANIME, "$root.field7.title"),
                    Pair(CommandCategory.ECONOMY, "$root.field8.title"),
                    Pair(CommandCategory.GAME, "$root.field9.title"),
                    Pair(CommandCategory.IMAGE, "$root.field5.title"),
                    Pair(CommandCategory.MODERATION, "$root.field3.title"),
                    Pair(CommandCategory.MUSIC, "$root.field4.title"),
                    Pair(CommandCategory.UTILITY, "$root.field1.title")
                )

                if (textChannel != null && textChannel.isNSFW) {
                    categoryPathMap[CommandCategory.NSFW] = "$root.field10.title"
                }
                val categoryFiltered = categoryPathMap.filter { entry ->
                    entry.key == category || category == null
                }

                val commandAmount = i18n.getTranslation("en", "$root.footer")
                    .withVariable("cmdCount", commandList.size.toString())

                val eb = Embedder(container.daoManager, textChannel?.guild?.idLong ?: -1, user.idLong)
                    .setTitle(title, "https://melijn.com/commands")
                    .setFooter(commandAmount, null)

                categoryFiltered.toSortedMap { o1, o2 ->
                    o1.toString().compareTo(o2.toString())
                }.forEach { entry ->
                    eb.addField(
                        i18n.getTranslation("en", entry.value),
                        commandListString(commandList, entry.key),
                        false
                    )
                }
                return eb
            }
        }

        override suspend fun execute(context: ICommandContext) {
            val category = getEnumFromArgN<CommandCategory>(context, 0)

            val textChannel = if (context.isFromGuild) context.message.textChannel else null
            val eb = getHelpListMessage(context.container, textChannel, context.author, category)

            sendEmbedRsp(context, eb.build())
        }

    }
}

private fun commandListString(list: List<AbstractCommand>, category: CommandCategory): String = list
    .filter { command -> command.commandCategory == category }
    .joinToString("`, `", "`", "`") { cmd ->
        cmd.name
    }
