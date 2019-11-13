package me.melijn.melijnbot.objects.events.eventutil

import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.entities.VoiceChannel

object VoiceUtil {
    fun channelUpdate(container: Container, channelJoined: VoiceChannel) {
        println("Updated ${channelJoined.name}")
    }
}