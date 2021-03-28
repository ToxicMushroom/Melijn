package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent

class VoiceLeaveListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceLeaveEvent) {
            if (!event.member.user.isBot) {
                VoiceUtil.channelUpdate(container, event.channelLeft)
                VoiceUtil.handleChannelRoleLeave(container.daoManager, event.member, event.channelLeft)
            }
        }
    }
}