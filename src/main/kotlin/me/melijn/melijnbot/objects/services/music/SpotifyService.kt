package me.melijn.melijnbot.objects.services.music

import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import me.melijn.melijnbot.objects.web.WebManager
import java.util.concurrent.TimeUnit

class SpotifyService(
    val webManager: WebManager
) : Service("Spotify", 30, 30, TimeUnit.MINUTES) {

    override val service = Task {
        webManager.updateSpotifyCredentials()
    }
}