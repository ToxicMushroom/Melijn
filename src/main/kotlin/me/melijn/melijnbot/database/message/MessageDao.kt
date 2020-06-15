package me.melijn.melijnbot.database.message

import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.MessageType
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Message.MentionType
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import java.time.Instant
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
        driverManager.executeUpdate("INSERT INTO $table (guildId, type, message) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET message = ?",
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
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND type = ?",
            guildId, type.toString())
    }
}

data class ModularMessage(
    var messageContent: String? = null,
    var embed: MessageEmbed? = null,
    var attachments: Map<String, String> = emptyMap(),
    var extra: Map<String, String> = emptyMap()
) {

    fun toJSON(): String {
        val json = DataObject.empty()
        messageContent?.let { json.put("content", it) }
        embed?.let { membed ->
            json.put("embed", membed.toData()
                .put("type", EmbedType.RICH)
            )
        }

        val attachmentsJson = DataArray.empty()
        for ((key, value) in attachments) {
            attachmentsJson.add(DataObject.empty()
                .put("url", key)
                .put("file", value))
        }
        json.put("attachments", attachmentsJson)

        val extraJson = DataArray.empty()
        for ((key, value) in extra) {
            extraJson.add(DataArray.empty()
                .add(key)
                .add(value)
            )
        }
        json.put("extra", extraJson)
        return json.toString()
    }

    fun toMessage(): Message? {
        var membed = embed
        if (messageContent == null && (membed == null || membed.isEmpty || !membed.isSendable) && attachments.isEmpty()) return null

        // Timestamp handler
        if (membed != null && extra.containsKey("currentTimestamp")) {
            val eb = EmbedBuilder(membed)
            eb.setTimestamp(Instant.now())
            membed = eb.build()
        }

        val mb = MessageBuilder()
            .setEmbed(membed)
            .setContent(messageContent)

        // Timestamp handler
        if (extra.containsKey("isPingable")) {
            mb.setAllowedMentions(MentionType.values().toSet())
        } else {
            mb.setAllowedMentions(emptyList())
        }

        return mb.build()
    }

    companion object {
        fun fromJSON(json: String): ModularMessage {
            try {
                val jsonObj = DataObject.fromJson(json)

                // Just text
                var content: String? = null
                if (jsonObj.hasKey("content")) {
                    content = jsonObj.getString("content")
                }

                // Embed
                var embed: MessageEmbed? = null
                if (jsonObj.hasKey("embed")) {
                    val jdaImpl = (MelijnBot.shardManager.shards[0] as JDAImpl)
                    val embedString = jsonObj.getObject("embed")
                    val dataObject = DataObject.fromJson(embedString.toString())
                    embed = jdaImpl.entityBuilder.createMessageEmbed(dataObject)
                }

                // Attachments
                val attachments = mutableMapOf<String, String>()
                if (jsonObj.hasKey("attachments")) {
                    val attachmentsJson = jsonObj.getArray("attachments")

                    for (i in 0 until attachmentsJson.length()) {
                        val attachmentObj = attachmentsJson.getObject(i)
                        attachments[attachmentObj.getString("url")] = attachmentObj.getString("file")
                    }
                }

                // Extra data
                val extra = mutableMapOf<String, String>()
                if (jsonObj.hasKey("extra")) {
                    val extraJson = jsonObj.getArray("extra")
                    for (i in 0 until extraJson.length()) {
                        val extraObj = extraJson.getArray(i)
                        extra[extraObj.getString(0)] = extraObj.getString(1)
                    }
                }
                return ModularMessage(content, embed, attachments, extra)
            } catch (e: Exception) {
                e.printStackTrace()
                throw IllegalArgumentException("Invalid JSON structure")
            }
        }
    }
}