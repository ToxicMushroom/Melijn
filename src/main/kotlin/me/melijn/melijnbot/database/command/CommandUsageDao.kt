package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager
import java.time.LocalTime
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class CommandUsageDao(driverManager: DriverManager) : Dao(driverManager) {

    private var currentHour: Int = 25
    private var hourMillis: Long = 0

    override val table: String = "commandUsage"
    override val tableStructure: String = "commandId int, time bigint, usageCount bigint"
    override val primaryKey: String = "commandId, time"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    suspend fun addUse(commandId: Int, currentTimeMillis: Long = System.currentTimeMillis()) {
        if (currentHour != LocalTime.now().hour) {
            currentHour = LocalTime.now().hour
            hourMillis = currentTimeMillis
        }
        driverManager.executeUpdate("INSERT INTO $table (commandId, time, usageCount) VALUES (?, ?, ?) ON CONFLICT ($primaryKey) DO UPDATE SET usageCount = $table.usageCount + 1",
            commandId, hourMillis, 1)
    }


    suspend fun getUsageWithinPeriod(from: Long, until: Long): LinkedHashMap<Int, Long> = suspendCoroutine {
        driverManager.executeQuery("SELECT * FROM $table WHERE time > ? AND time < ?", { rs ->
            val commandUsages = LinkedHashMap<Int, Long>()
            while (rs.next()) {
                val commandId = rs.getInt("commandId")
                val usageCount = rs.getLong("usageCount")

                val baseValue = commandUsages[commandId] ?: 0
                commandUsages[commandId] = baseValue + usageCount
            }
            it.resume(commandUsages)
        }, from, until)


    }
}