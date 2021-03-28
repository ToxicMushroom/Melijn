package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent

class VoiceMoveListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceMoveEvent) {
            if (!event.member.user.isBot) {
                VoiceUtil.channelUpdate(container, event.channelJoined)
                VoiceUtil.channelUpdate(container, event.channelLeft)
                VoiceUtil.handleChannelRoleJoin(container.daoManager, event.member, event.channelJoined)
                VoiceUtil.handleChannelRoleLeave(container.daoManager, event.member, event.channelLeft)
            }
        }
    }
}