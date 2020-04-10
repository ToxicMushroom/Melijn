package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.database.role.SelfRoleGroup
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val emoteMap = mutableMapOf<Long, Map<String, Long>>()
        val channelMap = mutableMapOf<Long, Long>()

        context.daoManager.driverManager.executeQuery("SELECT * FROM channels WHERE channelType = ?", { rs ->
            while (rs.next()) {
                channelMap[rs.getLong("guildId")] = rs.getLong("channelId")
            }
        }, "SELFROLE")
        println("collected channel")

        context.daoManager.driverManager.executeQuery("SELECT * FROM old_selfroles", { rs ->
            while (rs.next()) {
                val currentMap = emoteMap.getOrDefault(rs.getLong("guildId"), emptyMap()).toMutableMap()
                currentMap[rs.getString("emoteji")] = rs.getLong("roleId")
                emoteMap[rs.getLong("guildId")] = currentMap
            }
        })
        println("collected selfroles")

        for ((guildId, emoteMap) in emoteMap) {
            val srGroup = SelfRoleGroup("default", emptyList(), channelMap.getOrDefault(guildId, -1), true, false)
            context.daoManager.selfRoleGroupWrapper.insertOrUpdate(guildId, srGroup)

            for ((emoteji, roleId) in emoteMap) {
                context.daoManager.selfRoleWrapper.add(guildId, srGroup.groupName, emoteji, roleId)
            }
            println("working on $guildId")
        }

        sendMsg(context, "done")
    }
}