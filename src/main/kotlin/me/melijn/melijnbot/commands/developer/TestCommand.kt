package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        migrate(context)
    }

    suspend fun migrate(context: CommandContext) {

        val daoManager = context.daoManager
        val driverManager = context.daoManager.driverManager

        val wrapper = daoManager.streamUrlWrapper
        val rows = DataArray.empty()
        val table = "stream_urls"
        driverManager.executeMySQLQuery("SELECT * FROM $table", { rs ->

            while (rs.next()) {
                val obj = DataObject.empty()

                obj.put("guildId", rs.getLong("guildId"))
                obj.put("url", rs.getString("url"))
                //obj.put("mode", rs.getString("mode"))

                rows.add(obj)
            }
        })

        for (i in 0 until rows.length()) {
            val row = rows.getObject(i)

            wrapper.setUrl(
                row.getLong("guildId"),
                row.getString("url")
            )

            context.logger.info("Migrating $table row ${i + 1}/${rows.length()}")
        }
        sendMsg(context, "Migrated $table")
    }
}