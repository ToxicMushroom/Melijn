package me.melijn.melijnbot.database.filter

import me.melijn.melijnbot.database.Dao
import me.melijn.melijnbot.database.DriverManager

class FilterWrappingModeDao(driverManager: DriverManager) : Dao(driverManager) {
    override val table: String = "filterWrappingModes"
    override val tableStructure: String = "guildId bigint, channelId bigint, mode varchar(64)"
    override val primaryKey: String
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}