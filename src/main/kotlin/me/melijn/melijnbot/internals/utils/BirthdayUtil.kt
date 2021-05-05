package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.jagtag.BirthdayJagTagParser
import me.melijn.melijnbot.internals.jagtag.BirthdayParserArgs
import me.melijn.melijnbot.internals.models.ModularMessage
import net.dv8tion.jda.api.entities.Member

object BirthdayUtil {
    suspend fun replaceVariablesInBirthdayMessage(
        daoManager: DaoManager,
        member: Member,
        modularMessage: ModularMessage,
        birthYear: Int?,
        msgName: String
    ): ModularMessage {
        val args = BirthdayParserArgs(daoManager, member, birthYear)
        return modularMessage.mapAllStringFieldsSafe("BirthdayMessage(msgName=$msgName)") {
            if (it != null) {
                BirthdayJagTagParser.parseJagTag(args, it)
            } else {
                null
            }
        }
    }
}