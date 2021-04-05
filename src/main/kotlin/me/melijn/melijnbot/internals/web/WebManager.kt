package me.melijn.melijnbot.internals.web


import com.apollographql.apollo.ApolloClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.web.bins.BinApis
import me.melijn.melijnbot.internals.web.booru.BooruApi
import me.melijn.melijnbot.internals.web.botlist.BotListApi
import me.melijn.melijnbot.internals.web.jikan.JikanApi
import me.melijn.melijnbot.internals.web.kitsu.KitsuApi
import me.melijn.melijnbot.internals.web.nsfw.Rule34Api
import me.melijn.melijnbot.internals.web.osu.OsuApi
import me.melijn.melijnbot.internals.web.spotify.MySpotifyApi
import me.melijn.melijnbot.internals.web.weebsh.WeebApi
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy


class WebManager(val settings: Settings) {

    val httpClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        install(UserAgent) {
            agent = "Melijn / 2.0.8 Discord bot"
        }
    }
    val proxiedHttpClient = HttpClient(OkHttp) {
        expectSuccess = false
        this.engine {
            val cb = OkHttpClient.Builder()
            val client = cb.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(settings.proxy.host, settings.proxy.port)))
                .build()
            this.preconfigured = client
        }
        install(UserAgent) {
            agent = "Melijn / 2.0.8 Discord bot"
        }
    }
    val aniListApolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://graphql.anilist.co")
        .okHttpClient(OkHttpClient())
        .build()

    var spotifyApi: MySpotifyApi? = null


    val rule34Api: Rule34Api = Rule34Api(httpClient)
    val booruApi: BooruApi = BooruApi(httpClient)
    val binApis: BinApis = BinApis(httpClient)
    val kitsuApi: KitsuApi = KitsuApi(httpClient)
    val osuApi: OsuApi = OsuApi(proxiedHttpClient, settings.tokens.osu)
    val botListApi: BotListApi = BotListApi(httpClient, settings)
    val weebApi: WeebApi = WeebApi(httpClient, settings)
    val jikanApi: JikanApi = JikanApi(settings.api.jikan)

    init {
        if (settings.api.spotify.clientId.isNotBlank() && settings.api.spotify.password.isNotBlank()) {
            spotifyApi = MySpotifyApi(settings.api.spotify)
        }
    }
}