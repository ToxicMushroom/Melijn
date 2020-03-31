package me.melijn.melijnbot.objects.events.eventlisteners

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.objects.events.AbstractListener
import me.melijn.melijnbot.objects.events.eventutil.JoinLeaveUtil
import me.melijn.melijnbot.objects.events.eventutil.JoinLeaveUtil.joinRole
import me.melijn.melijnbot.objects.utils.VerificationUtils
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelByType
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent

class JoinLeaveListener(container: Container) : AbstractListener(container) {

    override fun onEvent(event: GenericEvent) {
        if (event is GuildMemberJoinEvent) onGuildMemberJoin(event)
        else if (event is GuildMemberRemoveEvent) onGuildMemberLeave(event)
    }

    private fun onGuildMemberJoin(event: GuildMemberJoinEvent) = CoroutineScope(Dispatchers.Default).launch {
        val daoManager = container.daoManager
        val member = event.member
        JoinLeaveUtil.reAddMute(daoManager, event)

        if (guildHasNoVerification(event)) {
            JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, member.user, ChannelType.JOIN, MessageType.JOIN)
            JoinLeaveUtil.forceRole(daoManager, member)
            joinRole(daoManager, member)

        } else {
            VerificationUtils.addUnverified(member, daoManager)

        }
    }

    private suspend fun guildHasNoVerification(event: GuildMemberJoinEvent): Boolean {
        val channel = event.guild.getAndVerifyChannelByType(container.daoManager, ChannelType.VERIFICATION)
        return channel == null
    }

    private fun onGuildMemberLeave(event: GuildMemberRemoveEvent) = CoroutineScope(Dispatchers.Default).launch {
        val daoManager = container.daoManager
        val user = event.user

        if (!daoManager.unverifiedUsersWrapper.contains(event.guild.idLong, user.idLong)) {
            JoinLeaveUtil.postWelcomeMessage(daoManager, event.guild, user, ChannelType.LEAVE, MessageType.LEAVE)
        }
    }
}