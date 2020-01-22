package me.melijn.melijnbot.objects.utils

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.moderation.getBanMessage
import me.melijn.melijnbot.database.autopunishment.Punishment
import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.PunishmentType
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyLogChannelByType
import net.dv8tion.jda.api.entities.Member

object PPUtils {

    suspend fun updatePP(member: Member, ppMap: Map<String, Long>, container: Container) {
        val guildId = member.guild.idLong
        val apWrapper = container.daoManager.autoPunishmentWrapper
        val oldPPMap = apWrapper.autoPunishmentCache.get(Pair(guildId, member.idLong)).await()
        apWrapper.set(guildId, member.idLong, ppMap)

        val apgWrapper = container.daoManager.autoPunishmentGroupWrapper
        val pgs = apgWrapper.autoPunishmentCache.get(guildId).await()

        val punishments = container.daoManager.punishmentWrapper.punishmentCache.get(guildId).await()
        for (pg in pgs) {
            val key = ppMap.keys.firstOrNull { key -> key == pg.groupName } ?: continue
            val newPoints = ppMap[key] ?: continue
            val oldPoints = oldPPMap.getOrDefault(pg.groupName, 0)
            val entries = pg.pointGoalMap.filter { (tp, p) -> tp > oldPoints && newPoints >= tp }


            for (entry in entries) {
                val punishment = punishments.first { punish -> punish.name == entry.value }
                applyPunishment(member, punishment, container)
                punishment.punishmentType
            }

            pg.pointGoalMap
        }
    }

    private suspend fun applyPunishment(member: Member, punishment: Punishment, container: Container) {
        when (punishment.punishmentType) {
            PunishmentType.BAN -> {
                val delDays = punishment.extraMap.getInt("delDays", 0)
                val duration = punishment.extraMap.getLong("duration", -1)
                val dull = if (duration == -1L) null else duration

                applyBan(member, punishment, container, delDays, dull)
            }
            PunishmentType.SOFTBAN -> {

            }
            PunishmentType.MUTE -> {

            }
            PunishmentType.KICK -> {

            }
            PunishmentType.WARN -> {

            }
        }
    }

    private suspend fun applyBan(member: Member, punishment: Punishment, container: Container, delDays: Int, duration: Long?) {
        val pc = member.user.openPrivateChannel().awaitOrNull()
        val jda = member.jda
        val guild = member.guild
        val lang = getLanguage(container.daoManager, member.idLong, guild.idLong)
        val banning = i18n.getTranslation(lang, "message.banning")
        val banningMessage = pc?.sendMessage(banning)?.awaitOrNull()

        val ban = Ban(
            guild.idLong,
            member.idLong,
            jda.selfUser.idLong,
            punishment.reason,
            null,
            null,
            System.currentTimeMillis(),
            null,
            true
        )

        container.daoManager.banWrapper.setBan(ban)
        val ex = member.ban(delDays, punishment.reason).awaitEX()
        if (ex != null) {
            val failed = i18n.getTranslation(lang, "message.banning.failed")
            banningMessage?.editMessage(failed)?.queue()
            return
        }

        val banMessageDM = getBanMessage(lang, guild, member.user, jda.selfUser, ban)
        val banMessageLog = getBanMessage(lang, guild, member.user, jda.selfUser, ban, true, member.user.isBot, banningMessage != null)
        banningMessage?.editMessage(banMessageDM)?.queue()

        val lcType = if (duration == null) LogChannelType.PERMANENT_BAN else LogChannelType.TEMP_BAN
        val channel = guild.getAndVerifyLogChannelByType(container.daoManager, lcType) ?: return
        sendEmbed(container.daoManager.embedDisabledWrapper, channel, banMessageLog)
    }
}