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
        if (args[0].equals("list", ignoreCase = true)) {

            val title = i18n.getTranslation(language, "$root.response2.title")
            val util = i18n.getTranslation(language, "$root.response2.field1.title")
            val administration = i18n.getTranslation(language, "$root.response2.field2.title")
            val moderation = i18n.getTranslation(language, "$root.response2.field3.title")
            val music = i18n.getTranslation(language, "$root.response2.field4.title")
            val image =i18n.getTranslation(language, "$root.response2.field5.title")
            val animal = i18n.getTranslation(language, "$root.response2.field6.title")
            val anime = i18n.getTranslation(language, "$root.response2.field7.title")
            val supporter = i18n.getTranslation(language, "$root.response2.field8.title")
            val commandAmount = i18n.getTranslation(language, "$root.response2.footer")
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
            return
        }

        for (command in commandList) {
            if (!command.isCommandFor(args[0])) continue
            var msg = i18n.getTranslation(language, "$root.response3.line1")
            msg += i18n.getTranslation(language, "$root.response3.line2")
            if (command.aliases.isNotEmpty()){
                msg += i18n.getTranslation(language, "$root.response3.line3")
            }
            msg += i18n.getTranslation(language, "$root.response3.line4")
            if (command.help != "empty") {
                msg += i18n.getTranslation(language, "$root.response3.line5")
            }
            msg += i18n.getTranslation(language, "$root.response3.line6")

            msg = replaceCmdVars(msg, context, command)

            sendMsg(context, msg)
            return
        }

        sendSyntax(context, syntax)
    }


    private suspend fun replaceCmdVars(msg: String, context: CommandContext, command: AbstractCommand): String = msg
            .replace("%cmdName%", command.name)
            .replace("%cmdSyntax%", i18n.getTranslation(context.getLanguage(), command.syntax))
            .replace("%cmdAliases%", command.aliases.joinToString())
            .replace("%cmdDescription%", i18n.getTranslation(context.getLanguage(), command.description))
            .replace("%cmdHelp%", i18n.getTranslation(context.getLanguage(), command.help))
            .replace("%cmdCategory%", command.commandCategory.toString())
            .replace("%prefix%", context.usedPrefix)


    private fun commandListString(list: Set<AbstractCommand>, category: CommandCategory): String = list
            .stream()
            .filter { command -> command.commandCategory == category }
            .map { fCommand -> fCommand.name }
            .collect(Collectors.joining("\n"))


    private fun replaceArgs(string: String, guildId: Long, usedPrefix: String): String = string
            .replace("%guildId%", guildId.toString())
            .replace("%prefix%", usedPrefix)

}