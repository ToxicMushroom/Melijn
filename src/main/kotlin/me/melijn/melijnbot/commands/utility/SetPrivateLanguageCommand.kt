package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.commands.administration.SetLanguageCommand
import me.melijn.melijnbot.enums.Language
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_LANGUAGE
import me.melijn.melijnbot.internals.utils.getEnumFromArgNMessage
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.withSafeVariable

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

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendCurrentLang(context)
        } else {
            setLang(context)
        }
    }

    private suspend fun sendCurrentLang(context: ICommandContext) {
        val dao = context.daoManager.userLanguageWrapper
        val lang = dao.getLanguage(context.authorId)

        val msg = context.getTranslation(
            if (lang.isBlank()) {
                "$root.show.unset"
            } else {
                "$root.show.set"
            }
        ).withSafeVariable("language", lang)

        sendRsp(context, msg)
    }

    private suspend fun setLang(context: ICommandContext) {
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
            .withSafeVariable("language", language.toString())

        sendRsp(context, msg)
    }
}