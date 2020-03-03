package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendPaginationMsg


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        sendPagination(context)
    }

    private suspend fun sendPagination(context: CommandContext) {
        val list = mutableListOf(
            "aaa",
            "bbb",
            "ccc"
        )

        sendPaginationMsg(context, list, 0)
    }

    private suspend fun ffmPegFlushed(context: CommandContext) = withContext(Dispatchers.IO) {

    }


//    suspend fun migrate(context: CommandContext) {
//
//        val daoManager = context.daoManager
//        val driverManager = context.daoManager.driverManager
//
//        val wrapper = daoManager.streamUrlWrapper
//        val rows = DataArray.empty()
//        val table = "stream_urls"
//        driverManager.executeMySQLQuery("SELECT * FROM $table", { rs ->
//
//            while (rs.next()) {
//                val obj = DataObject.empty()
//
//                obj.put("guildId", rs.getLong("guildId"))
//                obj.put("url", rs.getString("url"))
//                //obj.put("mode", rs.getString("mode"))
//
//                rows.add(obj)
//            }
//        })
//
//        for (i in 0 until rows.length()) {
//            val row = rows.getObject(i)
//
//            wrapper.setUrl(
//                row.getLong("guildId"),
//                row.getString("url")
//            )
//
//            context.logger.info("Migrating $table row ${i + 1}/${rows.length()}")
//        }
//        sendMsg(context, "Migrated $table")
//    }
}