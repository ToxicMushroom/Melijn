package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.commandutil.game.OsuUtil.addBeatmapInfoToEmbed
import me.melijn.melijnbot.commandutil.game.OsuUtil.convertRankToEmote
import me.melijn.melijnbot.commandutil.game.OsuUtil.getBeatmapNMessage
import me.melijn.melijnbot.commandutil.game.OsuUtil.getOsuAvatarUrl
import me.melijn.melijnbot.commandutil.game.OsuUtil.getOsuUserLink
import me.melijn.melijnbot.commandutil.game.OsuUtil.getOsuUserinfoNMessage
import me.melijn.melijnbot.commandutil.game.OsuUtil.getOsuUsernameByArgsNMessage
import me.melijn.melijnbot.commandutil.game.OsuUtil.getStatsDecimalformat
import me.melijn.melijnbot.commandutil.game.OsuUtil.sendUnknownOsuUserMsg
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getDurationString
import me.melijn.melijnbot.internals.utils.getIntegerFromArgN
import me.melijn.melijnbot.internals.utils.getStringFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable
import me.melijn.melijnbot.internals.web.apis.OsuMode

class OsuCommand : AbstractCommand("command.osu") {

    init {
        id = 206
        name = "osu"
        aliases = arrayOf("osu!", "o")
        val mode = OsuMode.OSU
        children = arrayOf(
            UserArg(root, mode),
            TopArg(root, mode),
            RecentArg(root, mode),
            SetUserArg(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }

    class SetUserArg(val parent: String) : AbstractCommand("$parent.setuser") {

        init {
            name = "setUser"
            aliases = arrayOf("su")
        }

        override suspend fun execute(context: ICommandContext) {
            val osuWrapper = context.daoManager.osuWrapper
            if (context.args.isEmpty()) {
                val currentName = osuWrapper.getUserName(context.authorId)
                val msg = if (currentName == null) {
                    context.getTranslation("$root.show.unset")
                } else {
                    context.getTranslation("$root.show.set")
                        .withSafeVariable("name", currentName)
                }
                sendRsp(context, msg)
                return
            }

            val name = getStringFromArgsNMessage(context, 0, 1, 50) ?: return

            if (context.args.first() == "null") {
                osuWrapper.remove(context.authorId)
                val msg = context.getTranslation("$root.unset")
                sendRsp(context, msg)
                return
            }

            getOsuUserinfoNMessage(context, name, OsuMode.OSU) ?: return
            osuWrapper.setName(context.authorId, name)

            val msg = context.getTranslation("$root.set")
                .withSafeVariable("name", name)
            sendRsp(context, msg)
        }
    }

    class RecentArg(val parent: String, private val osuMode: OsuMode) : AbstractCommand("$parent.recent") {

        init {
            name = "recent"
        }

        override suspend fun execute(context: ICommandContext) {
            val name = getOsuUsernameByArgsNMessage(context, 0) ?: return

            val results = context.webManager.osuApi.getUserRecentPlays(name, osuMode)
            if (results == null || results.isEmpty()) {
                val msg = context.getTranslation("$root.norecent")
                    .withSafeVariable("name", name)
                sendRsp(context, msg)
                return
            }

            val index = (getIntegerFromArgN(context, 1, 1, results.size) ?: 1) - 1
            val result = results[index]
            val beatMap = getBeatmapNMessage(context, result, index) ?: return

            val eb = Embedder(context)
            eb.setAuthor(
                name + " | recent #${index + 1}",
                getOsuUserLink(result.userId),
                getOsuAvatarUrl(result.userId)
            )

            addBeatmapInfoToEmbed(result, eb, beatMap)
            sendEmbedRsp(context, eb.build())
        }
    }

    class TopArg(val parent: String, private val osuMode: OsuMode) : AbstractCommand("$parent.top") {

        init {
            name = "top"
            aliases = arrayOf("best", "t", "b")
        }

        override suspend fun execute(context: ICommandContext) {
            val name = getOsuUsernameByArgsNMessage(context, 0) ?: return
            val results = context.webManager.osuApi.getUserTopPlays(name, osuMode)
            if (results == null) {
                sendUnknownOsuUserMsg(context, name)
                return
            }

            if (results.isEmpty()) {
                val msg = context.getTranslation("$parent.unknownscore")
                    .withSafeVariable("name", name)
                sendRsp(context, msg)
                return
            }

            val index = (getIntegerFromArgN(context, 1, 1, results.size) ?: 1) - 1
            val result = results[index]
            val beatMap = getBeatmapNMessage(context, result, index) ?: return

            val eb = Embedder(context)
            eb.setAuthor(
                name + " | score #${index + 1}",
                "https://osu.ppy.sh/scores/osu/${result.scoreId}",
                "https://s.ppy.sh/a/${result.userId}"
            )
            addBeatmapInfoToEmbed(result, eb, beatMap)

            sendEmbedRsp(context, eb.build())
        }
    }

    class UserArg(val parent: String, private val osuMode: OsuMode) : AbstractCommand("$parent.user") {

        init {
            name = "user"
            aliases = arrayOf("u", "profile", "p")
        }

        override suspend fun execute(context: ICommandContext) {
            val name = getOsuUsernameByArgsNMessage(context, 0) ?: return
            val result = getOsuUserinfoNMessage(context, name, osuMode) ?: return

            val ssSEmote = convertRankToEmote("SSH")
            val ssEmote = convertRankToEmote("SS")
            val sSEmote = convertRankToEmote("SH")
            val sEmote = convertRankToEmote("S")
            val aEmote = convertRankToEmote("A")

            val formatter = getStatsDecimalformat()
            val eb = Embedder(context)
                .setTitle(result.username, getOsuUserLink(result.id))
                .addField("Games Played", formatter.format(result.plays), true)
                .addField("Accuracy", formatter.format(result.acc) + "%", true)
                .addField("Level", formatter.format(result.level), true)
                .addField(
                    "Scores", """
                    ${ssSEmote}: ${formatter.format(result.countSSH)}
                    ${ssEmote}: ${formatter.format(result.countSS)}
                    ${sSEmote}: ${formatter.format(result.countSH)}
                    ${sEmote}: ${formatter.format(result.countS)}
                    ${aEmote}: ${formatter.format(result.countA)}""", true
                )
                .addField("Playtime", getDurationString(result.playtime * 1000), true)
                .addField(
                    "Rank", """
                        🌐 - ${formatter.format(result.rank)}
                        :flag_${result.country.lowercase()}: - ${formatter.format(result.localRank)}
                        """.trimIndent(), true
                )
                .setThumbnail(getOsuAvatarUrl(result.id))
                .setFooter("Joined: ${result.joinDate}")

            sendEmbedRsp(context, eb.build())
        }
    }
}

enum class OsuMod(val offset: Int) {
    NF(1), // NO fail
    Easy(2),
    TouchDevice(3),
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
    FadeIn(21),
    Random(22), // ??
    Cinema(23), // ??
    Target(24), // ??
    K9(25), // 9Key
    KeyCoop(26), // ?
    K1(27), // 1Key
    K2(28), // 2Key
    K3(29), // 3Key
    ScoreV2(30),
    Mirrored(31);

    companion object {
        fun byIndex(index: Int): OsuMod? {
            return values().firstOrNull { it.offset == index }
        }
    }
}