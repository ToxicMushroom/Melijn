package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.music.MusicManager;
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
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        AudioManager audioManager = guild.getAudioManager();
        if (guild.getAudioManager().isConnected()) {
            if (event.getChannelLeft() == audioManager.getConnectedChannel()) {
                AudioPlayer audioPlayer = manager.getPlayer(guild).getAudioPlayer();
                int doveDuiven = 0;
                for (Member member : audioManager.getConnectedChannel().getMembers()) {
                    if (member.getVoiceState().isDeafened() || member.getVoiceState().isGuildDeafened()) {
                        if (member != guild.getSelfMember()) doveDuiven++;
                    }
                }
                if ((audioManager.getConnectedChannel().getMembers().size() - doveDuiven) == 1) {
                    audioPlayer.stopTrack();
                    audioManager.closeAudioConnection();
                } else if (audioPlayer.getPlayingTrack() == null) {
                    if (PixelSniper.mySQL.getStreamUrl(guild) != null) {
                        manager.getPlayer(guild).getListener().tracks.clear();
                        manager.loadSimpelTrack(guild, PixelSniper.mySQL.getStreamUrl(guild));
                    }
                }
            }
        } else if (PixelSniper.mySQL.getChannelId(guild, ChannelType.MUSIC) != null) {
            if (PixelSniper.mySQL.getStreamerMode(guild.getId()) && event.getChannelJoined().getId().equalsIgnoreCase(PixelSniper.mySQL.getChannelId(guild, ChannelType.MUSIC)) && !audioManager.isConnected()) {
                audioManager.openAudioConnection(guild.getVoiceChannelById(PixelSniper.mySQL.getChannelId(guildId, ChannelType.MUSIC)));
                if (PixelSniper.mySQL.getStreamUrl(guild) != null && manager.getPlayer(guild).getAudioPlayer().getPlayingTrack() == null) {
                    manager.getPlayer(guild).getListener().tracks.clear();
                    manager.loadSimpelTrack(guild, PixelSniper.mySQL.getStreamUrl(guild));
                }
            }
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        Guild guild = event.getGuild();
        String guildId = guild.getId();
        AudioManager audioManager = guild.getAudioManager();
        if (event.getChannelJoined().getId().equalsIgnoreCase(PixelSniper.mySQL.getChannelId(guild, ChannelType.MUSIC)) &&
                PixelSniper.mySQL.getStreamerMode(guild.getId()) && PixelSniper.mySQL.getChannelId(guild, ChannelType.MUSIC) != null &&
                !audioManager.isConnected()) {
            audioManager.openAudioConnection(guild.getVoiceChannelById(PixelSniper.mySQL.getChannelId(guildId, ChannelType.MUSIC)));
            if (PixelSniper.mySQL.getStreamUrl(guild) != null && manager.getPlayer(guild).getAudioPlayer().getPlayingTrack() == null) {
                manager.getPlayer(guild).getListener().tracks.clear();
                manager.loadSimpelTrack(guild, PixelSniper.mySQL.getStreamUrl(guild));
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            AudioPlayer audioPlayer = manager.getPlayer(guild).getAudioPlayer();
            int doveDuiven = 0;
            for (Member member : audioManager.getConnectedChannel().getMembers()) {
                if (member.getVoiceState().isDeafened() || member.getVoiceState().isGuildDeafened()) {
                    if (member != guild.getSelfMember()) doveDuiven++;
                }
            }
            if ((audioManager.getConnectedChannel().getMembers().size() - doveDuiven) == 1) {
                audioPlayer.stopTrack();
                audioManager.closeAudioConnection();
            }
        }
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        Guild guild = event.getGuild();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            AudioPlayer audioPlayer = manager.getPlayer(guild).getAudioPlayer();
            int doveDuiven = 0;
            for (Member member : audioManager.getConnectedChannel().getMembers()) {
                if (member.getVoiceState().isDeafened() || member.getVoiceState().isGuildDeafened()) {
                    if (member != guild.getSelfMember()) doveDuiven++;
                }
            }
            if ((audioManager.getConnectedChannel().getMembers().size() - doveDuiven) == 1) {
                audioPlayer.stopTrack();
                audioManager.closeAudioConnection();
            }
        }
    }
}
