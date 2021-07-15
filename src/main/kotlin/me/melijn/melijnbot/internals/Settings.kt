package me.melijn.melijnbot.internals

import io.github.cdimascio.dotenv.dotenv
import me.melijn.melijnbot.enums.Environment
import me.melijn.melijnbot.internals.utils.splitIETEL
import net.dv8tion.jda.api.utils.data.DataArray

data class Settings(
    val botInfo: BotInfo,
    val restServer: RestServer,
    val helperBot: HelperBot,
    val api: Api,
    val proxy: Proxy,
    val environment: Environment,
    val lavalink: Lavalink,
    val tokens: Token,
    val database: Database,
    val redis: Redis,
    val emote: Emote,
    val economy: Economy,
    val unLoggedThreads: Array<String>,
    val sentry: Sentry
) {

    data class Sentry(
        val url: String
    )

    data class BotInfo(
        val prefix: String,
        val id: Long,
        val shardCount: Int,
        val embedColor: Int,
        val podCount: Int,
        val exceptionChannel: Long,
        val hostPattern: String,
        val developerIds: LongArray
    )

    data class RestServer(
        val port: Int,
        val token: String
    )

    data class Api(
        val jikan: Jikan,
        val spotify: Spotify,
        val imgHoard: ImgHoard,
        val sauceNao: SauceNao,
        val twitter: Twitter
    ) {

        data class Spotify(
            var clientId: String,
            var password: String
        )

        data class Jikan(
            var ssl: Boolean,
            var host: String,
            var key: String,
            var port: Int
        )

        data class ImgHoard(
            var token: String
        )

        data class SauceNao(
            var token: String
        )

        data class Twitter(
            var key: String,
            var secretKey: String,
            var bearerToken: String,
        )
    }

    data class Proxy(
        val enabled: Boolean,
        val host: String,
        val port: Int
    )

    data class Lavalink(
        var http_nodes: Array<LLNode>,
        var verified_nodes: Array<LLNode>,
        var enabled_http_nodes: Boolean,
        var enabled: Boolean
    ) {
        data class LLNode(val groupId: String, val host: String, val pass: String)
    }

    data class Token(
        var discord: String,
        var weebSh: String,
        var randomCatApi: String,
        var kSoftApi: String,
        var osu: String,
        var hot: String,
        val tenor: String
    )

    data class Database(
        var database: String,
        var password: String,
        var user: String,
        var host: String,
        var port: Int
    )

    data class Redis(
        val host: String,
        val port: Int,
        val password: String,
        val enabled: Boolean
    )

    data class Emote(
        val slotId: Long
    )

    data class Economy(
        val baseMel: Long,
        val premiumMultiplier: Float,
        val streakExpireHours: Int
    )

    data class HelperBot(
        val host: String,
        val token: String
    )

    companion object {
        private val dotenv = dotenv {
            this.filename = System.getenv("ENV_FILE") ?: ".env"
            this.ignoreIfMissing = true
        }

        fun get(path: String): String = dotenv[path.uppercase().replace(".", "_")]
            ?: throw IllegalStateException("missing env value: $path")

        fun getLong(path: String): Long = get(path).toLong()
        fun getFloat(path: String): Float = get(path).toFloat()
        fun getInt(path: String): Int = get(path).toInt()
        fun getBoolean(path: String): Boolean = get(path).toBoolean()

        fun initSettings(): Settings {

            val llNodes = mutableListOf<Lavalink.LLNode>()
            val groupList = DataArray.fromJson(get("lavalink.nodes"))
            for (i in 0 until groupList.length()) {
                val groupEntry = groupList.getObject(i)
                val group = groupEntry.getString("group")
                val nodes = groupEntry.getArray("nodes")
                for (j in 0 until nodes.length()) {
                    val node = nodes.getObject(j)
                    llNodes.add(Lavalink.LLNode(group, node.getString("host"), node.getString("pass")))
                }
            }

            return Settings(
                BotInfo(
                    get("botinfo.prefix"),
                    getLong("botinfo.id"),
                    getInt("botinfo.shardCount"),
                    getInt("botinfo.embedColor"),
                    getInt("botinfo.podCount"),
                    getLong("botinfo.exceptionsChannelId"),
                    get("botinfo.hostPattern"),
                    get("botinfo.developerIds").split(",").map { it.toLong() }.toLongArray()
                ),
                RestServer(
                    getInt("restserver.port"),
                    get("restserver.token")
                ),
                HelperBot(
                    get("helperbot.host"),
                    get("helperbot.token")
                ),
                Api(
                    Api.Jikan(
                        getBoolean("api.jikan.ssl"),
                        get("api.jikan.host"),
                        get("api.jikan.key"),
                        getInt("api.jikan.port")
                    ),
                    Api.Spotify(
                        get("api.spotify.clientId"),
                        get("api.spotify.password")
                    ),
                    Api.ImgHoard(
                        get("api.imghoard.token")
                    ),
                    Api.SauceNao(
                        get("api.saucenao.token")
                    ),
                    Api.Twitter(
                        get("api.twitter.key"),
                        get("api.twitter.secretkey"),
                        get("api.twitter.bearertoken"),
                    )
                ),
                Proxy(
                    getBoolean("proxy.enabled"),
                    get("proxy.host"),
                    getInt("proxy.port")
                ),
                Environment.valueOf(get("environment")),
                Lavalink(
                    llNodes.filter { it.groupId == "http" }.toTypedArray(),
                    llNodes.filter { it.groupId == "normal" }.toTypedArray(),
                    getBoolean("lavalink.enabled.http.nodes"),
                    getBoolean("lavalink.enabled")
                ),
                Token(
                    get("token.discord"),
                    get("token.weebSh"),
                    get("token.randomCatApi"),
                    get("token.kSoftApi"),
                    get("token.osuApi"),
                    get("token.hot"),
                    get("token.tenor"),
                ),
                Database(
                    get("database.database"),
                    get("database.password"),
                    get("database.user"),
                    get("database.host"),
                    getInt("database.port")
                ),
                Redis(
                    get("redis.host"),
                    getInt("redis.port"),
                    get("redis.password"),
                    getBoolean("redis.enabled")
                ),
                Emote(
                    getLong("emote.slotId")
                ),
                Economy(
                    getLong("economy.baseMel"),
                    getFloat("economy.premiumMultiplier"),
                    getInt("economy.streakExpireHours")
                ),
                get("unloggedThreads").splitIETEL(",").toTypedArray(),
                Sentry(
                    get("sentry.url")
                )
            )
        }

    }
}

