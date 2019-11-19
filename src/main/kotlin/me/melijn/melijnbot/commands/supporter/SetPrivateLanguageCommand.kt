package me.melijn.melijnbot.commands.supporter

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commands.administration.SetLanguageCommand
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetPrivateLanguageCommand : AbstractCommand("command.setprivatelanguage") {

    init {
        id = 5
        name = "setPrivateLanguage"
        aliases = arrayOf("spl", "setPrivateLang", "setPrivLang")
        children = arrayOf(SetLanguageCommand.ListArg())
        commandCategory = CommandCategory.SUPPORTER
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
        val dao = context.daoManager.userLanguageWrapper
        val lang = dao.languageCache.get(context.authorId).await()

        val language = context.getLanguage()
        val msg = if (lang.isBlank()) {
            i18n.getTranslation(language, "$root.unset.currentlangresponse")
        } else {
            val unReplaced = i18n.getTranslation(language, "$root.currentlangresponse")
            replaceLang(unReplaced, lang)
        }

        sendMsg(context, msg)
    }

    private suspend fun setLang(context: CommandContext) {
        val shouldUnset = "null".equals(context.commandParts[2], true)
        val language = context.getLanguage()

        val lang: String = try {
            if (shouldUnset) {
                ""
            } else {
                Language.valueOf(context.commandParts[2].toUpperCase()).toString()
            }
        } catch (ignored: IllegalArgumentException) {
            val unReplacedMsg = i18n.getTranslation(language, "$root.set.invalidarg")

            val msg = replaceArg(unReplacedMsg, context.commandParts)

            sendMsg(context, msg)
            return
        }

        val dao = context.daoManager.userLanguageWrapper
        dao.setLanguage(context.authorId, lang)

        val possible = if (shouldUnset) "un" else ""
        val unReplacedMsg = i18n.getTranslation(language, "$root.${possible}set.success")

        val msg = replaceLang(unReplacedMsg, lang)

        sendMsg(context, msg)
    }

    private fun replaceArg(msg: String, commandParts: List<String>): String = msg
        .replace("%argument%", commandParts[2])
        .replace("%prefix%", commandParts[0])

    private fun replaceLang(msg: String, lang: String): String = msg
        .replace("%language%", lang)

}