package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.message.ModularMessage
import me.melijn.melijnbot.internals.jagtag.BirthdayJagTagParser
import me.melijn.melijnbot.internals.jagtag.BirthdayParserArgs
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl

object BirthdayUtil {
    suspend fun replaceVariablesInBirthdayMessage(daoManager: DaoManager, member: Member, modularMessage: ModularMessage, birthYear: Int?): ModularMessage {
        val newMessage = ModularMessage()

        val args = BirthdayParserArgs(daoManager, member, birthYear)
        newMessage.messageContent = modularMessage.messageContent?.let {
            BirthdayJagTagParser.parseJagTag(args, it)
        }

        val oldEmbedData = modularMessage.embed?.toData()
            ?.put("type", EmbedType.RICH)
        if (oldEmbedData != null) {
            val newEmbedJSON = BirthdayJagTagParser.parseJagTag(args, oldEmbedData.toString())
            val newEmbedData = DataObject.fromJson(newEmbedJSON)
            val newEmbed = (member.jda as JDAImpl).entityBuilder.createMessageEmbed(newEmbedData)
            newMessage.embed = newEmbed
        }


        val newAttachments = mutableMapOf<String, String>()
        modularMessage.attachments.forEach { (t, u) ->
            val url = BirthdayJagTagParser.parseJagTag(args, t)
            val file = BirthdayJagTagParser.parseJagTag(args, u)
            newAttachments[url] = file
        }
        newMessage.attachments = newAttachments
        return newMessage

    }
}