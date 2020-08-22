package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager
import net.dv8tion.jda.api.utils.data.DataArray
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SelfRoleDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "selfRoles"
    override val tableStructure: String = "guildId bigint, groupName varchar(64), emotejiInfo varchar(4096)"
    override val primaryKey: String = "guildId, groupName"

    override val cacheName: String = "roles:self"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun set(guildId: Long, groupName: String, emotejiInfo: String) {
        /* exampl roleInfo
        [
            [
                "♂️", //emoteji
                "male", //name
                [[100, 16516516541654], [100, 4646848479874]], // <chance, roleId> list
                true //assignAllRoles
            ]
        ]
         */

        driverManager.executeUpdate("INSERT INTO $table (guildId, groupName, emotejiInfo) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET emotejiInfo = ?",
            guildId, groupName, emotejiInfo, emotejiInfo)
    }

    fun clear(guildId: Long, groupName: String) {
        driverManager.executeUpdate("DELETE FROM $table WHERE guildId = ? AND groupName = ?",
            guildId, groupName)
    }

    suspend fun getMap(guildId: Long): Map<String, DataArray> = suspendCoroutine {
        val map = mutableMapOf<String, DataArray>()
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            while (rs.next()) {
                val group = rs.getString("groupName")
                val dataObject = DataArray.fromJson(rs.getString("emotejiInfo"))
                map[group] = dataObject
            }
        }, guildId)
        it.resume(map)
    }
}