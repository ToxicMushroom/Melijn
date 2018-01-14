package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.music.MusicManager;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Channels extends ListenerAdapter {

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e) {
        if (e.getGuild().getAudioManager().isConnected()) {
            AudioPlayer audioPlayer = manager.getPlayer(e.getGuild()).getAudioPlayer();
            if (e.getGuild().getAudioManager().getConnectedChannel().getMembers().size() == 1) {
                audioPlayer.setPaused(true);
            } else {
                if (audioPlayer.isPaused()) audioPlayer.setPaused(false);
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {
        if (e.getGuild().getAudioManager().isConnected()) {
            AudioPlayer audioPlayer = manager.getPlayer(e.getGuild()).getAudioPlayer();
            if (e.getGuild().getAudioManager().getConnectedChannel().getMembers().size() == 1) {
                audioPlayer.setPaused(true);
            } else {
                if (audioPlayer.isPaused()) audioPlayer.setPaused(false);
            }
        }
    }
}
