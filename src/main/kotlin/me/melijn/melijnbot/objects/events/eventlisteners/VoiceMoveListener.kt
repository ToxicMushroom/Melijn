package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent

class VoiceMoveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceMoveEvent) {
            runBlocking {
                VoiceUtil.channelUpdate(container, event.channelJoined, event.member.user)
                VoiceUtil.channelUpdate(container, event.channelLeft, event.member.user)
            }
        }
    }
}