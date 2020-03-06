package me.melijn.melijnbot.commands.developer

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.database.TestDaoManager
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.sendPaginationMsg
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import org.slf4j.LoggerFactory
import java.io.File


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

    private val logger = LoggerFactory.getLogger(TestCommand::class.qualifiedName)

    suspend fun migrate(dao: TestDaoManager) {
        val driverManager = dao.driverManager

        val wrapper = dao.banWrapper
        val rows = DataArray.empty()
        val table = "bans_old"
        driverManager.executeQuery("SELECT * FROM $table", { rs ->

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

            wrapper.setBan(Ban(
                row.getLong("guildId"),
                row.getLong("bannedId"),
                row.getLong("banAuthorId"),
                row.getString("reason", "/"),
                row.getLong("unbanAuthorId"),
                row.getString("unbanReason")
            )
            )

            logger.info("Migrating $table row ${i + 1}/${rows.length()}")
        }
        logger.info("Migrated $table")

    }
}


fun main(args: Array<String>) {
    var settings: Settings = ObjectMapper().readValue(File("config.json"), Settings::class.java)
    val taskManager = TaskManager()
    val testDaoManager = TestDaoManager(taskManager, settings.database)
    migrate(testDaoManager)
}