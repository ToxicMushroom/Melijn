package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import java.util.stream.Collectors


class HelpCommand : AbstractCommand("command.help") {

    init {
        id = 6
        name = "help"
        syntax = Translateable("$root.syntax")
        aliases = arrayOf("commands", "command", "cmds", "cmd")
        description = Translateable("$root.description")
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        val args = context.args
        if (args.isEmpty()) {
            if (context.isFromGuild) {
                sendMsg(context, replaceArgs(Translateable("$root.response1.server").string(context), context.guildId, context.usedPrefix))
            } else {
                sendMsg(context, replaceArgs(Translateable("$root.response1.pm").string(context), context.guildId, context.usedPrefix))
            }
        } else {
            val commandList = context.getCommands()
            if (args[0].equals("list", ignoreCase = true)) {

                val title = Translateable("$root.response2.title").string(context)
                val util = Translateable("$root.response2.field1.title").string(context)
                val administration = Translateable("$root.response2.field2.title").string(context)
                val moderation = Translateable("$root.response2.field3.title").string(context)
                val music = Translateable("$root.response2.field4.title").string(context)
                val image = Translateable("$root.response2.field5.title").string(context)
                val animal = Translateable("$root.response2.field6.title").string(context)
                val anime = Translateable("$root.response2.field7.title").string(context)
                val supporter = Translateable("$root.response2.field8.title").string(context)
                val commandAmount = Translateable("$root.response2.footer").string(context)
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
                if (command.isCommandFor(args[0])) {
                    var msg = Translateable("$root.response3.line1").string(context)
                    msg += Translateable("$root.response3.line2").string(context)
                    if (command.aliases.isNotEmpty())
                        msg += Translateable("$root.response3.line3").string(context)
                    msg += Translateable("$root.response3.line4").string(context)
                    if (command.help.path != "empty")
                        msg += Translateable("$root.response3.line5").string(context)
                    msg += Translateable("$root.response3.line6").string(context)

                    msg = replaceCmdVars(msg, context, command)

                    sendMsg(context, msg)
                    return
                }
            }

            sendSyntax(context, syntax)
        }
    }

    private fun replaceCmdVars(msg: String, context: CommandContext, command: AbstractCommand): String {
        return msg
                .replace("%cmdName%", command.name)
                .replace("%cmdSyntax%", command.syntax.string(context))
                .replace("%cmdAliases%", command.aliases.joinToString())
                .replace("%cmdDescription%", command.description.string(context))
                .replace("%cmdHelp%", command.help.string(context))
                .replace("%cmdCategory%", command.commandCategory.toString())
                .replace("%prefix%", context.usedPrefix)
    }

    fun commandListString(list: Set<AbstractCommand>, category: CommandCategory): String {
        return list.stream()
                .filter { command -> command.commandCategory == category }
                .map { fCommand -> fCommand.name }
                .collect(Collectors.joining("\n"))
    }

    private fun replaceArgs(string: String, guildId: Long, usedPrefix: String): String {
        return string.replace("%guildId%", guildId.toString()).replace("%prefix%", usedPrefix)
    }
}