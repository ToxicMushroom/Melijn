package me.melijn.melijnbot.objects.web


import com.apollographql.apollo.ApolloClient
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.web.bins.BinApis
import me.melijn.melijnbot.objects.web.cancer.BotListApi
import me.melijn.melijnbot.objects.web.kitsu.KitsuApi
import me.melijn.melijnbot.objects.web.spotify.MySpotifyApi
import me.melijn.melijnbot.objects.web.weebsh.WeebshApi
import okhttp3.Dispatcher
import okhttp3.OkHttpClient


class WebManager(val taskManager: TaskManager, val settings: Settings) {


    val httpClient = OkHttpClient()
        .newBuilder()
        .dispatcher(Dispatcher(taskManager.executorService))
        .build()


    val aniListApolloClient: ApolloClient = ApolloClient.builder()
        .serverUrl("https://graphql.anilist.co")
        .okHttpClient(httpClient)
        .build()


    var spotifyApi: MySpotifyApi? = null
    val binApis: BinApis = BinApis(httpClient)
    val kitsuApi: KitsuApi = KitsuApi(httpClient)
    val botListApi: BotListApi = BotListApi(httpClient, taskManager, settings)
    val weebshApi: WeebshApi = WeebshApi(settings)

    init {
        if (settings.spotify.clientId.isNotBlank() && settings.spotify.password.isNotBlank())
            spotifyApi = MySpotifyApi(httpClient, taskManager, settings.spotify)
    }
}