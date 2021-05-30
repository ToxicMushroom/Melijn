package me.melijn.melijnbot.internals.utils.checks

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.getZoneId
import me.melijn.melijnbot.internals.utils.toUCSC
import me.melijn.melijnbot.internals.utils.toUpperWordCase
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel

const val UNKNOWN_ID_CAUSE = "unknownid"
const val CANNOT_INTERACT_CAUSE = "cannotinteract"
const val CANNOT_ASSIGN_CAUSE = "cannotassign"
private const val NO_PERM_CAUSE = "nopermission"

suspend fun Guild.getAndVerifyLogChannelByType(
    daoManager: DaoManager,
    type: LogChannelType,
    logIfInvalid: Boolean = true
): TextChannel? {
    val logChannelWrapper = daoManager.logChannelWrapper
    val channelId = logChannelWrapper.getChannelId(idLong, type)

    return this.getAndVerifyLogChannelById(daoManager, type, channelId, logIfInvalid)
}

suspend fun Guild.getAndVerifyLogChannelById(
    daoManager: DaoManager,
    type: LogChannelType,
    channelId: Long,
    logIfInvalid: Boolean = true
): TextChannel? {
    val logChannelWrapper = daoManager.logChannelWrapper
    val textChannel = getTextChannelById(channelId)
    var shouldRemove = false
    var cause = ""
    var causeArg = ""
    if (channelId != -1L && textChannel == null) {
        cause = UNKNOWN_ID_CAUSE
        causeArg = channelId.toString()
        shouldRemove = true
    }
    if (textChannel == null) return null
    if (!textChannel.canTalk()) {
        cause = NO_PERM_CAUSE
        causeArg = Permission.MESSAGE_WRITE.toUCSC()
        shouldRemove = true
    }

    if (shouldRemove) {
        logChannelWrapper.removeChannel(this.idLong, type)
        if (logIfInvalid) {
            val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT, false)
            val language = getLanguage(daoManager, -1, this.idLong)
            val zoneId = getZoneId(daoManager, textChannel.guild.idLong)
            LogUtils.sendRemovedLogChannelLog(language, zoneId, type, logChannel, cause, causeArg)
        }
        return null
    }
    return textChannel
}

suspend fun Guild.getAndVerifyChannelByType(
    daoManager: DaoManager,
    type: ChannelType,
    vararg requiredPerms: Permission
): TextChannel? {
    val channelWrapper = daoManager.channelWrapper
    val channelId = channelWrapper.getChannelId(this.idLong, type)

    return this.getAndVerifyChannelById(daoManager, type, channelId, requiredPerms.toSet())
}

suspend fun Guild.getAndVerifyChannelById(
    daoManager: DaoManager,
    type: ChannelType,
    channelId: Long,
    requiredPerms: Set<Permission> = emptySet()
): TextChannel? {
    val textChannel = getTextChannelById(channelId)
    val selfMember = this.selfMember
    var shouldRemove = false
    val zoneId = getZoneId(daoManager, this.idLong)

    if (channelId != -1L && textChannel == null) {
        val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
        val language = getLanguage(daoManager, -1, this.idLong)
        LogUtils.sendRemovedChannelLog(language, zoneId, type, logChannel, UNKNOWN_ID_CAUSE, channelId.toString())
        shouldRemove = true
    }

    for (perm in requiredPerms) {
        if (shouldRemove || textChannel == null) break
        if (!selfMember.hasPermission(textChannel, perm)) {
            val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
            val language = getLanguage(daoManager, -1, this.idLong)
            LogUtils.sendRemovedChannelLog(language, zoneId, type, logChannel, NO_PERM_CAUSE, perm.toUCSC())
            shouldRemove = true
        }
    }

    if (shouldRemove) {
        daoManager.channelWrapper.removeChannel(this.idLong, type)
        return null
    }

    return textChannel
}

suspend fun Guild.getAndVerifyMusicChannel(
    daoManager: DaoManager,
    vararg requiredPerms: Permission
): VoiceChannel? {
    val channelWrapper = daoManager.musicChannelWrapper
    val zoneId = getZoneId(daoManager, this.idLong)
    val channelId = channelWrapper.getChannel(this.idLong)
    val voiceChannel = getVoiceChannelById(channelId)
    val selfMember = this.selfMember

    var shouldRemove = false
    if (channelId != -1L && voiceChannel == null) {
        shouldRemove = true
        val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
        val language = getLanguage(daoManager, -1, this.idLong)
        LogUtils.sendRemovedMusicChannelLog(language, zoneId, logChannel, UNKNOWN_ID_CAUSE, channelId.toString())
    }

    for (perm in requiredPerms) {
        if (shouldRemove || voiceChannel == null) break
        if (!selfMember.hasPermission(voiceChannel, perm)) {
            shouldRemove = true
            val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
            val language = getLanguage(daoManager, -1, this.idLong)
            LogUtils.sendRemovedMusicChannelLog(
                language,
                zoneId,
                logChannel,
                NO_PERM_CAUSE,
                perm.toString().toUpperWordCase()
            )
        }
    }

    if (shouldRemove) {
        channelWrapper.removeChannel(this.idLong)
        return null
    }

    return voiceChannel
}

suspend fun Guild.getAndVerifyRoleByType(
    daoManager: DaoManager,
    type: RoleType,
    shouldBeInteractable: Boolean = false
): Role? {
    val roleWrapper = daoManager.roleWrapper
    val roleId = roleWrapper.getRoleId(idLong, type)
    if (roleId == -1L) return null

    return this.getAndVerifyRoleById(daoManager, type, roleId, shouldBeInteractable)
}

suspend fun Guild.getAndVerifyRoleById(
    daoManager: DaoManager,
    roleType: RoleType,
    roleId: Long,
    shouldBeInteractable: Boolean = false,
    canAssignRole: Boolean = false
): Role? {
    val role = getRoleById(roleId)
    var shouldRemove = false
    var cause = ""
    var causeArg = ""
    if (role == null) {
        cause = UNKNOWN_ID_CAUSE
        causeArg = roleId.toString()
        shouldRemove = true
    } else if (shouldBeInteractable && !selfMember.canInteract(role)) {
        cause = CANNOT_INTERACT_CAUSE
        shouldRemove = true
    } else if (canAssignRole && !selfMember.hasPermission(Permission.MANAGE_ROLES)) {
        cause = NO_PERM_CAUSE
        causeArg = Permission.MANAGE_ROLES.name
        shouldRemove = true
    }

    if (shouldRemove) {
        val language = getLanguage(daoManager, -1, this.idLong)
        val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
        val zoneId = getZoneId(daoManager, this.idLong)
        LogUtils.sendRemovedRoleLog(language, zoneId, roleType, logChannel, cause, causeArg)
        daoManager.roleWrapper.removeRole(this.idLong, roleType)
        return null
    }

    return role
}