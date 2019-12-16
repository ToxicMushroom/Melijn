package me.melijn.melijnbot.objects.utils

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.commands.utility.VOTE_URL
import me.melijn.melijnbot.enums.SpecialPermission
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.command.hasPermission
import me.melijn.melijnbot.objects.translation.getLanguage
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object RunConditionUtil {

    /**
     * [return] returns true if the check passed
     *
     * **/
    suspend fun runConditionCheckPassed(container: Container, runCondition: RunCondition, event: MessageReceivedEvent, command: AbstractCommand): Boolean {
        val userId = event.author.idLong
        val language = getLanguage(container.daoManager, userId, if (event.isFromGuild) event.guild.idLong else -1)
        return when (runCondition) {
            RunCondition.GUILD -> checkGuild(event, language)
            RunCondition.VC_BOT_ALONE_OR_USER_DJ -> checkOtherOrSameVCBotAloneOrUserDJ(container, event, command, language)
            RunCondition.SAME_VC_BOT_ALONE_OR_USER_DJ -> checkSameVCBotAloneOrUserDJ(container, event, command, language)
            RunCondition.VC_BOT_OR_USER_DJ -> checkVCBotOrUserDJ(container, event, command, language)
            RunCondition.BOT_ALONE_OR_USER_DJ -> checkBotAloneOrUserDJ(container, event, command, language)
            RunCondition.PLAYING_TRACK_NOT_NULL -> checkPlayingTrackNotNull(container, event, language)
            RunCondition.DEV_ONLY -> checkDevOnly(container, event, language)
            RunCondition.CHANNEL_NSFW -> checkChannelNSFW(container, event, language)
            RunCondition.VOTED -> checkVoted(container, event, language)
            RunCondition.SUPPORTER -> checkSupporter(container, event, language)
        }
    }

    private fun checkSupporter(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return true
        // val supporters = container.daoManager.supporterWrapper.userSupporterIds
//        return if (!supporters.contains(event.author.idLong)) {
//            val msg = i18n.getTranslation(language, "message.runcondition.failed.supporter")
//            event.channel.sendMessage(msg).queue()
//            false
//        } else {
//            true
//        }
    }

    private suspend fun checkVoted(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return if (container.settings.developerIds.contains(event.author.idLong)) {
            true
        } else {
            val vote = container.daoManager.voteWrapper.getUserVote(event.author.idLong) ?: return false
            if ((System.currentTimeMillis() - vote.lastTime) < 86_400_000) {
                true
            } else {
                val msg = i18n.getTranslation(language, "message.runcondition.failed.voted")
                    .replace("%url%", VOTE_URL)
                event.channel.sendMessage(msg).queue()
                false
            }
        }
    }

    private fun checkChannelNSFW(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        return if (container.settings.developerIds.contains(event.author.idLong)) {
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
        return if (container.settings.developerIds.contains(event.author.idLong)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.devonly")
            event.channel.sendMessage(msg).queue()
            false
        }
    }

    suspend fun checkPlayingTrackNotNull(container: Container, event: MessageReceivedEvent, language: String): Boolean {
        val trackManager = container.lavaManager.musicPlayerManager.getGuildMusicPlayer(event.guild).guildTrackManager
        val cTrack: AudioTrack? = trackManager.iPlayer.playingTrack
        if (cTrack == null || event.guild.selfMember.voiceState?.inVoiceChannel() != true) {
            val noSongPlaying = i18n.getTranslation(language, "message.runcondition.failed.playingtracknotnull")
            sendMsg(event.textChannel, noSongPlaying)
            return false
        }
        return true
    }

    suspend fun checkVCBotOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        val botVc = container.lavaManager.getConnectedChannel(event.guild)

        if (vc == null && botVc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendMsg(event.textChannel, msg)
            return false
        }

        if (vc?.id == botVc?.id) return true
        else if (vc != null && botVc == null) return true
        else if (hasPermission(command, container, event, SpecialPermission.MUSIC_BYPASS_SAMEVC.node, true)) return true

        val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbot")
        sendMsg(event.textChannel, msg)
        return false

    }

    //passes if the bot is in the same vc with one listener
    suspend fun checkOtherOrSameVCBotAloneOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        val bc = member.guild.selfMember.voiceState?.channel

        if (vc == null && bc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendMsg(event.textChannel, msg)
            return false
        }

        return if (vc?.id == bc?.id && vc?.let { listeningMembers(it, member.idLong) } == 0) true
        else if (vc != null && bc == null) true
        else if (vc?.id != bc?.id && bc != null && listeningMembers(bc) == 0) true
        else if (hasPermission(command, container, event, SpecialPermission.MUSIC_BYPASS_VCBOTALONE.node, true)) true
        else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbotalone")
            sendMsg(event.textChannel, msg)
            false
        }
    }

    suspend fun checkSameVCBotAloneOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        if (vc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendMsg(event.textChannel, msg)
            return false
        }
        return if (vc.members.contains(member.guild.selfMember) && listeningMembers(vc, member.idLong) == 0) true
        else if (!vc.members.contains(member.guild.selfMember) && member.guild.selfMember.voiceState?.inVoiceChannel() != true) true
        else if (hasPermission(command, container, event, SpecialPermission.MUSIC_BYPASS_SAMEVCALONE.node, true)) true
        else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.samevcalone")
            sendMsg(event.textChannel, msg)
            false
        }
    }

    suspend fun checkGuild(event: MessageReceivedEvent, language: String): Boolean {
        return if (event.isFromGuild) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.guildonly")
            sendMsg(event.privateChannel, msg)
            false
        }
    }

    suspend fun checkBotAloneOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val guild = event.guild
        val selfMember = guild.selfMember
        val vc = selfMember.voiceState?.channel
        val botAlone = vc == null || listeningMembers(vc, event.author.idLong) == 0
        return if (botAlone || hasPermission(command, container, event, SpecialPermission.MUSIC_BYPASS_BOTALONE.node, true)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.botalone")
            sendMsg(event.textChannel, msg)
            false
        }
    }
}