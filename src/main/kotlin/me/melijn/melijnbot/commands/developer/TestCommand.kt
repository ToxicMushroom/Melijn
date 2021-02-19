package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.database.audio.GainProfile
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.message.sendRsp
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    override suspend fun execute(context: ICommandContext) {
        val gps = mutableMapOf<Long, Map<String, GainProfile>>()

        context.daoManager.driverManager.executeQuery("SELECT * FROM gainProfiles", { rs ->
            while (rs.next()) {
                val map = gps[rs.getLong("id")]?.toMutableMap() ?: mutableMapOf()
                map[rs.getString("name")] = GainProfile.fromString(rs.getString("profile"))
                gps[rs.getLong("id")] = map
            }
        })

        sendRsp(context, "Collected ${gps.size} gainprofiles")

        val jobs = mutableListOf<Job>()
        for ((id, map) in gps) {
            for ((name, profile) in map) {
                jobs.add(TaskManager.async {
                    context.daoManager.gainProfileWrapper.add(id, name, profile.toFloatArray())
                })
            }
        }
        jobs.joinAll()
        sendRsp(context, "Migrating ${gps.size} completed")
    }
}
