package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import moe.ganen.jikankt.JikanKt

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        val result = JikanKt.searchAnime("Your Name")
        result.results?.let {
            val item = it.firstOrNull() ?: return
            println(item.url)
        }
//        val driverManager = context.daoManager.driverManager
//
//        var jrcounter = 0
//        val jrw = context.daoManager.joinRoleWrapper
//        val jrgw = context.daoManager.joinRoleGroupWrapper
//
//        driverManager.executeQuery("SELECT * FROM roles WHERE roleType = ?", { rs ->
//            while (rs.next()) {
//                runBlocking {
//                    jrgw.insertOrUpdate(context.guildId, JoinRoleGroupInfo("basic", true, true))
//                    val guildId = rs.getLong("guildId")
//                    jrw.set(guildId, "basic", rs.getLong("roleId"), 100)
//                }
//                println("migrated ${jrcounter++} joinroles")
//            }
//        }, "JOIN")

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