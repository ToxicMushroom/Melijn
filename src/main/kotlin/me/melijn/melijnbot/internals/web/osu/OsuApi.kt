package me.melijn.melijnbot.internals.web.osu

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import me.melijn.melijnbot.internals.translation.OSU_URL
import net.dv8tion.jda.api.utils.data.DataArray


class OsuApi(val httpClient: HttpClient, private val apiKey: String) {
    // k - api key (required).
    // u - specify a user_id or a username to return best scores from (required).
    // m - mode (0 = osu!, 1 = Taiko, 2 = CtB, 3 = osu!mania). Optional, default value is 0.
    // type - specify if u is a user_id or a username. Use string for usernames or id for user_ids. Optional, default behavior is automatic recognition (may be problematic for usernames made up of digits only).
    suspend fun getUserInfo(name: String, gameMode: Int = 0): OsuUser? {
        val result = httpClient.get<String>("$OSU_URL/get_user?k=$apiKey&u=$name&type=string&m=$gameMode")
        if (result.isEmpty()) return null
        val data = DataArray.fromJson(result)
        if (data.isEmpty) return null
        val jsonUser = data.getObject(0)

        return OsuUser(
            jsonUser.getString("user_id"),
            jsonUser.getString("username"),
            jsonUser.getString("join_date"),
            jsonUser.getString("playcount").toLong(),
            jsonUser.getString("level").toFloat(),
            jsonUser.getString("accuracy").toFloat(),
            jsonUser.getString("count_rank_ssh").toLong(),
            jsonUser.getString("count_rank_ss").toLong(),
            jsonUser.getString("count_rank_sh").toLong(),
            jsonUser.getString("count_rank_s").toLong(),
            jsonUser.getString("count_rank_a").toLong(),
            jsonUser.getString("country"),
            jsonUser.getString("total_seconds_played").toLong(),
            jsonUser.getString("pp_rank").toLong(),
            jsonUser.getString("pp_country_rank").toLong()
        )
    }

    // limit - amount of results (range between 1 and 100 - defaults to 10).
    suspend fun getUserTopPlays(name: String): List<OsuScoreResult>? {
        val result = httpClient.get<String>("$OSU_URL/get_user_best?k=$apiKey&u=$name&type=string&limit=25")
        if (result.isEmpty()) return null

        val data = DataArray.fromJson(result)
        if (data.isEmpty) return null

        val list = mutableListOf<OsuScoreResult>()

        for (i in 0 until data.length()) {
            val entry = data.getObject(i)
            list.add(OsuScoreResult(
                entry.getString("beatmap_id").toLong(),
                entry.getString("score_id").toLong(),
                entry.getString("score").toLong(),
                entry.getString("maxcombo").toLong(),
                entry.getString("count50").toLong(),
                entry.getString("count100").toLong(),
                entry.getString("count300").toLong(),
                entry.getString("countmiss").toLong(),
                entry.getString("countkatu").toLong(),
                entry.getString("countgeki").toLong(),
                entry.getString("perfect").toBoolean(),
                entry.getString("enabled_mods").toInt(),
                entry.getString("user_id").toLong(),
                entry.getString("date"),
                entry.getString("rank"),
                entry.getString("pp").toFloat(),
                entry.getString("replay_available").toBoolean()
            ))
        }

        return list
    }
}

data class OsuScoreResult(
    val beatmapId: Long,
    val scoreId: Long,
    val score: Long,
    val maxCombo: Long,
    val count50: Long,
    val count100: Long,
    val count300: Long,
    val countMiss: Long,
    val countKatu: Long,
    val countGeki: Long,
    val perfect: Boolean,
    val mods: Int,
    val userId: Long,
    val date: String,
    val rank: String,
    val pp: Float,
    val replay: Boolean
)

data class OsuUser(
    val id: String,
    val username: String,
    val joinDate: String,
    val plays: Long,
    val level: Float,
    val acc: Float,
    val countSSH: Long,
    val countSS: Long,
    val countSH: Long,
    val countS: Long,
    val countA: Long,
    val country: String,
    val playtime: Long,
    val rank: Long,
    val localRank: Long
)