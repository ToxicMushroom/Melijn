package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
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
            TopArg(root)
        )
        //commandCategory = CommandCategory.UTILITY
    }


    companion object {
        val emotes = mapOf<String, Long>(
            Pair("SSH", 743910024518041741),
            Pair("SS", 743910024337817641),
            Pair("SH", 743910024564178994),
            Pair("S", 743910024501395496),
            Pair("A", 743910024337817673)
        )
    }

    class TopArg(parent: String) : AbstractCommand("$parent.top") {

        init {
            name = "top"
            aliases = arrayOf("best", "t", "b")
        }

        override suspend fun execute(context: CommandContext) {
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

            val index = (getIntegerFromArgNMessage(context, 1, 1, results.size) ?: return) - 1

            val result = results[index]
            val rankAchievedEmote = emotes[result.rank]?.let { context.shardManager.getEmoteById(it)?.asMention } ?: "?"
            val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.GERMANY))
            formatter.isGroupingUsed = true
            formatter.groupingSize = 3

            var mods = ""
            Integer.toBinaryString(result.mods).split("").reversed().withIndex().forEach { (index, value) ->
                if (value == "1") mods += "${OsuMod.values().first { it.offset == (index) }}, "
            }
            mods = mods.trim().removeSuffix(",")
            val eb = Embedder(context)
                .setTitle(name + " | score #${index + 1}", "https://osu.ppy.sh/users/${result.userId}")
                .addField("pp", formatter.format(result.pp), true)
                .addField("max-combo", formatter.format(result.maxCombo), true)
                .addField("mods", mods, true)
                .addField("hits",
                    "miss: ${formatter.format(result.countMiss)}\n" +
                        "50: ${formatter.format(result.count50)}\n" +
                        "100: ${formatter.format(result.count100)}\n" +
                        "300: ${formatter.format(result.count300)}\n" +
                        "Katu: ${formatter.format(result.countKatu)}\n" +
                        "Geki: ${formatter.format(result.countGeki)}", true)
                .addField("rank", rankAchievedEmote, true)
                .setThumbnail("http://s.ppy.sh/a/${result.userId}")
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

            val ssSEmote = context.shardManager.getEmoteById(743910024518041741)?.asMention ?: "SS*"
            val ssEmote = context.shardManager.getEmoteById(743910024337817641)?.asMention ?: "SS"
            val sSEmote = context.shardManager.getEmoteById(743910024337817641)?.asMention ?: "S*"
            val sEmote = context.shardManager.getEmoteById(743910024501395496)?.asMention ?: "S"
            val aEmote = context.shardManager.getEmoteById(743910024337817673)?.asMention ?: "A"

            val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.GERMANY))
            formatter.isGroupingUsed = true
            formatter.groupingSize = 3


            val eb = Embedder(context)
                .setTitle(result.username, "https://osu.ppy.sh/users/${result.id}")
                .addField("plays", formatter.format(result.plays), true)
                .addField("acc", formatter.format(result.acc).take(5), true)
                .addField("level", formatter.format(result.level).take(6), true)
                .addField("scores", "${ssSEmote}: ${formatter.format(result.countSSH)}\n" +
                    "${ssEmote}: ${formatter.format(result.countSS)}\n" +
                    "${sSEmote}: ${formatter.format(result.countSH)}\n" +
                    "${sEmote}: ${formatter.format(result.countS)}\n" +
                    "${aEmote}: ${formatter.format(result.countA)}", true)
                .addField("rank",
                    "\uD83C\uDF10: ${formatter.format(result.rank)}\n" +
                        ":flag_${result.country.toLowerCase()}:: ${formatter.format(result.localRank)}", true)
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