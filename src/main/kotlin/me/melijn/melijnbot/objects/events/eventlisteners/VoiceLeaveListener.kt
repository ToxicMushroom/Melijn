package me.melijn.melijnbot.objects.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent

class VoiceLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceLeaveEvent) {
            VoiceUtil.channelUpdate(container, event.channelLeft)
        }
    }
}