package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class OsuCommand : AbstractCommand("command.osu") {

    init {
        id = 206
        name = "osu"
        aliases = arrayOf("osu!")
        children = arrayOf(
            UserArg(root),
            TopArg(root),
            RecentArg(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    class RecentArg(parent: String) : AbstractCommand("$parent.recent") {

        init {
            name = "recent"
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 50) ?: return
            val results = context.webManager.osuApi.getUserRecentPlays(name)

            if (results == null) {
                sendRsp(context, "user **$name** not found")
                return
            }

            if (results.isEmpty()) {
                sendRsp(context, "user **$name** has no scores")
                return
            }

            val index = (getIntegerFromArgN(context, 1, 1, results.size) ?: 1) - 1

            val result = results[index]
            val rankAchievedEmote = emotes[result.rank]?.let { context.shardManager.getEmoteById(it)?.asMention }
                ?: result.rank
            val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.GERMANY))
            formatter.isGroupingUsed = true
            formatter.groupingSize = 3

            var mods = ""
            Integer.toBinaryString(result.mods).split("").reversed().withIndex().forEach { (index, value) ->
                if (value == "1") mods += "${OsuMod.values().first { it.offset == (index) }}, "
            }

            val beatMap = context.webManager.osuApi.getBeatMap(result.beatmapId)
            if (beatMap == null) {
                sendRsp(context, "beatmap for ${result.beatmapId} wasn't found")
                return
            }

            val accuracy = (result.count300 * 300 + result.count100 * 100 + result.count50 * 50) * 100.0 /
                ((result.count300 + result.count100 + result.count50 + result.countMiss) * 300.0).toFloat()

            mods = mods.trim().removeSuffix(",")
            val eb = Embedder(context)
                .setAuthor(name + " | recent #${index + 1}", "https://osu.ppy.sh/users/${result.userId}", "https://s.ppy.sh/a/${result.userId}")
                .setThumbnail("https://b.ppy.sh/thumb/${beatMap.beatMapSetId}l.jpg")
                .addField("Beatmap Info", """
                    **title** [${beatMap.title}](https://osu.ppy.sh/b/${beatMap.beatMapId})
                    **author** [${beatMap.creator}](https://osu.ppy.sh/u/${beatMap.creatorId})
                    **artist** ${beatMap.artist}
                    **diff** ${formatter.format(beatMap.difficulty)} | ${beatMap.version}
                    **bpm** ${beatMap.bpm}
                """.trimIndent(), false)
                .addField("Combo", "`" + formatter.format(result.maxCombo) + "x`/" + formatter.format(beatMap.maxCombo) + "x", true)
                .addField("Rank", rankAchievedEmote, true)
                .addField("Score", "`" + formatter.format(result.score) + "`", true)
                .setFooter("Score placed on: ${result.date}")

            eb.addField("Hits", """
                  miss: `${formatter.format(result.countMiss)}`
                  50: `${formatter.format(result.count50)}` 
                  100: `${formatter.format(result.count100)}`
                  300: `${formatter.format(result.count300)}`
            """.trimIndent(), true)
                .addField("Accuracy", "`" + formatter.format(accuracy) + "%`", true)
            if (mods.isNotBlank()) eb.addField("Mods", mods, true)


            sendEmbedRsp(context, eb.build())
        }
    }


    companion object {
        val emotes = mapOf<String, Long>(
            Pair("SSH", 744300240370139226),
            Pair("SS", 744300239946514433),
            Pair("SH", 744300240269475861),
            Pair("S", 744300240202367017),
            Pair("A", 744300239867084842),
            Pair("B", 744300240114417665),
            Pair("C", 744300239954903062),
            Pair("D", 744300240248635503)
        )
    }

    class TopArg(parent: String) : AbstractCommand("$parent.top") {

        init {
            name = "top"
            aliases = arrayOf("best", "t", "b")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 50) ?: return
            val results = context.webManager.osuApi.getUserTopPlays(name)

            if (results == null) {
                sendRsp(context, "user **$name** not found")
                return
            }

            if (results.isEmpty()) {
                sendRsp(context, "user **$name** has no scores")
                return
            }

            val index = (getIntegerFromArgN(context, 1, 1, results.size) ?: 1) - 1

            val result = results[index]
            val rankAchievedEmote = emotes[result.rank]?.let { context.shardManager.getEmoteById(it)?.asMention }
                ?: result.rank
            val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.GERMANY))
            formatter.isGroupingUsed = true
            formatter.groupingSize = 3

            var mods = ""
            Integer.toBinaryString(result.mods).split("").reversed().withIndex().forEach { (index, value) ->
                if (value == "1") mods += "${OsuMod.values().first { it.offset == (index) }}, "
            }

            val beatMap = context.webManager.osuApi.getBeatMap(result.beatmapId)
            if (beatMap == null) {
                sendRsp(context, "beatmap for ${result.beatmapId} wasn't found")
                return
            }

            val accuracy = (result.count300 * 300 + result.count100 * 100 + result.count50 * 500) * 100.0 /
                ((result.count300 + result.count100 + result.count50 + result.countMiss) * 300.0).toFloat()

            mods = mods.trim().removeSuffix(",")
            val eb = Embedder(context)
                .setAuthor(name + " | score #${index + 1}", "https://osu.ppy.sh/scores/osu/${result.scoreId}", "https://s.ppy.sh/a/${result.userId}")
                .setThumbnail("https://b.ppy.sh/thumb/${beatMap.beatMapSetId}l.jpg")
                .addField("Beatmap Info", """
                    **title** [${beatMap.title}](https://osu.ppy.sh/b/${beatMap.beatMapId})
                    **author** [${beatMap.creator}](https://osu.ppy.sh/u/${beatMap.creatorId})
                    **artist** ${beatMap.artist}
                    **diff** ${formatter.format(beatMap.difficulty)} | ${beatMap.version}
                    **bpm** ${beatMap.bpm}
                """.trimIndent(), false)
                .addField("PP", "`" + formatter.format(result.pp) + "`", true)
                .addField("Combo", "`" + formatter.format(result.maxCombo) + "x`/" + formatter.format(beatMap.maxCombo) + "x", true)
                .addField("Rank", rankAchievedEmote, true)

            eb.addField("Hits", """
                  miss: `${formatter.format(result.countMiss)}`
                  50: `${formatter.format(result.count50)}` 
                  100: `${formatter.format(result.count100)}`
                  300: `${formatter.format(result.count300)}`
            """.trimIndent(), true)
                .addField("Accuracy", "`" + formatter.format(accuracy) + "%`", true)
            if (mods.isNotBlank()) eb.addField("Mods", mods, true)
            eb
                .addField("Score", "`" + formatter.format(result.score) + "`", true)
                .setFooter("Score placed on: ${result.date}")

            sendEmbedRsp(context, eb.build())
        }
    }

    class UserArg(parent: String) : AbstractCommand("$parent.user") {

        init {
            name = "user"
            aliases = arrayOf("u", "profile", "p")
        }

        override suspend fun execute(context: CommandContext) {
            val name = getStringFromArgsNMessage(context, 0, 1, 50) ?: return
            val result = context.webManager.osuApi.getUserInfo(name)
            if (result == null) {
                sendRsp(context, "user **$name** not found")
                return
            }

            val ssSEmote = emotes["SSH"]?.let { context.shardManager.getEmoteById(it)?.asMention } ?: "SSH"
            val ssEmote = emotes["SS"]?.let { context.shardManager.getEmoteById(it)?.asMention } ?: "SS"
            val sSEmote = emotes["SH"]?.let { context.shardManager.getEmoteById(it)?.asMention } ?: "SH"
            val sEmote = emotes["S"]?.let { context.shardManager.getEmoteById(it)?.asMention } ?: "S"
            val aEmote = emotes["A"]?.let { context.shardManager.getEmoteById(it)?.asMention } ?: "A"

            val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.GERMANY))
            formatter.isGroupingUsed = true
            formatter.groupingSize = 3


            val eb = Embedder(context)
                .setTitle(result.username, "https://osu.ppy.sh/users/${result.id}")
                .addField("Games Played", formatter.format(result.plays), true)
                .addField("Accuracy", formatter.format(result.acc) + "%", true)
                .addField("Level", formatter.format(result.level), true)
                .addField("Scores", """
                    ${ssSEmote}: ${formatter.format(result.countSSH)}
                    ${ssEmote}: ${formatter.format(result.countSS)}
                    ${sSEmote}: ${formatter.format(result.countSH)}
                    ${sEmote}: ${formatter.format(result.countS)}
                    ${aEmote}: ${formatter.format(result.countA)}""", true)
                .addField("Playtime", getDurationString(result.playtime * 1000), true)
                .addField("Rank", """
                        🌐 - ${formatter.format(result.rank)}
                        :flag_${result.country.toLowerCase()}: - ${formatter.format(result.localRank)}
                        """.trimIndent(), true)
                .setThumbnail("http://s.ppy.sh/a/${result.id}")
                .setFooter("Joined: ${result.joinDate}")


            sendEmbedRsp(context, eb.build())
        }
    }

    override suspend fun execute(context: CommandContext) {
        sendSyntax(context)
    }
}

enum class OsuMod(val offset: Int) {
    NF(1), // NO fail
    EASY(2),
    TOUCH_DEVICE(3),
    HD(4), // Hidden
    HR(5), // Hard Rock
    SD(6), // Sudden Death
    DT(7), // Double Time
    RX(8), // Relax, auto tap
    HT(9), // Half Time
    NC(10), // Night Core, only on top of DT
    FL(11), // Flash Light
    AUTO(12), // Auto
    SpunOut(13),
    Relax2(14), // Moves cursor for you
    Perfect(15), // only on top of Sudden Death
    K4(16), // 4Key
    K5(17), // 5Key
    K6(18), // 6Key
    K7(19), // 7Key
    K8(20), // 8Key
    FADE_IN(21),
    RANDOM(22), // ??
    CINEMA(23), // ??
    TARGET(24), // ??
    K9(25), // 9Key
    KEY_COOP(26), // ?
    K1(27), // 1Key
    K2(28), // 2Key
    K3(29), // 3Key
    SCORE_V2(30),
    MIRRO(31)

}