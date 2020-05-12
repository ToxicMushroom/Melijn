package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        runConditions = arrayOf(
            RunCondition.VC_BOT_ALONE_OR_USER_DJ,
            RunCondition.PLAYING_TRACK_NOT_NULL,
            RunCondition.VOTED
        )
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
//        if (context.args.isEmpty()) {
//            sendSyntax(context)
//            return
//        }
//
//
//        val iPlayer = context.guildMusicPlayer.guildTrackManager.iPlayer
//        when {
//            context.args[0] == "speed" -> {
//                val speed = ((getLongFromArgNMessage(context, 1, 0) ?: return) / 100.0)
//                iPlayer.speed = speed
//
//                sendMsg(context, "set speed of playing track to $speed")
//                return
//            }
//            context.args[0] == "pitch" -> {
//                val pitch = ((getLongFromArgNMessage(context, 1, 0) ?: return) / 100.0)
//                iPlayer.pitch = pitch
//
//                sendMsg(context, "set pitch of playing track to $pitch")
//                return
//            }
//            context.args[0] == "rate" -> {
//                val rate = ((getLongFromArgNMessage(context, 1, 0) ?: return) / 100.0)
//                iPlayer.rate = rate
//
//                sendMsg(context, "set rate of playing track to $rate")
//                return
//            }
//            context.args[0] == "frequency" -> {
//                val frequency = getFloatFromArgNMessage(context, 1, 0.0f) ?: return
//                iPlayer.setTremolo(frequency, iPlayer.tremoloDepth)
//
//                sendMsg(context, "set tremolo frequency of playing track to $frequency")
//                return
//            }
//            context.args[0] == "depth" -> {
//                val depth = getFloatFromArgNMessage(context, 1, 0.0f) ?: return
//                iPlayer.setTremolo(iPlayer.tremoloFrequency, depth)
//
//                sendMsg(context, "set tremolo depth of playing track to $depth")
//                return
//            }
//            else -> sendSyntax(context)
//        }
    }
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