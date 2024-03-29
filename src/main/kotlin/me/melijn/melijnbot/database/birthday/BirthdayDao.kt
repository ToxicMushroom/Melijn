package me.melijn.melijnbot.database.birthday

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class BirthdayDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "birthdays"
    private val tableT = "timeZones"
    override val tableStructure: String = "userId bigint, birthday int, birthyear int, startTime int"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun getBirthdays(day: Int): Map<Long, BirthdayInfo> = suspendCoroutine {
        val prevDay = if (day == 1) 365 else day - 1
        val nextDay = if (day == 365) 1 else day + 1
        driverManager.executeQuery(
            "SELECT *, $table.startTime FROM $table LEFT JOIN $tableT ON $table.userId = $tableT.id WHERE ($table.birthday = ? OR $table.birthday = ? OR $table.birthday = ?)",
            { rs ->
                val map = mutableMapOf<Long, BirthdayInfo>()
                while (rs.next()) {
                    map[rs.getLong("userId")] = BirthdayInfo(
                        rs.getInt("birthyear"),
                        rs.getInt("birthday"),
                        0.0,
                        rs.getString("zoneId")
                    )
                }
                it.resume(map)
            },
            day,
            prevDay,
            nextDay
        )
    }

    suspend fun get(userId: Long): Pair<Int, Int>? = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE userId = ?", { rs ->
            if (rs.next()) {
                it.resume(Pair(rs.getInt("birthday"), rs.getInt("birthyear")))
            } else {
                it.resume(null)
            }
        }, userId)
    }

    fun set(userId: Long, birthday: Int, birthyear: Int) {
        driverManager.executeUpdate(
            "INSERT INTO $table (userId, birthday, birthyear) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET birthday = ?, birthyear = ?",
            userId, birthday, birthyear, birthday, birthyear
        )
    }

    fun remove(userId: Long) {
        driverManager.executeUpdate("DELETE FROM $table WHERE userId = ?", userId)
    }

}

data class BirthdayInfo(
    val birthYear: Int,
    var birthday: Int,
    var startMinute: Double,
    val zoneId: String?
)