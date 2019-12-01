package me.melijn.melijnbot.objects.utils.checks

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.toUCSC
import me.melijn.melijnbot.objects.utils.toUpperWordCase
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel

private const val UNKNOWN_ID_CAUSE = "unknownid"
private const val CANNOT_INTERACT_CAUSE = "cannotinteract"
private const val NO_PERM_CAUSE = "nopermission"

suspend fun Guild.getAndVerifyLogChannelByType(daoManager: DaoManager, type: LogChannelType, logIfInvalid: Boolean = true): TextChannel? {
    val logChannelWrapper = daoManager.logChannelWrapper
    val channelId = logChannelWrapper.logChannelCache.get(Pair(idLong, type)).await()
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
            LogUtils.sendRemovedLogChannelLog(language, type, logChannel, cause, causeArg)
        }
        return null
    }
    return textChannel
}

suspend fun Guild.getAndVerifyChannelByType(
    type: ChannelType,
    daoManager: DaoManager,
    vararg requiredPerms: Permission
): TextChannel? {
    val channelWrapper = daoManager.channelWrapper
    val channelId = channelWrapper.channelCache.get(Pair(idLong, type)).await()
    val textChannel = getTextChannelById(channelId)
    val selfMember = this.selfMember
    var shouldRemove = false

    if (channelId != -1L && textChannel == null) {
        val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
        val language = getLanguage(daoManager, -1, this.idLong)
        LogUtils.sendRemovedChannelLog(language, type, logChannel, UNKNOWN_ID_CAUSE, channelId.toString())
        shouldRemove = true
    }

    for (perm in requiredPerms) {
        if (shouldRemove || textChannel == null) break
        if (!selfMember.hasPermission(textChannel, perm)) {
            val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
            val language = getLanguage(daoManager, -1, this.idLong)
            LogUtils.sendRemovedChannelLog(language, type, logChannel, NO_PERM_CAUSE, perm.toUCSC())
            shouldRemove = true
        }
    }

    if (shouldRemove) {
        channelWrapper.removeChannel(this.idLong, type)
        return null
    }

    return textChannel
}

suspend fun Guild.getAndVerifyMusicChannel(
    daoManager: DaoManager,
    vararg requiredPerms: Permission
): VoiceChannel? {
    val channelWrapper = daoManager.musicChannelWrapper
    val channelId = channelWrapper.musicChannelCache.get(idLong).await()
    val voiceChannel = getVoiceChannelById(channelId)
    val selfMember = this.selfMember


    var shouldRemove = false
    if (channelId != -1L && voiceChannel == null) {
        shouldRemove = true
        val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
        val language = getLanguage(daoManager, -1, this.idLong)
        LogUtils.sendRemovedMusicChannelLog(language, logChannel, UNKNOWN_ID_CAUSE, channelId.toString())
    }

    for (perm in requiredPerms) {
        if (shouldRemove || voiceChannel == null) break
        if (!selfMember.hasPermission(voiceChannel, perm)) {
            shouldRemove = true
            val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
            val language = getLanguage(daoManager, -1, this.idLong)
            LogUtils.sendRemovedMusicChannelLog(language, logChannel, NO_PERM_CAUSE, perm.toString().toUpperWordCase())
        }
    }

    if (shouldRemove) {
        channelWrapper.removeChannel(this.idLong)
        return null
    }

    return voiceChannel
}

suspend fun Guild.getAndVerifyRoleByType(daoManager: DaoManager, type: RoleType, shouldBeInteractable: Boolean = false): Role? {
    val roleWrapper = daoManager.roleWrapper
    val channelId = roleWrapper.roleCache.get(Pair(idLong, type)).await()
    if (channelId == -1L) return null

    val role = getRoleById(channelId)
    var shouldRemove = false
    var cause = ""
    var causeArg = ""
    if (role == null) {
        cause = UNKNOWN_ID_CAUSE
        causeArg = channelId.toString()
        shouldRemove = true
    } else if (shouldBeInteractable && !selfMember.canInteract(role)) {
        cause = CANNOT_INTERACT_CAUSE
        shouldRemove = true
    }

    if (shouldRemove) {
        val language = getLanguage(daoManager, -1, this.idLong)
        val logChannel = this.getAndVerifyLogChannelByType(daoManager, LogChannelType.BOT)
        LogUtils.sendRemovedRoleLog(language, type, logChannel, cause, causeArg)
        roleWrapper.removeRole(this.idLong, type)
        return null
    }

    return role
}