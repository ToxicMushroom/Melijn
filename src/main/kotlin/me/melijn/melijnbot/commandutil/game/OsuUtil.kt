package me.melijn.melijnbot.commandutil.game

import me.melijn.melijnbot.commands.utility.OsuMod
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.getSyntax
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.web.apis.OsuBeatMap
import me.melijn.melijnbot.internals.web.apis.OsuMode
import me.melijn.melijnbot.internals.web.apis.OsuMutuableResult
import me.melijn.melijnbot.internals.web.apis.OsuUser
import net.dv8tion.jda.api.entities.User
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

object OsuUtil {
    suspend fun retrieveDiscordUserForOsuByArgsN(context: ICommandContext, index: Int): User? {
        return when {
            context.args.size > index -> {
                val arg = context.args[index]

                when {
                    DISCORD_ID.matches(arg) -> {
                        context.shardManager.retrieveUserById(arg).awaitOrNull()
                    }
                    USER_MENTION.matches(arg) -> {
                        val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
                        context.message.mentionedUsers.firstOrNull { it.id == id } ?:
                        context.shardManager.retrieveUserById(id).awaitOrNull()
                    }
                    else -> null
                }
            }
            else -> null
        }
    }

    private val emotes = listOf(
        Triple("SSH", 744300240370139226, "GradeSSSilver"),
        Triple("SS", 744300239946514433, "GradeSS"),
        Triple("SH", 744300240269475861, "GradeSSilver"),
        Triple("S", 744300240202367017, "GradeS"),
        Triple("A", 744300239867084842, "GradeA"),
        Triple("B", 744300240114417665, "GradeB"),
        Triple("C", 744300239954903062, "GradeC"),
        Triple("D", 744300240248635503, "GradeD")
    )

    fun convertRankToEmote(rank: String): String? {
        val (_, id, name) = emotes.firstOrNull() { it.first == rank } ?: return null
        return "<:$name:$id>"
    }

    /** Return possibly invalid osu usernames
     * @param useCache | when set to false, wont try to get user by cache, always use the input if present
     *  If the index is not present OR if string at index is '-' -> Use cache
     *  else -> Use their input
     * **/
    suspend fun getOsuUsernameByArgsNMessage(
        context: ICommandContext,
        index: Int,
        useCache: Boolean = true
    ): String? {
        if (useCache && (context.args.size >= index || context.args[index] == "-"))
            return getOsuUsernameNMessage(context)

        return getOsuUsernameByArgsNMessage(context, index)
    }

    /** Might return invalid supplied osu usernames **/
    private suspend fun getOsuUsernameByArgsNMessage(
        context: ICommandContext,
        index: Int
    ): String? {
        val username = OsuUtil.retrieveDiscordUserForOsuByArgsN(context, index)?.let { user ->
            context.daoManager.osuWrapper.getUserName(user.idLong)
        }
        return username ?: getStringFromArgsNMessage(context, index, 1, 50)
    }

    /** Will get the cached osu username, might be an invalid one **/
    private suspend fun getOsuUsernameNMessage(
        context: ICommandContext
    ): String? {
        val name = context.daoManager.osuWrapper.getUserName(context.authorId)
        val called = context.commandOrder.last()
        val parent = context.commandOrder.first()
        if (name == null) {
            val msg = context.getTranslation("${parent.root}.guide")
                .withVariable("syntax", getSyntax(context, called.syntax))
                .withSafeVariable("prefix", context.usedPrefix)

            sendRsp(context, msg)
        }
        return name
    }

    suspend fun getOsuUserinfoNMessage(context: ICommandContext, name: String, mode: OsuMode): OsuUser? {
        val profile = context.webManager.osuApi.getUserInfo(name, mode)
        if (profile == null) sendUnknownOsuUserMsg(context, name)
        return profile
    }

    suspend fun sendUnknownOsuUserMsg(context: ICommandContext, name: String) {
        val parent = context.commandOrder.first()
        val msg = context.getTranslation("${parent.root}.unknownuser")
            .withSafeVariable("name", name)
        sendRsp(context, msg)
    }

    fun getStatsDecimalformat(): DecimalFormat {
        val formatter = DecimalFormat("#.##", DecimalFormatSymbols(Locale.GERMANY))
        formatter.isGroupingUsed = true
        formatter.groupingSize = 3
        return formatter
    }

    suspend fun getBeatmapNMessage(context: ICommandContext, result: OsuMutuableResult, index: Int): OsuBeatMap? {
        val beatMap = context.webManager.osuApi.getBeatMap(result.toMutual().beatmapId)
        if (beatMap == null) {
            val parentRoot = context.commandOrder.first().root
            val msg = context.getTranslation("$parentRoot.unknownbeatmapfromscore")
                .withVariable("x", index + 1)
            sendRsp(context, msg)
        }
        return beatMap
    }

    private fun getOsuModsString(mods: Int): String {
        val sb = StringBuilder()
        Integer.toBinaryString(mods)
            .splitIETEL("")
            .reversed()
            .withIndex()
            .forEach { (index, value) ->
                if (value == "1") {
                    val modName = OsuMod.byIndex(index) ?: "missing"
                    sb.append(modName).append(", ")
                }
            }
        return sb.toString().removeSuffix(", ")
    }

    fun addBeatmapInfoToEmbed(
        mutuableResult: OsuMutuableResult,
        eb: Embedder,
        beatMap: OsuBeatMap
    ) {
        val result = mutuableResult.toMutual()
        val rankAchievedEmote = convertRankToEmote(result.rank) ?: result.rank
        val formatter = getStatsDecimalformat()
        val mods = getOsuModsString(result.mods)
        eb.setThumbnail("https://b.ppy.sh/thumb/${beatMap.beatMapSetId}l.jpg")
        eb.addField(
            "Beatmap Info", """
                            **title** [${beatMap.title}](https://osu.ppy.sh/b/${beatMap.beatMapId})
                            **author** [${beatMap.creator}](https://osu.ppy.sh/u/${beatMap.creatorId})
                            **artist** ${beatMap.artist}
                            **diff** ${formatter.format(beatMap.difficulty)} | ${beatMap.version}
                            **bpm** ${beatMap.bpm}
                        """.trimIndent(), false
        )
        if (result.pp != null) eb.addField("PP", "`${formatter.format(result.pp)}`", true)
        eb.addField(
            "Combo",
            "`${formatter.format(result.maxCombo)}x`/${formatter.format(beatMap.maxCombo)}x",
            true
        )
        eb.addField("Rank", rankAchievedEmote, true)
        eb.addField("Score", "`${formatter.format(result.score)}`", true)
        eb.setFooter("Score placed on: ${result.date}")

        eb.addField(
            "Hits", """
                          miss: `${formatter.format(result.countMiss)}`
                          50: `${formatter.format(result.count50)}` 
                          100: `${formatter.format(result.count100)}`
                          300: `${formatter.format(result.count300)}`
                    """.trimIndent(), true
        )
        eb.addField("Accuracy", "`${formatter.format(result.accuracy)}%`", true)
        if (mods.isNotBlank()) eb.addField("Mods", mods, true)
    }

    /** osu user profile link format **/
    fun getOsuUserLink(userId: Any) = "https://osu.ppy.sh/users/${userId}"

    /** osu avatar url format **/
    fun getOsuAvatarUrl(userId: Any) = "https://s.ppy.sh/a/${userId}"
}