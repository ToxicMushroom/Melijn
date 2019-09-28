package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.SelfRoleUtil
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent

class MessageReactionRemovedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionRemoveEvent) onGuildMessageReactionRemove(event)
    }

    private fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) = runBlocking {
        selfRoleHandler(event)
    }

    private suspend fun selfRoleHandler(event: GuildMessageReactionRemoveEvent) {
        val guild = event.guild
        val member = event.member
        val role = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        if (member.roles.contains(role)) {
            guild.removeRoleFromMember(member, role).queue()
        }
    }
}