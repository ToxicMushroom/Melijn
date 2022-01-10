package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.moderation.getPunishMessage
import me.melijn.melijnbot.database.autopunishment.ExpireTime
import me.melijn.melijnbot.database.autopunishment.Points
import me.melijn.melijnbot.database.autopunishment.PunishGroup
import me.melijn.melijnbot.database.autopunishment.Punishment
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.database.ban.SoftBan
import me.melijn.melijnbot.database.kick.Kick
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.database.warn.Warn
import me.melijn.melijnbot.enums.*
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendMsgAwaitN
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.restaction.MessageAction

object PPUtils {

    // Updates the punishment points of the user and checks for new punishment point goal hits, then applies the goals if hit
    suspend fun updatePP(
        member: Member,
        extraPPMap: Map<List<String>, Points>,
        container: Container,
        type: PointsTriggerType
    ) {
        val guildId = member.guild.idLong
        val daoManager = container.daoManager
        val apWrapper = daoManager.autoPunishmentWrapper
        val oldPPMap = apWrapper.getPointsMap(guildId, member.idLong).toMutableMap()

        val newPPMap = mutableMapOf<ExpireTime, Map<String, Points>>()
        newPPMap.putAll(oldPPMap)

        val apgWrapper = daoManager.autoPunishmentGroupWrapper

        val pgs = apgWrapper.getList(guildId).filter { // Only need to give points for enabled punishGroups
            it.enabledTypes.contains(type)
        }

        val punishments = daoManager.punishmentWrapper.getList(guildId)
        for (pg: PunishGroup in pgs) {
            val absExpireTime = if (pg.expireTime == 0L) {
                0
            } else {
                pg.expireTime + System.currentTimeMillis()
            }

            for ((pgNames, extraPoints) in extraPPMap) {
                if (!pgNames.contains(pg.groupName)) continue

                val oldPoints = oldPPMap.values.sumOf {
                    it[pg.groupName]?.let { points: Points ->
                        points
                    } ?: 0
                }

                val entries = pg.pointGoalMap.filter { (tp, _) ->
                    tp in (oldPoints + 1)..(oldPoints + extraPoints)
                }

                for (entry in entries) {
                    val punishment = punishments.first { (name) ->
                        name == entry.value
                    }
                    applyPunishment(member, punishment, container)
                }

                val processing = newPPMap[absExpireTime]?.toMutableMap()
                if (processing != null) {
                    val points = (processing[pg.groupName] ?: 0) + extraPoints
                    processing[pg.groupName] = points
                    newPPMap[absExpireTime] = processing
                } else {
                    newPPMap[absExpireTime] = mapOf(pg.groupName to extraPoints)
                }
            }
        }

        val actuallyNew = newPPMap.filter { entry ->
            !oldPPMap.containsKey(entry.key) || oldPPMap[entry.key] != entry.value
        }

        apWrapper.set(guildId, member.idLong, actuallyNew)
    }

    // Applies the correct punishment to the member
    private suspend fun applyPunishment(member: Member, punishment: Punishment, container: Container) {
        when (punishment.punishmentType) {
            // TODO Permission checks and logging in case of missing stuff
            PunishmentType.BAN -> {
                val delDays = punishment.extraMap.getInt("delDays", 0)
                val duration = punishment.extraMap.getLong("duration", -1)
                val dull = if (duration == -1L) null else duration

                applyBan(member, punishment, container, delDays, dull)
            }
            PunishmentType.SOFTBAN -> {
                val delDays = punishment.extraMap.getInt("delDays", 7)

                applySoftBan(member, punishment, container, delDays)
            }
            PunishmentType.MUTE -> {
                val duration = punishment.extraMap.getLong("duration", -1)
                val dull = if (duration == -1L) null else duration

                applyMute(member, punishment, container, dull)
            }
            PunishmentType.KICK -> {
                applyKick(member, punishment, container)
            }
            PunishmentType.WARN -> {
                applyWarn(member, punishment, container)
            }
            PunishmentType.ADDROLE -> {
                val duration = punishment.extraMap.getLong("duration", -1)
                val dull = if (duration == -1L) null else duration
                val roleId = punishment.extraMap.getLong("role", -1)
                if (roleId == -1L) return

                applyAddRole(member, punishment, container, dull, roleId)
            }
            PunishmentType.REMOVEROLE -> {
                val duration = punishment.extraMap.getLong("duration", -1)
                val dull = if (duration == -1L) null else duration
                val roleId = punishment.extraMap.getLong("role", -1)
                if (roleId == -1L) return

                applyRemoveRole(member, punishment, container, dull, roleId)
            }
        }
    }

    private suspend fun applyRemoveRole(
        member: Member,
        punishment: Punishment,
        container: Container,
        duration: Long?,
        roleId: Long
    ) {
        val guild = member.guild
        val daoManager = container.daoManager
        val wrapper = daoManager.tempRoleWrapper
        val role = guild.getRoleById(roleId) ?: return

        val success = guild.removeRoleFromMember(member, role).reason("punish role").awaitBool()
        if (duration != null && success) {
            wrapper.addTempRole(guild.idLong, member.idLong, roleId, duration, false)
        }
    }

    private suspend fun applyAddRole(
        member: Member,
        punishment: Punishment,
        container: Container,
        duration: Long?,
        roleId: Long
    ) {
        val guild = member.guild
        val daoManager = container.daoManager
        val wrapper = daoManager.tempRoleWrapper
        val role = guild.getRoleById(roleId) ?: return

        val success = guild.addRoleToMember(member, role).reason("punish role").awaitBool()
        if (duration != null && success) {
            wrapper.addTempRole(guild.idLong, member.idLong, roleId, duration, true)
        }
    }

    private suspend fun applyBan(
        member: Member,
        punishment: Punishment,
        container: Container,
        delDays: Int,
        duration: Long?
    ) {
        val banned = member.user
        val jda = member.jda
        val guild = member.guild
        val banAuthor = jda.selfUser
        val daoManager = container.daoManager
        val lang = getLanguage(daoManager, member.idLong, guild.idLong)
        val banning = i18n.getTranslation(lang, "message.banning")
        val banningMessage = banned.openPrivateChannel().awaitOrNull()?.sendMessage(banning)?.awaitOrNull()

        val ban = Ban(
            guild.idLong,
            member.idLong,
            banAuthor.idLong,
            punishment.reason,
            null,
            null,
            System.currentTimeMillis(),
            duration?.times(1000)?.plus(System.currentTimeMillis()),
            true
        )

        daoManager.banWrapper.setBan(ban)
        val ex = member.ban(delDays, punishment.reason).reason("punish ban").awaitEX()
        if (ex != null) {
            val failed = i18n.getTranslation(lang, "message.banning.failed")
            banningMessage?.editMessage(failed)?.queue()
            return
        }

        val banType = if (duration == null) MessageType.BAN else MessageType.TEMP_BAN
        val banLogType = if (duration == null) MessageType.BAN_LOG else MessageType.TEMP_BAN_LOG
        val banMessageDM = getPunishMessage(lang, daoManager, guild, banned, banAuthor, ban, msgType = banType)
        val banMessageLog = getPunishMessage(lang, daoManager, guild, banned, banAuthor, ban, true,
            banningMessage != null, banLogType)

        banningMessage?.editMessage(banMessageDM)?.override(true)?.queue()

        val lcType = if (duration == null) LogChannelType.PERMANENT_BAN else LogChannelType.TEMP_BAN
        val channel = guild.getAndVerifyLogChannelByType(daoManager, lcType) ?: return
        sendMsg(channel, container.webManager.proxiedHttpClient, banMessageLog)
    }

    private suspend fun applySoftBan(member: Member, punishment: Punishment, container: Container, delDays: Int) {
        val target = member.user
        val guild = member.guild
        val melijn = member.jda.selfUser
        val daoManager = container.daoManager
        val lang = getLanguage(daoManager, member.idLong, guild.idLong)
        val softBanning = i18n.getTranslation(lang, "message.softbanning")
        val softBanningMessage = target.openPrivateChannel().awaitOrNull()?.sendMessage(softBanning)?.awaitOrNull()

        val softBan = SoftBan(
            guild.idLong,
            member.idLong,
            melijn.idLong,
            punishment.reason,
            System.currentTimeMillis()
        )

        daoManager.softBanWrapper.addSoftBan(softBan)
        val ex = member.ban(delDays, punishment.reason).awaitEX()
        if (ex != null) {
            val failed = i18n.getTranslation(lang, "message.softbanning.failed")
            softBanningMessage?.editMessage(failed)?.queue()
            return
        }

        guild.unban(target).reason("(softBan) ${melijn.asTag}: ${softBan.reason}").queue()

        val softBanMessageDM = getPunishMessage(lang, daoManager, guild, target, melijn, softBan, msgType = MessageType.SOFT_BAN)
        val softBanMessageLog = getPunishMessage(lang, daoManager, guild, target, melijn, softBan,
            true, softBanningMessage != null, msgType = MessageType.SOFT_BAN_LOG)
        softBanningMessage?.editMessage(softBanMessageDM)?.override(true)?.queue()

        val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.SOFT_BAN) ?: return
        sendMsg(channel, container.webManager.proxiedHttpClient, softBanMessageLog)
    }

    private suspend fun applyMute(member: Member, punishment: Punishment, container: Container, duration: Long?) {
        val jda = member.jda
        val guild = member.guild
        val daoManager = container.daoManager
        val lang = getLanguage(daoManager, member.idLong, guild.idLong)
        val httpClient = container.webManager.proxiedHttpClient

        val mute = Mute(
            guild.idLong,
            member.idLong,
            jda.selfUser.idLong,
            punishment.reason,
            null,
            null,
            System.currentTimeMillis(),
            duration?.times(1000)?.plus(System.currentTimeMillis()),
            true
        )

        daoManager.muteWrapper.setMute(mute)
        val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE, true) ?: return
        guild.addRoleToMember(member, muteRole).reason("muted").await()

        val muteMessageDM = getPunishMessage(lang, daoManager, guild, member.user, jda.selfUser, mute, msgType = MessageType.MUTE)
        val pc = member.user.openPrivateChannel().awaitOrNull()
        val mutedMessage = pc?.let { sendMsgAwaitN(it, httpClient, muteMessageDM) }

        val muteMessageLog = getPunishMessage(
            lang,
            daoManager,
            guild,
            member.user,
            jda.selfUser,
            mute,
            true,
            mutedMessage != null,
            MessageType.MUTE_LOG
        )

        val lcType = if (duration == null) LogChannelType.PERMANENT_MUTE else LogChannelType.TEMP_MUTE
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, lcType) ?: return
        sendMsg(logChannel, httpClient, muteMessageLog)
    }

    private suspend fun applyKick(member: Member, punishment: Punishment, container: Container) {
        val pc = member.user.openPrivateChannel().awaitOrNull()
        val jda = member.jda
        val guild = member.guild
        val daoManager = container.daoManager
        val lang = getLanguage(daoManager, member.idLong, guild.idLong)
        val kicking = i18n.getTranslation(lang, "message.kicking")
        val kickingMessage = pc?.sendMessage(kicking)?.awaitOrNull()

        val kick = Kick(
            guild.idLong,
            member.idLong,
            jda.selfUser.idLong,
            punishment.reason
        )

        daoManager.kickWrapper.addKick(kick)
        val ex = member.kick(punishment.reason).awaitEX()
        if (ex != null) {
            val failed = i18n.getTranslation(lang, "message.kicking.failed")
            kickingMessage?.editMessage(failed)?.queue()
            return
        }

        val kickMessageDM = getPunishMessage(lang, daoManager, guild, member.user, jda.selfUser, kick, msgType = MessageType.KICK)
        val kickMessageLog = getPunishMessage(
            lang,
            daoManager,
            guild,
            member.user,
            jda.selfUser,
            kick,
            true,
            kickingMessage != null
        , msgType = MessageType.KICK_LOG
        )
        kickingMessage?.editMessage(kickMessageDM)?.override(true)?.queue()

        val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.KICK) ?: return
        sendMsg(channel, container.webManager.proxiedHttpClient, kickMessageLog)
    }

    private suspend fun applyWarn(member: Member, punishment: Punishment, container: Container) {
        val jda = member.jda
        val guild = member.guild
        val daoManager = container.daoManager
        val lang = getLanguage(daoManager, member.idLong, guild.idLong)

        val warn = Warn(
            guild.idLong,
            member.idLong,
            jda.selfUser.idLong,
            punishment.reason
        )

        daoManager.warnWrapper.addWarn(warn)

        val warnMessageDM = getPunishMessage(lang, daoManager, guild, member.user, jda.selfUser, warn, msgType = MessageType.WARN)
        val pc = member.user.openPrivateChannel().awaitOrNull()
        val kickedMessage = pc?.let { sendMsg(it, container.webManager.proxiedHttpClient, warnMessageDM) }

        val warnMessageLog = getPunishMessage(
            lang,
            daoManager,
            guild,
            member.user,
            jda.selfUser,
            warn,
            true,
            kickedMessage != null,
            msgType = MessageType.WARN_LOG
        )

        val channel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.KICK) ?: return
        sendMsg( channel, container.webManager.proxiedHttpClient, warnMessageLog)
    }
}

fun Message.editMessage(modular: ModularMessage, default: String = "Unsendable Message"): MessageAction {
    return modular.toMessage()?.let {
        this.editMessage(it)
    } ?: this.editMessage(
        MessageBuilder().apply {
            setContent(default)
        }.build()
    )
}
