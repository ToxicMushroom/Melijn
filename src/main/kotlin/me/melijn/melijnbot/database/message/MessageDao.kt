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
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "messages"
    override val tableStructure: String = "guildId bigint, type varchar(32), message varchar(4096)"
    override val keys: String = "UNIQUE KEY(guildId, type)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

    suspend fun set(guildId: Long, type: MessageType, message: String) {
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, message) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE message = ?",
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
        val json = JSONObject()
        messageContent?.let { json.put("content", it) }
        embed?.let { membed ->
            json.put("embed", JSONObject(membed.toData()
                .put("type", EmbedType.RICH)
                .toString()))
        }

        val attachmentsJson = JSONArray()
        for (attachment in attachments) {
            attachmentsJson.put(
                JSONObject()
                    .put("url", attachment.key)
                    .put("file", attachment.value)
            )
        }
        json.put("attachments", attachmentsJson)
        return json.toString(4)
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
                val jsonObj = JSONObject(json)
                var content: String? = null
                if (jsonObj.has("content")) {
                    content = jsonObj.getString("content")
                }
                var embed: MessageEmbed? = null
                if (jsonObj.has("embed")) {
                    val jdaImpl = (MelijnBot.shardManager?.shards?.get(0) as JDAImpl)
                    val embedString = jsonObj.getJSONObject("embed")
                    val dataObject = DataObject.fromJson(embedString.toString(4))
                    embed = jdaImpl.entityBuilder.createMessageEmbed(dataObject)
                }
                val attachments = mutableMapOf<String, String>()
                if (jsonObj.has("attachments")) {
                    val attachmentsJson = jsonObj.getJSONArray("attachments")

                    for (i in 0 until attachmentsJson.length()) {
                        val attachmentObj = attachmentsJson.getJSONObject(i)
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