package me.melijn.melijnbot.internals

import me.melijn.melijnbot.enums.Environment


data class Settings(
    val prefix: String,
    val id: Long,
    val name: String,
    val version: String,
    val developerIds: LongArray,
    val environment: Environment,
    val shardCount: Int,
    val restPort: Int,
    val embedColor: Int,
    val exceptionChannel: Long,
    val jikan: Jikan,
    val melijnCDN: MelijnCDN,
    val imghoard: Imghoard,
    val spotify: Spotify,
    val lavalink: Lavalink,
    val tokens: Tokens,
    val database: Database,
    val unLoggedThreads: Array<String>
) {


    data class Spotify(
        var clientId: String,
        var password: String
    )

    data class Jikan(
        var ssl: Boolean,
        var host: String,
        var port: Int
    )

    data class Lavalink(
        var http_nodes: Array<Node>,
        var verified_nodes: Array<Node>,
        var enabled: Boolean

    ) {

        data class Node(
            var host: String,
            var password: String
        )
    }

    data class Tokens(
        var discord: String,
        var topDotGG: String,
        var weebSh: String,
        var melijnRest: String,
        var botsOnDiscordXYZ: String,
        var botlistSpace: String,
        var discordBotListCom: String,
        var discordBotsGG: String,
        var botsForDiscordCom: String,
        var discordBoats: String,
        var randomCatApi: String,
        var kSoftApi: String
    )

    data class Database(
        var database: String,
        var password: String,
        var user: String,
        var host: String,
        var port: Int
    )

    data class MelijnCDN(
        var token: String
    )

    data class Imghoard(
        var token: String
    )
}
