package me.melijn.melijnbot.internals.services.mutes

import io.ktor.client.*
import me.melijn.melijnbot.commands.moderation.getUnTempPunishMessage
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.mute.Mute
import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.enums.MessageType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.utils.awaitEX
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyLogChannelByType
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.internals.utils.message.sendMsg
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit

class MuteService(
    val shardManager: ShardManager,
    val daoManager: DaoManager,
    val podInfo: PodInfo,
    val proxiedHttpClient: HttpClient
) : Service("Mute", 1000, 1100, TimeUnit.MILLISECONDS) {

    override val service = RunnableTask {
        val mutes = daoManager.muteWrapper.getUnmuteableMutes(podInfo)
        for (mute in mutes) {
            val selfUser = shardManager.shards[0].selfUser
            val newMute = mute.run {
                Mute(
                    guildId,
                    mutedId,
                    muteAuthorId,
                    reason,
                    selfUser.idLong,
                    "Mute expired",
                    startTime,
                    endTime,
                    false,
                    muteId
                )
            }

            daoManager.muteWrapper.setMute(newMute)
            val guild = shardManager.getGuildById(mute.guildId) ?: continue

            val muteRole = guild.getAndVerifyRoleByType(daoManager, RoleType.MUTE, true)
            val author = newMute.muteAuthorId?.let { shardManager.retrieveUserById(it).awaitOrNull() }
            val muted = shardManager.retrieveUserById(newMute.mutedId).awaitOrNull() ?: continue


            val roleRemoveError = if (muteRole != null) this.run {
                val mutedMember = guild.retrieveMember(muted).awaitOrNull() ?: return@run null
                guild
                    .removeRoleFromMember(mutedMember, muteRole)
                    .reason("MuteService: mute expired")
                    .awaitEX()
            } else null

            createAndSendUnmuteMessage(guild, selfUser, muted, author, newMute, roleRemoveError)
        }
    }

    // Sends unban message to tempban logchannel and the unbanned user
    private suspend fun createAndSendUnmuteMessage(
        guild: Guild,
        unmuteAuthor: User,
        mutedUser: User,
        muteAuthor: User?,
        mute: Mute,
        roleRemoveError: Throwable?
    ) {
        val language = getLanguage(daoManager, -1, guild.idLong)
        val logChannel = guild.getAndVerifyLogChannelByType(daoManager, LogChannelType.UNMUTE) ?: return

        val msgLc = getUnTempPunishMessage(
            language,
            daoManager,
            guild,
            mutedUser,
            muteAuthor,
            unmuteAuthor,
            mute,
            true,
            mutedUser.isBot,
            MessageType.UNMUTE_LOG,
            roleRemoveError?.message
        )

        sendMsg(logChannel, proxiedHttpClient, msgLc)
    }
}