package me.melijn.melijnbot.internals.web.jikan

import me.melijn.melijnbot.internals.Settings
import moe.ganen.jikankt.JikanKt
import moe.ganen.jikankt.connection.RestClient

class JikanApi(val settings: Settings.Api.Jikan) {

    init {
        val jikanUrl = settings.run {
            "http${(if (ssl) "s" else "")}://${host}:${port}/public/v3/"
        }

        JikanKt.apply {
            this.restClient = RestClient(false, jikanUrl, settings.key)
        }
    }
}