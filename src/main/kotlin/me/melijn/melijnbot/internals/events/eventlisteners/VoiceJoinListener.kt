package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent

class VoiceJoinListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) = runBlocking {
        if (event is GuildVoiceJoinEvent) {
            if (!event.member.user.isBot) {
                VoiceUtil.channelUpdate(container, event.channelJoined)
            }
        }
    }
}