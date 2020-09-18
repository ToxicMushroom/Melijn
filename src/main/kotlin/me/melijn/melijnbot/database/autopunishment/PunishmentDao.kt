package me.melijn.melijnbot.database.autopunishment

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.enums.PunishmentType
import net.dv8tion.jda.api.utils.data.DataObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class PunishmentDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "punishments"
    override val tableStructure: String = "guildId bigint, name varchar(64), punishmentType varchar(64), extraMap varchar(512), reason varchar(2000)"
    override val primaryKey: String = "guildId, name"

    override val cacheName: String = "punishment"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun put(guildId: Long, punishment: Punishment) {
        val sql = "INSERT INTO $table (guildId, name, punishmentType, extraMap, reason) VALUES (?, ?, ?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET punishmentType = ?, extraMap = ?, reason = ?"
        punishment.apply {
            driverManager.executeUpdate(sql, guildId, name, punishmentType.toString(), extraMap.toString(), reason, punishmentType.toString(), extraMap.toString(), reason)
        }
    }

    fun remove(guildId: Long, name: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND name = ?",
            guildId, name)
    }

    suspend fun get(guildId: Long): List<Punishment> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            val list = mutableListOf<Punishment>()
            while (rs.next()) {
                list.add(Punishment(
                    rs.getString("name"),
                    PunishmentType.valueOf(rs.getString("punishmentType")),
                    DataObject.fromJson(rs.getString("extraMap")),
                    rs.getString("reason")
                ))
            }
            it.resume(list)
        }, guildId)
    }

    suspend fun get(guildId: Long, punishmentType: String): List<Punishment> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ? AND punishmentType = ?", { rs ->
            val list = mutableListOf<Punishment>()
            while (rs.next()) {
                list.add(Punishment(
                    rs.getString("name"),
                    PunishmentType.valueOf(punishmentType),
                    DataObject.fromJson(rs.getString("extraMap")),
                    rs.getString("reason")
                ))
            }
            it.resume(list)
        }, guildId, punishmentType)
    }
}

data class Punishment(
    val name: String,
    val punishmentType: PunishmentType,
    @JsonDeserialize(using = DataObjectDeserializer::class)
    @JsonSerialize(using = DataObjectSerializer::class)
    var extraMap: DataObject,
    var reason: String
)

class DataObjectSerializer : StdSerializer<DataObject>(DataObject::class.java) {

    override fun serialize(value: DataObject, gen: JsonGenerator, provider: SerializerProvider) {
        gen.writeString(value.toString())
    }

}

class DataObjectDeserializer : StdDeserializer<DataObject>(DataObject::class.java) {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DataObject {
        return DataObject.fromJson(p.text)
    }
}
