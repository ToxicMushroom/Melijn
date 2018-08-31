package me.melijn.jda.events;

import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.SetMusicChannelCommand;
import me.melijn.jda.commands.management.SetStreamerModeCommand;
import me.melijn.jda.commands.music.SetStreamUrlCommand;
import me.melijn.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

public class Channels extends ListenerAdapter {

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            if (event.getChannelLeft() == audioManager.getConnectedChannel()) {
                AudioPlayer audioPlayer = manager.getPlayer(guild).getAudioPlayer();
                int doveDuiven = 0;
                for (Member member : audioManager.getConnectedChannel().getMembers()) {
                    if (member.getVoiceState().isDeafened() || member.getVoiceState().isGuildDeafened()) {
                        if (member != guild.getSelfMember()) doveDuiven++;
                    }
                }
                if ((audioManager.getConnectedChannel().getMembers().size() - doveDuiven) == 1) {
                    if (!audioPlayer.isPaused())
                        audioPlayer.stopTrack();
                    audioManager.closeAudioConnection();
                } else if (audioPlayer.getPlayingTrack() == null) {
                    if (SetStreamUrlCommand.streamUrls.containsKey(guildId)) {
                        manager.getPlayer(guild).getListener().tracks.clear();
                        manager.loadSimpelTrack(guild, SetStreamUrlCommand.streamUrls.get(guildId));
                    }
                }
            }
        } else if (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) &&
                !audioManager.isConnected() && event.getChannelJoined().getIdLong() == SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)) {
            audioManager.openAudioConnection(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)));
            if (SetStreamUrlCommand.streamUrls.containsKey(guildId) && manager.getPlayer(guild).getAudioPlayer().getPlayingTrack() == null) {
                manager.getPlayer(guild).getListener().tracks.clear();
                manager.loadSimpelTrack(guild, SetStreamUrlCommand.streamUrls.get(guildId));
            }
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        AudioManager audioManager = guild.getAudioManager();
        if (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) && !audioManager.isConnected() && event.getChannelJoined().getIdLong() == (SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId))) {
            audioManager.openAudioConnection(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)));
            if (SetStreamUrlCommand.streamUrls.containsKey(guildId) && manager.getPlayer(guild).getAudioPlayer().getPlayingTrack() == null) {
                manager.getPlayer(guild).getListener().tracks.clear();
                manager.loadSimpelTrack(guild, SetStreamUrlCommand.streamUrls.get(guildId));
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        luisteraarsCheck(guild, audioManager);
    }

    private void luisteraarsCheck(Guild guild, AudioManager audioManager) {
        if (audioManager.isConnected()) {
            AudioPlayer audioPlayer = manager.getPlayer(guild).getAudioPlayer();
            int doveDuiven = 0;
            for (Member member : audioManager.getConnectedChannel().getMembers()) {
                if (member.getVoiceState().isDeafened() || member.getVoiceState().isGuildDeafened()) {
                    if (member != guild.getSelfMember()) doveDuiven++;
                }
            }
            if ((audioManager.getConnectedChannel().getMembers().size() - doveDuiven) == 1) {
                if (!audioPlayer.isPaused())
                    audioPlayer.stopTrack();
                audioManager.closeAudioConnection();
            }
        }
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        luisteraarsCheck(guild, audioManager);
    }
}
