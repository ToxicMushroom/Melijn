package me.melijn.melijnbot.database

abstract class Dao(val driverManager: DriverManager) {

    abstract val table: String
    abstract val tableStructure: String
    abstract val primaryKey: String
    open val uniqueKey: String = ""

    fun clear() {
        driverManager.clear(table)
    }
}