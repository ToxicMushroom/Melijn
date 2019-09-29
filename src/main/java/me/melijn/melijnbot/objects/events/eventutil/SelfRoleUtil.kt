package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.ChannelType
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent

object SelfRoleUtil {
    suspend fun getSelectedSelfRoleNByReactionEvent(event: GenericGuildMessageReactionEvent, container: Container): Role? {
        val guild = event.guild
        val member = event.member
        val channel = event.channel
        val reaction = event.reaction
        val guildId = guild.idLong
        val selfMember = guild.selfMember
        val daoManager = container.daoManager

        if (!selfMember.canInteract(member)) return null
        val channelId = daoManager.channelWrapper.channelCache.get(Pair(guildId, ChannelType.SELFROLE)).await()
        if (channelId != channel.idLong) return null

        val reactionEmoteId = reaction.reactionEmote.idLong
        val map = daoManager.selfRoleWrapper.selfRoleCache.get(guildId).await()
        if (!map.containsKey(reactionEmoteId)) return null

        val roleId = map[reactionEmoteId] ?: return null
        val role = guild.getRoleById(roleId)
        if (role == null || !selfMember.canInteract(role)) {
            daoManager.selfRoleWrapper.remove(guildId, roleId)
        }
        return role
    }
}