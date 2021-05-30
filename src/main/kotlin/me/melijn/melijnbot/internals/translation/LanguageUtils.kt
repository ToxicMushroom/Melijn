package me.melijn.melijnbot.internals.translation

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.ICommandContext

suspend fun getLanguage(context: ICommandContext): String {
    val guildId = if (context.isFromGuild) context.guildId else -1L
    return getLanguage(context.daoManager, context.authorId, guildId)
}

suspend fun getLanguage(daoManager: DaoManager, userId: Long, guildId: Long = -1): String {
    //val isSupporter = daoManager.supporterWrapper.userSupporterIds.contains(userId)
    val isSupporter = true
    return if (guildId > 0) {
        if (isSupporter) {
            val userLang = getUserLanguage(daoManager, userId)
            if (userLang.isBlank()) {
                getGuildLanguageOrDefault(daoManager, guildId)
            } else {
                userLang
            }
        } else {
            getGuildLanguageOrDefault(daoManager, guildId)
        }
    } else {
        if (isSupporter) {
            getUserLanguageOrDefault(daoManager, userId)
        } else {
            DEFAULT_LANGUAGE
        }
    }

}

private suspend fun getGuildLanguageOrDefault(daoManager: DaoManager, guildId: Long): String {
    val guildLanguage = getGuildLanguage(daoManager, guildId)
    return if (guildLanguage.isBlank()) {
        DEFAULT_LANGUAGE
    } else {
        guildLanguage
    }
}

private suspend fun getUserLanguageOrDefault(daoManager: DaoManager, userId: Long): String {
    val userLanguage = getUserLanguage(daoManager, userId)
    return if (userLanguage.isBlank()) {
        DEFAULT_LANGUAGE
    } else {
        userLanguage
    }
}

private suspend fun getUserLanguage(daoManager: DaoManager, userId: Long): String {
    return daoManager.userLanguageWrapper.getLanguage(userId)
}

private suspend fun getGuildLanguage(daoManager: DaoManager, guildId: Long): String {
    return daoManager.guildLanguageWrapper.getLanguage(guildId)
}

val DEFAULT_LANGUAGE = "EN"