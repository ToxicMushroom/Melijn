package me.melijn.melijnbot.database.locking

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class LockExcludedDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val cacheName: String = "lockExcludeDao"
    override val table: String = "lock_excludes"
    override val tableStructure: String = "guild_id bigint, entity_id bigint, entity_type smallint"
    override val primaryKey: String = "guild_id, entity_type"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    private val excludeQuery = "INSERT INTO $table (guild_id, entity_id, entity_type) VALUES (?, ?, ?) " +
        "ON CONFLICT DO NOTHING"
    private val includeQuery = "DELETE FROM $table WHERE guild_id = ? AND entity_id = ? AND entity_type = ?"

    suspend fun getExcluded(guildId: Long, entityType: EntityType): List<Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guild_id = ? AND entity_type = ?", { rs ->
            val list = mutableListOf<Long>()

            while (rs.next()) {
                list.add(rs.getLong("entity_id"))
            }

            it.resume(list)
        }, guildId, entityType.id)
    }

    fun exclude(guildId: Long, entityType: EntityType, entity: Long) {

        driverManager.executeUpdate(
            excludeQuery,
            guildId, entity, entityType.id
        )
    }

    fun include(guildId: Long, entityType: EntityType, entity: Long) {
        driverManager.executeUpdate(
            includeQuery,
            guildId, entity, entityType.id
        )
    }

    fun excludeBulk(guildId: Long, entityType: EntityType, entities: List<Long>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement(excludeQuery).use {
                it.setLong(1, guildId)
                it.setByte(3, entityType.id)
                for (entity in entities) {
                    it.setLong(2, entity)
                    it.addBatch()
                }
                it.executeBatch()
            }
        }
    }

    fun includeBulk(guildId: Long, entityType: EntityType, entities: List<Long>) {
        driverManager.getUsableConnection { con ->
            con.prepareStatement(includeQuery).use {
                it.setLong(1, guildId)
                it.setByte(3, entityType.id)
                for (entity in entities) {
                    it.setLong(2, entity)
                    it.addBatch()
                }
                it.executeBatch()
            }
        }
    }
}