package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent

class VoiceJoinListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildVoiceJoinEvent) {
            if (!event.member.user.isBot) {
                TaskManager.async(event.member.user, event.guild) {
                    VoiceUtil.channelUpdate(container, event.channelJoined)
                    handleChannelRole(event)
                }
            }
        }
    }

    private suspend fun handleChannelRole(event: GuildVoiceJoinEvent) {
        val selfMember = event.guild.selfMember
        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return
        val member = event.member

        val wrapper = container.daoManager.channelRoleWrapper
        val shouldAdd = wrapper.getRoleIds(event.guild.idLong, event.channelJoined.idLong)

        for (roleId in shouldAdd) {
            val role = event.guild.getRoleById(roleId) ?: continue
            if (selfMember.canInteract(role)) {
                event.guild.addRoleToMember(member, role).reason("channelRole").queue()
            }
        }
    }
}