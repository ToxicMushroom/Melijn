package me.melijn.melijnbot.commands.administration


import kotlinx.coroutines.future.await
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_LANGUAGE
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlock
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetLanguageCommand : AbstractCommand("command.setlanguage") {

    init {
        id = 2
        name = "setLanguage"
        aliases = arrayOf("setLang", "sl")
        children = arrayOf(
            ListArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }


    override suspend fun execute(context: CommandContext) {
        when (context.commandParts.size) {
            2 -> {
                sendCurrentLang(context)
            }
            3 -> {
                setLang(context)
            }
            else -> sendSyntax(context)
        }
    }

    private suspend fun sendCurrentLang(context: CommandContext) {
        val wrapper = context.daoManager.guildLanguageWrapper
        val lang = wrapper.languageCache.get(context.guildId).await()

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

        lang = if (shouldUnset) {
            ""
        } else {
            getEnumFromArgNMessage<Language>(context, 0, MESSAGE_UNKNOWN_LANGUAGE)?.toString() ?: return
        }

        val wrapper = context.daoManager.guildLanguageWrapper
        wrapper.setLanguage(context.guildId, lang)

        val possible = if (shouldUnset) "un" else ""

        val unReplacedMsg = context.getTranslation("$root.${possible}set.success")
        val msg = replaceLang(
            unReplacedMsg,
            lang
        )
        sendMsg(context, msg)
    }

    private fun replaceLang(msg: String, lang: String): String {
        return msg.replace("%language%", lang)
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
        }

        override suspend fun execute(context: CommandContext) {
            var msg = context.getTranslation("$root.title")
            msg += Language.values()
                .withIndex()
                .joinToString("\n", "```INI\n", "```") { (index, lang) ->
                    "$index - [$lang]"
                }
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