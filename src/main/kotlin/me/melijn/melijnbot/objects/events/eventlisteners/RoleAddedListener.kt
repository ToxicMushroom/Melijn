package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.await
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent

class RoleAddedListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) = runBlocking {
        if (event is GuildMemberRoleAddEvent) {
            logRoleAdded(event)
        }
    }

    private suspend fun logRoleAdded(event: GuildMemberRoleAddEvent) {
        val auditLog = event.guild.retrieveAuditLogs()
            .type(ActionType.MEMBER_ROLE_UPDATE)
            .limit(1)
            .await()
            .firstOrNull { entry -> entry.targetId == event.user.id } ?: return
        val adder = auditLog.user ?: return

        for (role in event.roles) {
            LogUtils.sendRoleAddedLog(container, adder, event.user, role)
        }
    }
}