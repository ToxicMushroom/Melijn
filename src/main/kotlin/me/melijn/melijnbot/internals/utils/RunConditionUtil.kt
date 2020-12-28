package me.melijn.melijnbot.internals.utils

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.utility.VOTE_URL
import me.melijn.melijnbot.enums.Environment
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.command.hasPermission
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.message.getNicerUsedPrefix
import me.melijn.melijnbot.internals.utils.message.sendMsg
import me.melijn.melijnbot.internals.utils.message.sendRspOrMsg
import net.dv8tion.jda.api.entities.Message

object RunConditionUtil {

    /**
     * [return] returns true if the check passed
     *
     * **/
    suspend fun runConditionCheckPassed(
        container: Container,
        runCondition: RunCondition,
        message: Message,
        command: AbstractCommand,
        commandParts: List<String>
    ): Boolean {
        val userId = message.author.idLong
        val language = getLanguage(container.daoManager, userId, if (message.isFromGuild) message.guild.idLong else -1)
        val prefix = getNicerUsedPrefix(message.jda.selfUser, commandParts[0])
        return when (runCondition) {
            RunCondition.GUILD -> checkGuild(container, message, language)
            RunCondition.VC_BOT_ALONE_OR_USER_DJ -> checkOtherOrSameVCBotAloneOrUserDJ(
                container,
                message,
                command,
                language
            )
//            RunCondition.SAME_VC_BOT_ALONE_OR_USER_DJ -> checkSameVCBotAloneOrUserDJ(container, event, command, language)
            RunCondition.VC_BOT_OR_USER_DJ -> checkVCBotOrUserDJ(container, message, command, language)
            RunCondition.BOT_ALONE_OR_USER_DJ -> checkBotAloneOrUserDJ(container, message, command, language)
            RunCondition.PLAYING_TRACK_NOT_NULL -> checkPlayingTrackNotNullMessage(container, message, language)
            RunCondition.DEV_ONLY -> checkDevOnly(container, message, language)
            RunCondition.CHANNEL_NSFW -> checkChannelNSFW(container, message, language)
            RunCondition.VOTED -> checkVoted(container, message, language)
            RunCondition.USER_SUPPORTER -> checkUserSupporter(container, message, language, prefix)
            RunCondition.GUILD_SUPPORTER -> checkGuildSupporter(container, message, language, prefix)
        }
    }

    private suspend fun checkGuildSupporter(
        container: Container,
        message: Message,
        language: String,
        prefix: String
    ): Boolean {
        val supporterGuilds = container.daoManager.supporterWrapper.getGuilds()
        return if (!supporterGuilds.contains(message.guild.idLong)) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.server.supporter")
                .replacePrefix(prefix)
            message.channel.sendMessage(msg).queue()
            false
        } else {
            true
        }
    }

    private suspend fun checkUserSupporter(
        container: Container,
        message: Message,
        language: String,
        prefix: String
    ): Boolean {
        val supporters = container.daoManager.supporterWrapper.getUsers()
        return if (
            supporters.contains(message.author.idLong) ||
            container.settings.botInfo.developerIds.contains(message.author.idLong)
        ) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.user.supporter")
                .replacePrefix(prefix)
            message.channel.sendMessage(msg).queue()
            false
        }
    }

    private suspend fun checkVoted(container: Container, message: Message, language: String): Boolean {
        return if (
            container.settings.botInfo.developerIds.contains(message.author.idLong) ||
            container.daoManager.supporterWrapper.getUsers().contains(message.author.idLong) ||
            container.settings.environment == Environment.TESTING ||
            !container.voteReq
        ) {
            true
        } else {
            val vote = container.daoManager.voteWrapper.getUserVote(message.author.idLong)
            val lastTime =
                listOf(vote?.bfdLastTime, vote?.topggLastTime, vote?.dblLastTime, vote?.dboatsLastTime).maxByOrNull {
                    it ?: 0
                }

            if (vote != null && lastTime != null && (System.currentTimeMillis() - lastTime) < 86_400_000) {
                true
            } else {
                val msg = i18n.getTranslation(language, "message.runcondition.failed.voted")
                    .withVariable("url", VOTE_URL)
                message.channel.sendMessage(msg).queue()
                false
            }
        }
    }

    private fun checkChannelNSFW(container: Container, message: Message, language: String): Boolean {
        return if (
            (message.isFromGuild || container.settings.botInfo.developerIds.contains(message.author.idLong)) &&
            message.textChannel.isNSFW
        ) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.nsfw")
            message.channel.sendMessage(msg).queue()
            false
        }
    }

    private fun checkDevOnly(container: Container, message: Message, language: String): Boolean {
        return if (container.settings.botInfo.developerIds.contains(message.author.idLong)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.devonly")
            message.channel.sendMessage(msg).queue()
            false
        }
    }

    private suspend fun checkPlayingTrackNotNullMessage(
        container: Container,
        message: Message,
        language: String
    ): Boolean {
        if (checkPlayingTrackNotNull(container, message)) {
            return true
        }

        val noSongPlaying = i18n.getTranslation(language, "message.runcondition.failed.playingtracknotnull")
        sendRspOrMsg(message.textChannel, container.daoManager, noSongPlaying)
        return false
    }


    fun checkPlayingTrackNotNull(container: Container, message: Message): Boolean {
        val trackManager = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(message.guild).guildTrackManager
        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null || message.guild.selfMember.voiceState?.inVoiceChannel() != true) {
            return false
        }
        return true
    }

    private suspend fun checkVCBotOrUserDJ(
        container: Container,
        message: Message,
        command: AbstractCommand,
        language: String
    ): Boolean {
        val member = message.member ?: return false
        val vc = member.voiceState?.channel
        val botVc = container.lavaManager.getConnectedChannel(message.guild)

        if (vc == null && botVc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendRspOrMsg(message.textChannel, container.daoManager, msg)
            return false
        }

        if (vc?.id == botVc?.id) return true
        else if (vc != null && botVc == null) return true
        else if (hasPermission(
                container,
                message,
                SpecialPermission.MUSIC_BYPASS_SAMEVC.node,
                command.commandCategory,
                false
            )
        ) return true

        val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbot")
        sendRspOrMsg(message.textChannel, container.daoManager, msg)
        return false

    }

    // passes if the bot is in the same vc with one listener
    suspend fun checkOtherOrSameVCBotAloneOrUserDJ(
        container: Container,
        message: Message,
        command: AbstractCommand,
        language: String
    ): Boolean {
        val member = message.member ?: return false
        val vc = member.voiceState?.channel
        val bc = member.guild.selfMember.voiceState?.channel

        if (vc == null && bc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendRspOrMsg(message.textChannel, container.daoManager, msg)
            return false
        }

        return if (vc?.id == bc?.id && vc?.let { listeningMembers(it, member.idLong) } == 0) true
        else if (vc != null && bc == null) true
        else if (vc?.id != bc?.id && bc != null && listeningMembers(bc) == 0) true
        else if (hasPermission(
                container,
                message,
                SpecialPermission.MUSIC_BYPASS_VCBOTALONE.node,
                command.commandCategory,
                false
            )
        ) true
        else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbotalone")
            sendRspOrMsg(message.textChannel, container.daoManager, msg)
            false
        }
    }

    private fun checkGuild(container: Container, message: Message, language: String): Boolean {
        return if (message.isFromGuild) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.serveronly")
            sendMsg(message.privateChannel, msg)
            false
        }
    }

    suspend fun checkBotAloneOrUserDJ(
        container: Container,
        message: Message,
        command: AbstractCommand,
        language: String
    ): Boolean {
        val guild = message.guild
        val selfMember = guild.selfMember
        val vc = selfMember.voiceState?.channel
        val botAlone = vc == null || listeningMembers(vc, message.author.idLong) == 0
        return if (botAlone || hasPermission(
                container,
                message,
                SpecialPermission.MUSIC_BYPASS_BOTALONE.node,
                command.commandCategory,
                false
            )
        ) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.botalone")
            sendRspOrMsg(message.textChannel, container.daoManager, msg)
            false
        }
    }

    // Used by summon command without args
    suspend fun checkOtherBotAloneOrDJOrSameVC(
        container: Container,
        message: Message,
        command: AbstractCommand,
        language: String
    ): Boolean {
        val member = message.member ?: return false
        val vc = member.voiceState?.channel
        val bc = member.guild.selfMember.voiceState?.channel

        if (vc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendRspOrMsg(message.textChannel, container.daoManager, msg)
            return false
        }

        return if (bc == null) true
        else if (vc.idLong == bc.idLong) true
        else if (vc.idLong != bc.idLong && listeningMembers(bc) == 0) true
        else if (hasPermission(
                container,
                message,
                SpecialPermission.MUSIC_BYPASS_VCBOTALONE.node,
                command.commandCategory,
                false
            )
        ) true
        else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbotalone")
            sendRspOrMsg(message.textChannel, container.daoManager, msg)
            false
        }
    }
}