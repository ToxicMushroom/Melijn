package me.melijn.melijnbot.internals.services.music

import me.melijn.melijnbot.database.audio.SpotifyKeyWrapper
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.web.apis.MySpotifyApi
import java.util.concurrent.TimeUnit

class SpotifyService(
    private val spotifyKeyWrapper: SpotifyKeyWrapper,
    private val spotifyApi: MySpotifyApi
) : Service("Spotify", 30, 0, TimeUnit.MINUTES) {

    override val service = RunnableTask {
        var cachedToken = spotifyKeyWrapper.get()
        if (PodInfo.podId == 0 || cachedToken == null) {
            cachedToken = spotifyApi.requestNewToken() ?: return@RunnableTask run {
                logger.warn("spotify auth api not giving new token")
            }
            spotifyApi.setApiToken(cachedToken)
            spotifyKeyWrapper.update(cachedToken)
        }
        spotifyApi.setApiToken(cachedToken)
    }
}