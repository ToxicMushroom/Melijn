package me.melijn.melijnbot.database.translations

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class TranslationDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "translations"
    override val tableStructure: String = "language varchar(32), stringPath varchar(256), translation varchar(2048)"
    override val keys: String = "UNIQUE KEY(language, stringPath)"

    init {
        driverManager.registerTable(table, tableStructure, keys)
    }

}