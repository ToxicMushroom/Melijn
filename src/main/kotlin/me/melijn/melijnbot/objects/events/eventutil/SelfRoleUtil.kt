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

        val emoteji = if (reaction.reactionEmote.isEmote) {
            reaction.reactionEmote.emote.id
        } else {
            reaction.reactionEmote.emoji
        }
        val map = daoManager.selfRoleWrapper.selfRoleCache.get(guildId).await()
        if (!map.containsKey(emoteji)) return null

        val roleId = map[emoteji] ?: return null
        if (daoManager.forceRoleWrapper.forceRoleCache[guildId].await()[member.idLong]?.contains(roleId) == true) return null
        val role = guild.getRoleById(roleId)
        if (role == null || !selfMember.canInteract(role)) {
            daoManager.selfRoleWrapper.remove(guildId, emoteji)
        }
        return role
    }
}