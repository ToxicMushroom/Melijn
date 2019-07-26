package me.melijn.melijnbot.commands.administration


import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlock
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetLanguageCommand : AbstractCommand() {
    private val root = "command.setlanguage"

    init {
        id = 2
        name = "setLanguage"
        syntax = Translateable("$root.syntax")
        aliases = arrayOf("setLang", "sl")
        description = Translateable("$root.description")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ListCommand())
    }


    override fun execute(context: CommandContext) {
        when {
            context.commandParts.size == 2 -> {
                sendCurrentLang(context)
            }
            context.commandParts.size == 3 -> {
                setLang(context)
            }
            else -> sendSyntax(context, syntax)
        }
    }

    private fun sendCurrentLang(context: CommandContext) {
        val dao = context.daoManager.guildLanguageWrapper
        val lang = dao.languageCache.get(context.guildId).get()


        sendMsg(context, replaceLang(
                Translateable("$root.currentlangresponse").string(context),
                lang
        ))
    }

    private fun setLang(context: CommandContext) {
        val lang: String
        val shouldUnset = "null".equals(context.commandParts[2], true)
        try {
            lang = if (shouldUnset) ""
            else Language.valueOf(context.commandParts[2].toUpperCase()).toString()
        } catch (ignored: IllegalArgumentException) {
            sendMsg(context,
                    replaceArg(
                            Translateable("$root.set.invalidarg").string(context),
                            context.commandParts
                    )
            )
            return
        }


        val dao = context.daoManager.guildLanguageWrapper
        dao.setLanguage(context.guildId, lang)


        val possible = if (shouldUnset) "un" else ""
        sendMsg(context, replaceLang(
                Translateable("$root.${possible}set.success").string(context),
                lang
        ))
    }

    private fun replaceArg(msg: String, commandParts: List<String>): String {
        return msg.replace("%argument%", commandParts[2]).replace("%prefix%", commandParts[0])
    }

    private fun replaceLang(msg: String, lang: String): String {
        return msg.replace("%language%", lang)
    }


    /** SUBCOMMAND list **/
    class ListCommand : AbstractCommand() {
        private val root = "command.setlanguage.list"

        init {
            name = "list"
            description = Translateable("$root.description")
        }

        override fun execute(context: CommandContext) {
            sendMsgCodeBlock(context, replaceLangList(
                    Translateable("$root.response1").string(context)
            ), "INI")
        }

        private fun replaceLangList(string: String): String {
            val sb = StringBuilder()
            var i = 1
            for (value in Language.values()) {
                sb.append(i++).append(" - [").append(value).append("]").append("\n")
            }
            return string.replace("%languageList%", sb.toString())
        }
    }
}