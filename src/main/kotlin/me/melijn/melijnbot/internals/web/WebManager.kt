package me.melijn.melijnbot.internals.web

import com.apollographql.apollo.ApolloClient
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import me.melijn.melijnbot.internals.Settings
import me.melijn.melijnbot.internals.web.apis.*
import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy

class WebManager(val settings: Settings) {

    val objectMapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val commonClientConfig: HttpClientConfig<OkHttpConfig>.() -> Unit = {
        expectSuccess = false
        install(JsonFeature) {
            serializer = JacksonSerializer(objectMapper)
        }
        install(UserAgent) {
            agent = "Melijn / 2.0.8 Discord bot"
        }
    }

    val httpClient = HttpClient(OkHttp, commonClientConfig)
    val proxiedHttpClient = HttpClient(OkHttp) {
        commonClientConfig(this)
        this.engine {
            val clientBuilder = OkHttpClient.Builder()
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(settings.proxy.host, settings.proxy.port))
            val client = clientBuilder.proxy(proxy)
                .build()
            this.preconfigured = client
        }
    }

    val aniListApolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://graphql.anilist.co")
        .okHttpClient(OkHttpClient())
        .build()

    var spotifyApi: MySpotifyApi? = null

    val tenorApi: TenorApi = TenorApi(httpClient, settings.tokens.tenor)
    val rule34Api: Rule34Api = Rule34Api(httpClient)
    val imageApi: ImageApi = ImageApi(httpClient, proxiedHttpClient)
    val booruApi: BooruApi = BooruApi(httpClient)
    val osuApi: OsuApi = OsuApi(proxiedHttpClient, settings.tokens.osu)
    val weebApi: WeebApi = WeebApi(httpClient, settings)

    init {
        if (settings.api.spotify.clientId.isNotBlank() && settings.api.spotify.password.isNotBlank()) {
            spotifyApi = MySpotifyApi(settings.api.spotify)
        }
    }
}