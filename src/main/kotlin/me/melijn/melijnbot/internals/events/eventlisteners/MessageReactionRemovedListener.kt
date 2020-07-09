package me.melijn.melijnbot.internals.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.awaitBool
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent

class MessageReactionRemovedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionRemoveEvent) {
            onGuildMessageReactionRemove(event)
        }
    }

    private fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) = runBlocking {
        selfRoleHandler(event)
    }

    private suspend fun selfRoleHandler(event: GuildMessageReactionRemoveEvent) {
        val guild = event.guild
        val member = event.member ?: guild.retrieveMemberById(event.userIdLong).awaitOrNull() ?: return
        if (member.user.isBot) return
        val roles = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        for (role in roles) {
            if (member.roles.contains(role)) {
                val removed = guild.removeRoleFromMember(member, role).reason("unselfroled").awaitBool()
                if (!removed) {
                    LogUtils.sendMessageFailedToRemoveRoleFromMember(container.daoManager, member, role)
                }
            }
        }
    }
}