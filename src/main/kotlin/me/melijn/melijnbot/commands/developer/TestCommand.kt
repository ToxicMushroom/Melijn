package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
//        val driverManager = context.daoManager.driverManager
//
//        var vccounter = 0
//        val vpw = context.daoManager.verificationPasswordWrapper
//        driverManager.executeQuery("SELECT * FROM verificationcodes", { rs ->
//            while (rs.next()) {
//                runBlocking {
//                    vpw.set(rs.getLong("guildId"), rs.getString("code"))
//                }
//                println("migrated ${vccounter++} selfrolegroups")
//            }
//        })
//
//        var srgcounter = 0
//        val srgw = context.daoManager.selfRoleGroupWrapper
//        driverManager.executeQuery("SELECT * FROM old_selfrolegroups", { rs ->
//            while (rs.next()) {
//                runBlocking {
//                    srgw.insertOrUpdate(rs.getLong("guildId"),
//                        SelfRoleGroup(
//                            rs.getString("groupName"),
//                            emptyList(),
//                            rs.getLong("channelId"),
//                            rs.getBoolean("isEnabled"),
//                            "",
//                            rs.getBoolean("isSelfRoleAble")
//                        )
//                    )
//                }
//                println("migrated ${srgcounter++} selfrolegroups")
//            }
//        })
//
//        val srw = context.daoManager.selfRoleWrapper
//        val map = mutableMapOf<Long, Map<String, Map<String, Long>>>()
//        driverManager.executeQuery("SELECT * FROM old_selfroles", { rs ->
//            while (rs.next()) {
//                val guildId = rs.getLong("guildId")
//                val groupName = rs.getString("groupName")
//                val guildMap = map.getOrDefault(guildId, emptyMap()).toMutableMap()
//                val groupMap = guildMap.getOrDefault(groupName, emptyMap()).toMutableMap()
//                groupMap[rs.getString("emoteji")] = rs.getLong("roleId")
//                guildMap[groupName] = groupMap
//                map[guildId] = guildMap
//            }
//        })
//
//        var srcounter = 0
//        for ((guildId, guildMap) in map) {
//            for ((group, emotejiMap) in guildMap) {
//                for ((emote, roleId) in emotejiMap) {
//                    srw.set(guildId, group, emote, roleId)
//                    println("migrated ${srcounter++} selfroles")
//                }
//            }
//        }
    }
}