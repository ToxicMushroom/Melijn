package me.melijn.melijnbot.commands.administration


import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlock
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetLanguageCommand : AbstractCommand("command.setlanguage") {

    init {
        id = 2
        name = "setLanguage"
        aliases = arrayOf("setLang", "sl")
        commandCategory = CommandCategory.ADMINISTRATION
        children = arrayOf(ListCommand())
    }


    override suspend fun execute(context: CommandContext) {
        when {
            context.commandParts.size == 2 -> {
                sendCurrentLang(context)
            }
            context.commandParts.size == 3 -> {
                setLang(context)
            }
            else -> sendSyntax(context)
        }
    }

    private suspend fun sendCurrentLang(context: CommandContext) {
        val dao = context.daoManager.guildLanguageWrapper
        val lang = dao.languageCache.get(context.getGuildId()).await()

        val language = context.getLanguage()
        val unReplacedMsg = i18n.getTranslation(language, "$root.currentlangresponse")
        val msg = replaceLang(
            unReplacedMsg,
            lang
        )
        sendMsg(context, msg)
    }

    private suspend fun setLang(context: CommandContext) {
        val lang: String
        val shouldUnset = "null".equals(context.commandParts[2], true)
        try {
            lang = if (shouldUnset) ""
            else Language.valueOf(context.commandParts[2].toUpperCase()).toString()
        } catch (ignored: IllegalArgumentException) {
            val language = context.getLanguage()
            val unReplacedMsg = i18n.getTranslation(language, "$root.set.invalidarg")
            val msg = replaceArg(
                unReplacedMsg,
                context.commandParts
            )
            sendMsg(context, msg)
            return
        }


        val dao = context.daoManager.guildLanguageWrapper
        dao.setLanguage(context.getGuildId(), lang)


        val possible = if (shouldUnset) "un" else ""

        val language = context.getLanguage()
        val unReplacedMsg = i18n.getTranslation(language, "$root.${possible}set.success")
        val msg = replaceLang(
            unReplacedMsg,
            lang
        )
        sendMsg(context, msg)
    }

    private fun replaceArg(msg: String, commandParts: List<String>): String {
        return msg.replace("%argument%", commandParts[2]).replace("%prefix%", commandParts[0])
    }

    private fun replaceLang(msg: String, lang: String): String {
        return msg.replace("%language%", lang)
    }


    /** SUBCOMMAND list **/
    class ListCommand : AbstractCommand("command.setlanguage.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            val language = context.getLanguage()
            val unReplacedMsg = i18n.getTranslation(language, "$root.response1")
            val msg = replaceLangList(
                unReplacedMsg
            )
            sendMsgCodeBlock(context, msg, "INI")
        }

        private fun replaceLangList(string: String): String {
            val sb = StringBuilder()
            for ((index, value) in Language.values().withIndex()) {
                sb.append(index + 1).append(" - [").append(value).append("]").append("\n")
            }
            return string.replace("%languageList%", sb.toString())
        }
    }
}