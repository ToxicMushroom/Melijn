package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import me.melijn.melijnbot.internals.events.eventutil.SelfRoleUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.awaitBool
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent

class MessageReactionRemovedListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildMessageReactionRemoveEvent) {
            onGuildMessageReactionRemove(event)
        }
    }

    private fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) = TaskManager.async {
        selfRoleHandler(event)
    }

    private suspend fun selfRoleHandler(event: GuildMessageReactionRemoveEvent) {
        if (event.user?.isBot == true) return
        val guild = event.guild
        val roles = SelfRoleUtil.getSelectedSelfRoleNByReactionEvent(event, container) ?: return

        for (role in roles) {
            val removed = guild.removeRoleFromMember(event.userIdLong, role).reason("unselfroled").awaitBool()
            if (!removed) {
                val user = event.user ?: event.retrieveUser().awaitOrNull() ?: return
                LogUtils.sendMessageFailedToRemoveRoleFromMember(container.daoManager, user, role)
            }
        }
    }
}