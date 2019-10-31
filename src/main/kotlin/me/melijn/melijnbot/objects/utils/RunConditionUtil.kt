package me.melijn.melijnbot.objects.utils

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import me.melijn.melijnbot.Container
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
        if (cTrack == null) {
            val noSongPlaying = i18n.getTranslation(language, "message.music.notracks")
            sendMsg(event.textChannel, noSongPlaying)
            return false
        }
        return true
    }

    suspend fun checkVCBotOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        if (vc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendMsg(event.textChannel, msg)
            return false
        }
        if (vc.members.contains(member.guild.selfMember)) return true
        else if (!vc.members.contains(member.guild.selfMember) && member.guild.selfMember.voiceState?.inVoiceChannel() != true) return true
        else if (hasPermission(command, container, event, "music.bypass.samevc", true)) return true

        val msg = i18n.getTranslation(language, "message.runcondition.failed.vcbot")
        sendMsg(event.textChannel, msg)
        return false

    }

    //passes if the bot is in the same vc with one listener
    suspend fun checkOtherOrSameVCBotAloneOrUserDJ(container: Container, event: MessageReceivedEvent, command: AbstractCommand, language: String): Boolean {
        val member = event.member ?: return false
        val vc = member.voiceState?.channel
        if (vc == null) {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.vc")
            sendMsg(event.textChannel, msg)
            return false
        }
        val bc = member.guild.selfMember.voiceState?.channel

        return if (vc.members.contains(member.guild.selfMember) && listeningMembers(vc, member.idLong) == 0) true
        else if (!vc.members.contains(member.guild.selfMember) && member.guild.selfMember.voiceState?.inVoiceChannel() != true) true
        else if (!vc.members.contains(member.guild.selfMember) && bc != null && listeningMembers(bc) == 0) true
        else if (hasPermission(command, container, event, "music.bypass.vcbotalone", true)) true
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
        else if (hasPermission(command, container, event, "music.bypass.samevcalone", true)) true
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
        return if (botAlone || hasPermission(command, container, event, "music.bypass.botalone", true)) {
            true
        } else {
            val msg = i18n.getTranslation(language, "message.runcondition.failed.botaloneordj")
            sendMsg(event.textChannel, msg)
            false
        }
    }
}