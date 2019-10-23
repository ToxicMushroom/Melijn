package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.MessageType
import net.dv8tion.jda.api.AccountType
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "messages"
    override val tableStructure: String = "guildId bigint, type varchar(32), message varchar(4096)"
    override val primaryKey: String = "guildId, type"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun set(guildId: Long, type: MessageType, message: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, message) VALUES (?, ?, ?) ON CONFLICT $primaryKey DO UPDATE SET message = ?",
            guildId, type.toString(), message, message)
    }

    suspend fun get(guildId: Long, type: MessageType): String? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND type = ?", { rs ->
            if (rs.next()) {
                it.resume(rs.getString("message"))
            } else it.resume(null)
        }, guildId, type.toString())
    }

    suspend fun remove(guildId: Long, type: MessageType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND type = ?",
            guildId, type.toString())
    }
}

data class ModularMessage(var messageContent: String? = null,
                          var embed: MessageEmbed? = null,
                          var attachments: Map<String, String> = emptyMap()) {

    fun toJSON(): String {
        val json = DataObject.empty()
        messageContent?.let { json.put("content", it) }
        embed?.let { membed ->
            json.put("embed", membed.toData()
                .put("type", EmbedType.RICH)
                .toString())
        }

        val attachmentsJson = DataArray.empty()
        for (attachment in attachments) {
            attachmentsJson.add(DataObject.empty()
                .put("url", attachment.key)
                .put("file", attachment.value))
        }
        json.put("attachments", attachmentsJson)
        return json.toString()
    }

    fun toMessage(): Message? {
        val embed = embed
        if (messageContent == null && (embed == null || embed.isEmpty || !embed.isSendable(AccountType.BOT)) && attachments.isEmpty()) return null

        val mb = MessageBuilder()
            .setEmbed(embed)
            .setContent(messageContent)

        return mb.build()
    }

    companion object {
        fun fromJSON(json: String): ModularMessage {
            try {
                val jsonObj = DataObject.fromJson(json)
                var content: String? = null
                if (jsonObj.hasKey("content")) {
                    content = jsonObj.getString("content")
                }
                var embed: MessageEmbed? = null
                if (jsonObj.hasKey("embed")) {
                    val jdaImpl = (MelijnBot.shardManager?.shards?.get(0) as JDAImpl)
                    val embedString = jsonObj.getObject("embed")
                    val dataObject = DataObject.fromJson(embedString.toString())
                    embed = jdaImpl.entityBuilder.createMessageEmbed(dataObject)
                }
                val attachments = mutableMapOf<String, String>()
                if (jsonObj.hasKey("attachments")) {
                    val attachmentsJson = jsonObj.getArray("attachments")

                    for (i in 0 until attachmentsJson.length()) {
                        val attachmentObj = attachmentsJson.getObject(i)
                        attachments[attachmentObj.getString("url")] = attachmentObj.getString("file")
                    }
                }
                return ModularMessage(content, embed, attachments)
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalArgumentException("Invalid JSON structure")
            }
        }
    }
}