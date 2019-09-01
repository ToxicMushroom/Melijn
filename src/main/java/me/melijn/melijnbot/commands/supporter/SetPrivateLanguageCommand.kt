package me.melijn.melijnbot.commands.supporter

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commands.administration.SetLanguageCommand
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax

class SetPrivateLanguageCommand : AbstractCommand("command.setprivatelanguage") {

    init {
        id = 5
        name = "setPrivateLanguage"
        syntax = Translateable("$root.syntax")
        aliases = arrayOf("spl", "setPrivateLang", "setPrivLang")
        description = Translateable("$root.description")
        commandCategory = CommandCategory.SUPPORTER
        children = arrayOf(SetLanguageCommand.ListCommand())
    }

    override suspend fun execute(context: CommandContext) {
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

    private suspend fun sendCurrentLang(context: CommandContext) {
        val dao = context.daoManager.userLanguageWrapper
        val lang = dao.languageCache.get(context.authorId).await()

        if (lang.isBlank()) {
            sendMsg(context, Translateable("$root.unset.currentlangresponse").string(context))
        } else
            sendMsg(context, replaceLang(
                    Translateable("$root.currentlangresponse").string(context),
                    lang
            ))
    }

    private suspend fun setLang(context: CommandContext) {
        val lang: String
        val shouldUnset = "null".equals(context.commandParts[2], true)
        try {
            lang = if (shouldUnset) ""
            else Language.valueOf(context.commandParts[2].toUpperCase()).toString()
        } catch (ignored: IllegalArgumentException) {
            val msg = replaceArg(
                    Translateable("$root.set.invalidarg").string(context),
                    context.commandParts)

            sendMsg(context, msg)
            return
        }


        val dao = context.daoManager.userLanguageWrapper
        dao.setLanguage(context.authorId, lang)


        val possible = if (shouldUnset) "un" else ""
        sendMsg(context, replaceLang(
                Translateable("$root.${possible}set.success").string(context),
                lang
        ))
    }

    private fun replaceArg(msg: String, commandParts: List<String>): String = msg
            .replace("%argument%", commandParts[2])
            .replace("%prefix%", commandParts[0])

    private fun replaceLang(msg: String, lang: String): String = msg
            .replace("%language%", lang)

}