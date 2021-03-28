package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent

class VoiceLeaveListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceLeaveEvent) {
            if (!event.member.user.isBot) {
                VoiceUtil.channelUpdate(container, event.channelLeft)
                handleChannelRole(event)
            }
        }
    }

    private suspend fun handleChannelRole(event: GuildVoiceLeaveEvent) {
        val selfMember = event.guild.selfMember
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return
        val member = event.member

        val wrapper = container.daoManager.channelRoleWrapper
        val shouldAdd = wrapper.getRoleIds(event.guild.idLong, event.channelLeft.idLong)

        for (roleId in shouldAdd) {
            val role = event.guild.getRoleById(roleId) ?: continue
            if (selfMember.canInteract(role)) {
                event.guild.removeRoleFromMember(member, role).reason("channelRole").queue()
            }
        }
    }
}