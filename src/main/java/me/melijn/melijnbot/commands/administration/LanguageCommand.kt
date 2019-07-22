package me.melijn.melijnbot.commands.administration


import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.replacePrefix
import me.melijn.melijnbot.objects.utils.sendMsg

class LanguageCommand : AbstractCommand() {

    init {
        id = 2
        name = "language"
        aliases = arrayOf("lang")
        description = Translateable("command.language.description")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ListCommand())
    }

    override fun execute(context: CommandContext) {
        sendMsg(context, syntax.replacePrefix(context))
    }

    class ListCommand : AbstractCommand() {

        init {
            name = "list"
            description = Translateable("command.language.list.description")
        }

        override fun execute(context: CommandContext) {
            sendMsg(context, replaceLangList(
                    Translateable("command.language.list.response1").string(context)
            ))
        }

        private fun replaceLangList(string: String): String {
            val sb = StringBuilder()
            var i = 1
            repeat(200) {
                for (value in Language.values()) {
                    sb.append(i++).append(" - [").append(value).append("]").append("\n")
                }
            }
            return string.replace("%languageList%", sb.toString())
        }
    }

    class SetCommand : AbstractCommand() {

        init {
            name = "set"
            description = Translateable("command.language.set.description")
        }

        override fun execute(context: CommandContext) {
            sendMsg(context,
                    Translateable("command.language.set.response1").string(context)
            )
        }
    }
}