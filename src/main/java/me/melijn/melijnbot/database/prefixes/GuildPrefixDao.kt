package me.melijn.melijnbot.database.prefixes

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class GuildPrefixDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "guildPrefixes"
    override val tableStructure: String = "guildId bigInt, prefix varchar(32)"
    override val keys: String = "PRIMARY KEY(guildId)"
}