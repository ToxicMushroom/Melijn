package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.awaitOrNull
import me.melijn.melijnbot.objects.utils.checks.CANNOT_INTERACT_CAUSE
import me.melijn.melijnbot.objects.utils.checks.UNKNOWN_ID_CAUSE
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.objects.utils.getZoneId
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent

object SelfRoleUtil {

    suspend fun getSelectedSelfRoleNByReactionEvent(event: GenericGuildMessageReactionEvent, container: Container): List<Role>? {

        /* INIT */
        val guild = event.guild
        val member = event.member ?: guild.retrieveMemberById(event.userIdLong).awaitOrNull() ?: return null
        val channel = event.channel
        val channelId = channel.idLong
        val reaction = event.reaction
        val guildId = guild.idLong
        val selfMember = guild.selfMember
        val daoManager = container.daoManager

        if (!selfMember.canInteract(member)) return null
        /* END INIT */

        // val channelId = daoManager.channelWrapper.channelCache.get(Pair(guildId, ChannelType.SELFROLE)).await()
        // if (channelId != channel.idLong) return null

        val emoteji = if (reaction.reactionEmote.isEmote) {
            reaction.reactionEmote.emote.id
        } else {
            reaction.reactionEmote.emoji
        }

        val selfRoleGroups = daoManager.selfRoleGroupWrapper.getMap(guildId).await()
        val selfRoleGroupMatches = selfRoleGroups.filter {
            it.channelId == channelId && it.messageIds.contains(event.messageIdLong)
        }

        if (selfRoleGroupMatches.isEmpty()) {
            return null
        }

        val roleIds = mutableListOf<Long>()

        val roles = mutableListOf<Role>()
        val selfRoles = daoManager.selfRoleWrapper.selfRoleCache.get(guildId).await()
        for ((groupName) in selfRoleGroupMatches) {
            val subMap = selfRoles[groupName] ?: continue
            for (ls in subMap.values) {
                if (ls.isEmpty()) return null
                for (roleId in ls) {
                    if (daoManager.forceRoleWrapper.forceRoleCache[guildId].await()[member.idLong]?.contains(roleId) == true) return null
                    val role = guild.getRoleById(roleId)
                    val language = getLanguage(daoManager, -1, guildId)
                    val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
                    val zoneId = getZoneId(daoManager, guildId)

                    var shouldRemove = false
                    var cause = ""
                    var causeArg = ""
                    if (role == null) {
                        cause = UNKNOWN_ID_CAUSE
                        causeArg = roleId.toString()
                        shouldRemove = true
                    } else if (!selfMember.canInteract(role)) {
                        cause = CANNOT_INTERACT_CAUSE
                        causeArg = roleId.toString()
                        shouldRemove = true
                    }

                    if (shouldRemove) {
                        daoManager.selfRoleWrapper.remove(guildId, groupName, emoteji)
                        LogUtils.sendRemovedSelfRoleLog(language, zoneId, emoteji, logChannel, cause, causeArg)
                    }
                    role?.let { roles.add(it) }
                }
            }
        }

        if (roles.isEmpty()) return null
        return roles
    }
}