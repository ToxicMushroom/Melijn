package me.melijn.melijnbot.database.birthday

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BirthdayDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "birthday"
    override val tableStructure: String = "userId bigint, birthday int, birthyear int"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getBirthdays(time: Int): Map<Long, Int> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE birthday = ?", { rs ->
            val map = mutableMapOf<Long, Int>()
            while (rs.next()) {
                map[rs.getLong("userId")] = rs.getInt("birthyear")
            }
            it.resume(map)
        })
    }

    suspend fun get(userId: Long): Pair<Int, Int>? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE guildId = ?", { rs ->
            if (rs.next()) {
                it.resume(Pair(rs.getInt("birthday"), rs.getInt("birthyear")))
            } else {
                it.resume(null)
            }
        }, userId)
    }

    suspend fun set(userId: Long, birthday: Int, birthyear: Int) {
        driverManager.executeUpdate("INSERT INTO $table (userId, birthday,birthyear) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET birthday = ? ,birthyear = ?",
                userId, birthday, birthyear, birthday, birthyear)
    }

    suspend fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }
}
