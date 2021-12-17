package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.events.AbstractListener
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildBanEvent

class BannedEventListener(container: Container) : AbstractListener(container) {

    override suspend fun onEvent(event: GenericEvent) {
        if (event is GuildBanEvent) {
            onGuildBan(event)
        }
    }

    private fun onGuildBan(event: GuildBanEvent) {
        container.daoManager.bannedUsers.add(event.user.idLong, event.guild.idLong)
    }
}