package me.melijn.melijnbot.internals.services.music

import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.web.spotify.MySpotifyApi
import java.util.concurrent.TimeUnit

class SpotifyService(
    private val spotifyApi: MySpotifyApi
) : Service("Spotify", 30, 30, TimeUnit.MINUTES) {

    override val service = RunnableTask {
        spotifyApi.updateSpotifyCredentials()
    }
}