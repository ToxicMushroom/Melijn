package me.melijn.melijnbot.internals.web


import com.apollographql.apollo.ApolloClient
import io.ktor.client.*
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.web.bins.BinApis
import me.melijn.melijnbot.internals.web.botlist.BotListApi
import me.melijn.melijnbot.internals.web.kitsu.KitsuApi
import me.melijn.melijnbot.internals.web.osu.OsuApi
import me.melijn.melijnbot.internals.web.spotify.MySpotifyApi
import me.melijn.melijnbot.internals.web.weebsh.WeebshApi
import okhttp3.OkHttpClient


class WebManager(val settings: Settings) {

    val httpClient = HttpClient()

    val aniListApolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://graphql.anilist.co")
        .okHttpClient(OkHttpClient())
        .build()


    var spotifyApi: MySpotifyApi? = null
    val binApis: BinApis = BinApis(httpClient)
    val kitsuApi: KitsuApi = KitsuApi(httpClient)
    val osuApi: OsuApi = OsuApi(httpClient, settings.tokens.osu)
    val botListApi: BotListApi = BotListApi(httpClient, settings)
    val weebshApi: WeebshApi = WeebshApi(settings)

    init {
        if (settings.api.spotify.clientId.isNotBlank() && settings.api.spotify.password.isNotBlank()) {
            spotifyApi = MySpotifyApi(settings.api.spotify)
        }
    }
}