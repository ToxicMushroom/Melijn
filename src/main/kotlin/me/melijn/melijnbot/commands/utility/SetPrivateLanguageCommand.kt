package me.melijn.melijnbot.commands.utility

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.commands.administration.SetLanguageCommand
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_LANGUAGE
import me.melijn.melijnbot.objects.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.objects.utils.sendMsg

class SetPrivateLanguageCommand : AbstractCommand("command.setprivatelanguage") {

    init {
        id = 5
        name = "setPrivateLanguage"
        aliases = arrayOf("spl", "setPrivateLang", "setPrivLang")
        children = arrayOf(
            SetLanguageCommand.ListArg("command.setlanguage")
        )
        runConditions = arrayOf(RunCondition.USER_SUPPORTER)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendCurrentLang(context)
        } else {
            setLang(context)
        }
    }

    private suspend fun sendCurrentLang(context: CommandContext) {
        val dao = context.daoManager.userLanguageWrapper
        val lang = dao.languageCache.get(context.authorId).await()

        val msg = context.getTranslation(
            if (lang.isBlank()) {
                "$root.show.unset"
            } else {
                "$root.show.set"
            }
        ).replace("%language%", lang)

        sendMsg(context, msg)
    }

    private suspend fun setLang(context: CommandContext) {
        val shouldUnset = "null".equals(context.commandParts[2], true)

        val language = if (shouldUnset) {
            null
        } else {
            getEnumFromArgNMessage<Language>(context, 0, MESSAGE_UNKNOWN_LANGUAGE) ?: return
        }


        val dao = context.daoManager.userLanguageWrapper
        if (language == null) {
            dao.removeLanguage(context.authorId)
        } else {
            dao.setLanguage(context.authorId, language.toString())
        }

        val possible = if (shouldUnset) {
            "un"
        } else {
            ""
        }

        val msg = context.getTranslation("$root.${possible}set")
            .replace("%language%", language.toString())

        sendMsg(context, msg)
    }
}