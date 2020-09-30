package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.internals.jagtag.BirthdayJagTagParser
import me.melijn.melijnbot.internals.jagtag.BirthdayParserArgs
import net.dv8tion.jda.api.entities.Member

object BirthdayUtil {
    suspend fun replaceVariablesInBirthdayMessage(daoManager: DaoManager, member: Member, modularMessage: ModularMessage, birthYear: Int?): ModularMessage {
        val args = BirthdayParserArgs(daoManager, member, birthYear)
        return modularMessage.mapAllStringFields {
            if (it != null) {
                BirthdayJagTagParser.parseJagTag(args, it)
            } else {
                null
            }
        }
    }
}