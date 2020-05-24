package me.melijn.melijnbot.database.settings

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class PrivateAllowSpacedPrefixDao(driverManager: DriverManager) : Dao(driverManager) {

    override val table: String = "privateAllowSpacedPrefixStates"
    override val tableStructure: String = "userId bigint, triState boolean"
    override val primaryKey: String = "userId"

    init {
        driverManager.registerTable(table, tableStructure, primaryKey)
    }

    fun getState
}