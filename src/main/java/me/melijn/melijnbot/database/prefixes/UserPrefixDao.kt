package me.melijn.melijnbot.database.prefixes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class UserPrefixDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "userPrefixes"
    override val tableStructure: String = ""
    override val keys: String = "PRIMARY KEY userId"
}