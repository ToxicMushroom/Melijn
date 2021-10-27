package me.melijn.melijnbot.database.verification

import me.melijn.melijnbot.database.CacheDBDao
import me.melijn.melijnbot.database.DriverManager

class VerificationSettingsDao(driverManager: DriverManager) : CacheDBDao(driverManager) {

    override val table: String = "verification_settings"
    override val tableStructure: String = "guild_id long, type verification_type"
    override val primaryKey: String = "guild_id"
    override val cacheName: String = table

    init {
//        driverManager.addQuery("CREATE ENUM verification_type VALUES ")
        driverManager.registerTable(table, tableStructure, primaryKey, uniqueKey)
    }


}