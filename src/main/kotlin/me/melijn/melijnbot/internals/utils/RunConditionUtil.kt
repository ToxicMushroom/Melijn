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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object RunConditionUtil {

    /**
     * [return] returns true if the check passed
     *
     * **/
    suspend fun runConditionCheckPassed(
        container: Container,
        runCondition: RunCondition,
        event: MessageReceivedEvent,
        command: AbstractCommand,
        commandParts: List<String>
    ): Boolean {
        val userId = event.author.idLong
        val language = getLanguage(container.daoManager, userId, if (event.isFromGuild) event.guild.idLong else -1)
        val prefix = getNicerUsedPrefix(event.jda.selfUser, commandParts[0])
        return when (runCondition) {
            RunCondition.GUILD -> checkGuild(container, event, language)
            RunCondition.VC_BOT_ALONE_OR_USER_DJ -> checkOtherOrSameVCBotAloneOrUserDJ(container, event, command, language)
//            RunCondition.SAME_VC_BOT_ALONE_OR_USER_DJ -> checkSameVCBotAloneOrUserDJ(container, event, command, language)
            RunCondition.VC_BOT_OR_USER_DJ -> checkVCBotOrUserDJ(container, event, command, language)
            RunCondition.BOT_ALONE_OR_USER_DJ -> checkBotAloneOrUserDJ(container, event, command, language)
            RunCondition.PLAYING_TRACK_NOT_NULL -> checkPlayingTrackNotNullMessage(container, event, language)
            RunCondition.DEV_ONLY -> checkDevOnly(container, event, language)
            RunCondition.CHANNEL_NSFW -> checkChannelNSFW(container, event, language)
            RunCondition.VOTED -> checkVoted(container, event, language)
            RunCondition.USER_SUPPORTER -> checkUserSupporter(container, event, language, prefix)
            RunCondition.GUILD_SUPPORTER -> checkGuildSupporter(container, event, language, prefix)
        }
    }

    private suspend fun checkGuildSupporter(container: Container, event: MessageReceivedEvent, language: String, prefix: String): Boolean {
        val supporterGuilds = container.daoManager.supporterWrapper.getGuilds()
        return if (!supporterGuilds.contains(event.guild.idLong)) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.server.supporter")
                .replacePrefix(prefix)
            event.channel.sendMessage(msg).queue()
            false
        } else {
            true
        }
    }

    private suspend fun checkUserSupporter(container: Container, event: MessageReceivedEvent, language: String, prefix: String): Boolean {
        val supporters = container.daoManager.supporterWrapper.getUsers()
        return if (
            supporters.contains(event.author.idLong) ||
            container.settings.botInfo.developerIds.contains(event.author.idLong)
        ) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.user.supporter")
                .replacePrefix(prefix)
            event.channel.sendMessage(msg).queue()
            false
        }
    }

    private suspend fun checkVoted(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return if (
            container.settings.botInfo.developerIds.contains(event.author.idLong) ||
            container.daoManager.supporterWrapper.getUsers().contains(event.author.idLong) ||
            container.settings.environment == Environment.TESTING ||
            !container.voteReq
        ) {
            true
        } else {
            val vote = container.daoManager.voteWrapper.getUserVote(event.author.idLong)
            val lastTime = listOf(vote?.bfdLastTime, vote?.topggLastTime, vote?.dblLastTime, vote?.dboatsLastTime).maxByOrNull {
                it ?: 0
            }

            if (vote != null && lastTime != null && (System.currentTimeMillis() - lastTime) < 86_400_000) {
                true
            } else {
                val msg = i18n.getTranslation(language, "message.runcondition.failed.voted")
                    .withVariable("url", VOTE_URL)
                event.channel.sendMessage(msg).queue()
                false
            }
        }
    }

    private fun checkChannelNSFW(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return if (container.settings.botInfo.developerIds.contains(event.author.idLong)) {
            true
        } else {
            if (event.textChannel.isNSFW) {
                true
            } else {
                val msg = i18n.getTranslation(language, "message.runcondition.failed.nsfw")
                event.channel.sendMessage(msg).queue()
                false
            }
        }
    }

    private fun checkDevOnly(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return if (container.settings.botInfo.developerIds.contains(event.author.idLong)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.devonly")
            event.channel.sendMessage(msg).queue()
            false
        }
    }

    private suspend fun checkPlayingTrackNotNullMessage(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        if (checkPlayingTrackNotNull(container, event)) {
            return true
        }

        val noSongPlaying = i18n.getTranslation(language, "message.runcondition.failed.playingtracknotnull")
        sendRspOrMsg(event.textChannel, container.daoManager, noSongPlaying)
        return false
    }


    fun checkPlayingTrackNotNull(container: Container, event: MessageReceivedEvent): Boolean {
        val trackManager = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(event.guild).guildTrackManager
        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null || event.guild.selfMember.voiceState?.inVoiceChannel() != true) {
            return false
        }
        return true
    }

    private suspend fun checkVCBotOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        val botVc = container.lavaManager.getConnectedChannel(event.guild)

        if (vc == null && botVc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendRspOrMsg(event.textChannel, container.daoManager, msg)
            return false
        }

        if (vc?.id == botVc?.id) return true
        else if (vc != null && botVc == null) return true
        else if (hasPermission(container, event, SpecialPermission.MUSIC_BYPASS_SAMEVC.node, command.commandCategory, false)) return true

        val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbot")
        sendRspOrMsg(event.textChannel, container.daoManager, msg)
        return false

    }

    // passes if the bot is in the same vc with one listener
    suspend fun checkOtherOrSameVCBotAloneOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        val bc = member.guild.selfMember.voiceState?.channel

        if (vc == null && bc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendRspOrMsg(event.textChannel, container.daoManager, msg)
            return false
        }

        return if (vc?.id == bc?.id && vc?.let { listeningMembers(it, member.idLong) } == 0) true
        else if (vc != null && bc == null) true
        else if (vc?.id != bc?.id && bc != null && listeningMembers(bc) == 0) true
        else if (hasPermission(container, event, SpecialPermission.MUSIC_BYPASS_VCBOTALONE.node, command.commandCategory, false)) true
        else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbotalone")
            sendRspOrMsg(event.textChannel, container.daoManager, msg)
            false
        }
    }

    private fun checkGuild(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return if (event.isFromGuild) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.serveronly")
            sendMsg(event.privateChannel, msg)
            false
        }
    }

    suspend fun checkBotAloneOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val guild = event.guild
        val selfMember = guild.selfMember
        val vc = selfMember.voiceState?.channel
        val botAlone = vc == null || listeningMembers(vc, event.author.idLong) == 0
        return if (botAlone || hasPermission(container, event, SpecialPermission.MUSIC_BYPASS_BOTALONE.node, command.commandCategory, false)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.botalone")
            sendRspOrMsg(event.textChannel, container.daoManager, msg)
            false
        }
    }

    // Used by summon command without args
    suspend fun checkOtherBotAloneOrDJOrSameVC(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        val bc = member.guild.selfMember.voiceState?.channel

        if (vc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendRspOrMsg(event.textChannel, container.daoManager, msg)
            return false
        }

        return if (bc == null) true
        else if (vc.idLong == bc.idLong) true
        else if (vc.idLong != bc.idLong && listeningMembers(bc) == 0) true
        else if (hasPermission(container, event, SpecialPermission.MUSIC_BYPASS_VCBOTALONE.node, command.commandCategory, false)) true
        else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbotalone")
            sendRspOrMsg(event.textChannel, container.daoManager, msg)
            false
        }
    }
}