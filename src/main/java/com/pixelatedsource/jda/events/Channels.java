package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.music.MusicManager;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

public class Channels extends ListenerAdapter {

    private MusicManager manager = MusicManager.getManagerinstance();

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent e) {
        if (e.getGuild().getAudioManager().isConnected()) {
            if (e.getGuild().getAudioManager().getConnectedChannel().getMembers().size() == 1) {
                manager.getPlayer(e.getGuild()).stopTrack();
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent e) {
        if (e.getGuild().getAudioManager().isConnected()) {
            if (e.getGuild().getAudioManager().getConnectedChannel().getMembers().size() == 1) {
                manager.getPlayer(e.getGuild()).stopTrack();
            }
        }

    }
}
