package me.melijn.melijnbot.objects.translation

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import java.util.*

class Translateable(private val path: String = "") {

    companion object {
        val defaultRecourseBundle: ResourceBundle = ResourceBundle.getBundle("strings", Locale.getDefault())
        val dutchBelgianRecourseBundle: ResourceBundle = ResourceBundle.getBundle("strings", Locale("nl_BE"))
    }


    fun string(ctx: CommandContext): String {
        return if (ctx.isFromGuild) {
            when (ctx.daoManager.guildLanguageWrapper.languageCache.get(ctx.guild.idLong).get()) {
                "nl_BE" -> dutchBelgianRecourseBundle.getString(path)
                else -> defaultRecourseBundle.getString(path)
            }
        } else {
            defaultRecourseBundle.getString(path)
        }
    }

    fun string(daoManager: DaoManager, userId: Long, guildId: Long = -1): String {
        return if (guildId > 0) {

            when (daoManager.guildLanguageWrapper.languageCache.get(guildId).get()) {
                "nl_BE" -> dutchBelgianRecourseBundle.getString(path)
                else -> defaultRecourseBundle.getString(path)
            }
        } else {
            defaultRecourseBundle.getString(path)
        }
    }
}

