package me.melijn.melijnbot.internals.events.eventutil

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.CANNOT_INTERACT_CAUSE
import me.melijn.melijnbot.internals.utils.checks.UNKNOWN_ID_CAUSE
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.getZoneId
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import kotlin.random.Random

object SelfRoleUtil {

    suspend fun getSelectedSelfRoleNByReactionEvent(
        event: GenericGuildMessageReactionEvent,
        container: Container
    ): List<Role>? {

        /* INIT */
        val guild = event.guild
        val member = event.member ?: guild.retrieveMemberById(event.userIdLong).awaitOrNull() ?: return null
        val channel = event.channel
        val channelId = channel.idLong
        val reaction = event.reaction
        val guildId = guild.idLong
        val selfMember = guild.selfMember
        val daoManager = container.daoManager

        if (!selfMember.hasPermission(Permission.MANAGE_ROLES)) return null
        /* END INIT */

        val emoteji = if (reaction.reactionEmote.isEmote) {
            reaction.reactionEmote.emote.id
        } else {
            reaction.reactionEmote.emoji
        }

        val selfRoleGroups = daoManager.selfRoleGroupWrapper.getMap(guildId)
        val selfRoleGroupMatches = selfRoleGroups.filter {
            it.channelId == channelId &&
                (it.messageIds.contains(event.messageIdLong) || it.messageIds.isEmpty())
                && it.isEnabled
        }

        if (selfRoleGroupMatches.isEmpty()) {
            return null
        }


        val roles = mutableListOf<Role>()
        val selfRoles = daoManager.selfRoleWrapper.getMap(guildId)
        for (group in selfRoleGroupMatches) {
            val groupRoles = mutableListOf<Role>()
            val groupName = group.groupName
            val subMap = selfRoles[groupName] ?: continue

            for (i in 0 until subMap.length()) {
                val dataEntry = subMap.getArray(i)

                val emotejiEntry = dataEntry.getString(0)
                if (emotejiEntry != emoteji) continue


                val hasPermission = hasPermission(
                    container,
                    member,
                    event.channel,
                    "rr.${groupName.lowercase()}.$emoteji",
                    null,
                    group.requiresPermission
                )
                if (!hasPermission) continue

                val roleDataArr = dataEntry.getArray(2)

                val shouldAll = dataEntry.getBoolean(3)

                var coolIndex = -1

                if (!shouldAll) {
                    if (roleDataArr.length() == 1) {
                        coolIndex = 0
                    } else {
                        // More then 2 roles for this emoteji -> start lottery
                        var range = 0
                        for (j in 0 until roleDataArr.length()) {
                            range += roleDataArr.getArray(j).getInt(0)
                        }
                        val winner = Random.nextInt(range)
                        range = 0

                        for (j in 0 until roleDataArr.length()) {
                            val bool1 = (range <= winner)
                            range += roleDataArr.getArray(j).getInt(0)
                            val bool2 = (range <= winner)
                            if (bool1 && !bool2) {
                                coolIndex = j
                                break
                            }
                        }
                    }
                }


                for (j in 0 until roleDataArr.length()) {
                    val roleData = roleDataArr.getArray(j)
                    val roleId = roleData.getLong(1)
                    val notWon = j != coolIndex
                    if (!shouldAll && notWon) { // check if this role has won, role if we should get all roles -> when both are false we return
                        continue
                    }

                    if (daoManager.forceRoleWrapper.getForceRoles(guildId)[member.idLong]?.contains(roleId) == true) continue
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
                        continue
                    }

                    role?.let { groupRoles.add(it) }
                }
            }
            if (event is GuildMessageReactionAddEvent && group.limitToOneRole) {
                for (l in 0 until subMap.length()) {
                    val dataEntry2 = subMap.getArray(l)
                    val emotejiEntry2 = dataEntry2.getString(0)
                    if (emotejiEntry2 == emoteji) continue
                    val roleDataArr2 = dataEntry2.getArray(2)
                    val shouldAll2 = dataEntry2.getBoolean(3)

                    var hasRoles = 0
                    for (m in 0 until roleDataArr2.length()) {
                        val roleId = roleDataArr2.getArray(m).getLong(1)
                        hasRoles += if (member.roles.any { role -> role.idLong == roleId }) 1 else 0
                    }

                    val alreadyHasRole = if (shouldAll2) hasRoles == roleDataArr2.length() else hasRoles > 0
                    if (alreadyHasRole) {
                        groupRoles.clear()
                        break
                    }
                }
            }
            roles.addAll(groupRoles)
        }

        if (roles.isEmpty()) return null
        return roles
    }
}