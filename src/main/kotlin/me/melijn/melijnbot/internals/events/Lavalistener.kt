package me.melijn.melijnbot.internals.events

import me.melijn.llklient.io.jda.JDALavalink
import net.dv8tion.jda.api.events.GenericEvent

class Lavalistener(private val jdaLavalink: JDALavalink) : SuspendListener() {

    override suspend fun onEvent(event: GenericEvent) {
        jdaLavalink.onEvent(event)
    }

}