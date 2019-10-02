package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.util.stream.Collectors


class HelpCommand : AbstractCommand("command.help") {

    init {
        id = 6
        name = "help"
        aliases = arrayOf("commands", "command", "cmds", "cmd")
        children = arrayOf(ListArg(root))
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val args = context.args
        val language = context.getLanguage()
        if (args.isEmpty()) {
            val part = if (context.isFromGuild) "server" else "pm"
            val response = i18n.getTranslation(language, "$root.response1.$part")
            val msg = replaceArgs(response, context.getGuildId(), context.usedPrefix)
            sendMsg(context, msg)
            return
        }
        val commandList = context.getCommands()
        val parent = commandList.firstOrNull() { cmd -> cmd.isCommandFor(args[0]) }
        if (parent == null) {
            sendSyntax(context, syntax)
            return
        }
        val parentChildList = getCorrectChildElseParent(context, mutableListOf(parent), args)
        val command = parentChildList.last()

        var msg = i18n.getTranslation(language, "$root.response3.line1")
        msg += i18n.getTranslation(language, "$root.response3.line2")
        if (command.aliases.isNotEmpty()) {
            msg += i18n.getTranslation(language, "$root.response3.line3")
        }
        msg += i18n.getTranslation(language, "$root.response3.line4")
        if (command.help != "empty") {
            msg += i18n.getTranslation(language, "$root.response3.line5")
        }
        msg += i18n.getTranslation(language, "$root.response3.line6")
        msg = replaceCmdVars(msg, context, parentChildList)

        sendMsg(context, msg)
    }

    //Converts ("ping", "pong", "dunste") into a list of (PingCommand, PongArg, DunsteArg) if the args are matching an existing parent child sequence
    private fun getCorrectChildElseParent(context: CommandContext, cmdList: MutableList<AbstractCommand>, args: List<String>, argIndex: Int = 1): MutableList<AbstractCommand> {
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
        }

        override suspend fun execute(context: CommandContext) {
            val commandList = context.getCommands()
            val language = context.getLanguage()
            val title = i18n.getTranslation(language, "$root.title")
            val util = i18n.getTranslation(language, "$root.field1.title")
            val administration = i18n.getTranslation(language, "$root.field2.title")
            val moderation = i18n.getTranslation(language, "$root.field3.title")
            val music = i18n.getTranslation(language, "$root.field4.title")
            val image = i18n.getTranslation(language, "$root.field5.title")
            val animal = i18n.getTranslation(language, "$root.field6.title")
            val anime = i18n.getTranslation(language, "$root.field7.title")
            val supporter = i18n.getTranslation(language, "$root.field8.title")
            val commandAmount = i18n.getTranslation(language, "$root.footer")
                .replace("%cmdCount%", commandList.size.toString())

            val eb = Embedder(context)
            eb.setTitle(title, "https://melijn.com/commands")

            eb.addField(util, commandListString(commandList, CommandCategory.UTILITY), true)
            eb.addField(administration, commandListString(commandList, CommandCategory.ADMINISTRATION), true)
            eb.addField(moderation, commandListString(commandList, CommandCategory.MODERATION), true)
            eb.addField(music, commandListString(commandList, CommandCategory.MUSIC), true)
            eb.addField(image, commandListString(commandList, CommandCategory.IMAGE), true)
            eb.addField(animal, commandListString(commandList, CommandCategory.ANIMAL), true)
            eb.addField(anime, commandListString(commandList, CommandCategory.ANIME), true)
            eb.addField(supporter, commandListString(commandList, CommandCategory.SUPPORTER), true)

            eb.setFooter(commandAmount, null)

            sendEmbed(context, eb.build())
        }

    }


    private suspend fun replaceCmdVars(msg: String, context: CommandContext, parentChildList: List<AbstractCommand>): String {
        val command = parentChildList.last()
        val name = parentChildList.joinToString(" ") { cmd -> cmd.name }
        return msg
            .replace("%cmdName%", name)
            .replace("%cmdSyntax%", i18n.getTranslation(context.getLanguage(), command.syntax))
            .replace("%cmdAliases%", command.aliases.joinToString())
            .replace("%cmdDescription%", i18n.getTranslation(context.getLanguage(), command.description))
            .replace("%cmdHelp%", i18n.getTranslation(context.getLanguage(), command.help))
            .replace("%cmdCategory%", command.commandCategory.toString())
            .replace("%prefix%", context.usedPrefix)
    }


    private fun replaceArgs(string: String, guildId: Long, usedPrefix: String): String = string
        .replace("%guildId%", guildId.toString())
        .replace("%prefix%", usedPrefix)

}

private fun commandListString(list: Set<AbstractCommand>, category: CommandCategory): String = list
    .stream()
    .filter { command -> command.commandCategory == category }
    .map { fCommand -> fCommand.name }
    .collect(Collectors.joining("\n"))
