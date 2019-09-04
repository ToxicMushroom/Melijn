package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.MessageType
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
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

    fun set(guildId: Long, type: MessageType, message: String) {
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

    fun remove(guildId: Long, type: MessageType) {
        driverManager.executeUpdate("REMOVE FROM $table WHERE guildId = ? AND type = ?",
                guildId, type.toString())
    }
}

data class ModularMessage(var messageContent: String? = null,
                          var embed: MessageEmbed? = null,
                          var attachments: List<String> = emptyList()) {
    fun toJSON(): String {
        val json = JSONObject()
        messageContent?.let { json.put("content", it) }
        embed?.let { membed ->
            json.put("embed", membed.toData().toString())
        }

        json.put("attachments", attachments.joinToString("%SPLIT%"))
        return json.toString(4)
    }

    companion object {
        fun fromJSON(jda: JDA, json: String): ModularMessage {
            try {
                val jsonObj = JSONObject(json)
                var content: String? = null
                if (jsonObj.has("content")) {
                    content = jsonObj.getString("content")
                }
                var embed: MessageEmbed? = null
                if (jsonObj.has("embed")) {
                    val jdaImpl = (jda as JDAImpl)
                    val embedString = jsonObj.getString("embed")
                    val dataObject = DataObject.fromJson(embedString)
                    embed = jdaImpl.entityBuilder.createMessageEmbed(dataObject)
                }
                val attachments = mutableListOf<String>()
                if (jsonObj.has("attachments")) {
                    val attachmentsString = jsonObj.getString("attachments")
                    val attachmentStrings = attachmentsString.split("%SPLIT%")
                    attachments.addAll(attachmentStrings)
                }
                return ModularMessage(content, embed, attachments)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid JSON structure")
            }
        }
    }
}