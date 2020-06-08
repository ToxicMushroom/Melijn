package me.melijn.melijnbot.objects.services.music

import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.RunnableTask
import me.melijn.melijnbot.objects.web.spotify.MySpotifyApi
import java.util.concurrent.TimeUnit

class SpotifyService(
    private val spotifyApi: MySpotifyApi
) : Service("Spotify", 30, 30, TimeUnit.MINUTES) {

    override val service = RunnableTask {
        spotifyApi.updateSpotifyCredentials()
    }
}