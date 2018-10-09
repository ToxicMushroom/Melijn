package me.melijn.jda.events;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.SetMusicChannelCommand;
import me.melijn.jda.commands.management.SetStreamerModeCommand;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.atomic.AtomicInteger;

public class Channels extends ListenerAdapter {

    private MusicManager manager = MusicManager.getManagerInstance();

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        long guildId = guild.getIdLong();
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            if (event.getChannelLeft() == audioManager.getConnectedChannel()) {
                AudioPlayer audioPlayer = manager.getPlayer(guild).getAudioPlayer();
                if (someoneIsListening(guild)) {
                    String url = Melijn.mySQL.getStreamUrl(guildId);
                    if (!"".equals(url)) {
                        manager.getPlayer(guild).getListener().tracks.clear();
                        manager.loadSimpelTrack(guild, url);
                    }
                } else {
                    audioPlayer.setPaused(true);
                    if (audioManager.getConnectedChannel().getMembers().size() > 1) {
                        runLeaveTimer(guild, 300, true);
                    } else {
                        runLeaveTimer(guild, 60, false);
                    }
                }
            }
        } else if (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) &&
                !audioManager.isConnected() &&
                event.getChannelJoined().getIdLong() == SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)) {
            audioManager.openAudioConnection(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)));
            tryPlayStreamUrl(guild);
            TaskScheduler.async(() -> {
                //Hacky way to unmute bot in afk channel
                if (guild.getAfkChannel() != null &&
                        guild.getSelfMember().hasPermission(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)), Permission.VOICE_MUTE_OTHERS) &&
                        guild.getSelfMember().getVoiceState().getChannel() == null ||
                        guild.getAfkChannel().getIdLong() == guild.getSelfMember().getVoiceState().getChannel().getIdLong()) {
                    guild.getController().setMute(guild.getSelfMember(), true).queue(done ->
                            event.getGuild().getController().setMute(event.getGuild().getSelfMember(), false).queue());
                }
            }, event.getJDA().getPing() + 1800);
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        long guildId = guild.getIdLong();
        AudioManager audioManager = guild.getAudioManager();
        if (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) &&
                !audioManager.isConnected() &&
                event.getChannelJoined().getIdLong() == (SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId))) {
            audioManager.openAudioConnection(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)));
            tryPlayStreamUrl(guild);
            TaskScheduler.async(() -> {
                if (guild.getAfkChannel() != null &&
                        guild.getSelfMember().hasPermission(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)), Permission.VOICE_MUTE_OTHERS) &&
                        (!guild.getSelfMember().getVoiceState().inVoiceChannel()) || (guild.getAfkChannel().getIdLong() == guild.getSelfMember().getVoiceState().getChannel().getIdLong())) {
                    guild.getController().setMute(guild.getSelfMember(), true).queue(done -> guild.getController().setMute(guild.getSelfMember(), false).queue());
                }
            }, 2000);
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        if (!event.getMember().equals(event.getGuild().getSelfMember()))
            whenListeningDoActions(event.getGuild());
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        if (event.getGuild() == null || EvalCommand.userBlackList.contains(event.getGuild().getIdLong()))
            return;
        if (!event.getMember().equals(event.getGuild().getSelfMember()))
            whenListeningDoActions(event.getGuild());
    }

    private void whenListeningDoActions(Guild guild) {
        if (guild.getAudioManager().getConnectedChannel() != null) {
            if (!someoneIsListening(guild)) {
                AudioPlayer player = manager.getPlayer(guild).getAudioPlayer();
                player.setPaused(true);
                if (guild.getAudioManager().getConnectedChannel().getMembers().size() > 1) {
                    runLeaveTimer(guild, 300, true);
                } else {
                    runLeaveTimer(guild, 60, false);
                }
            }
        }
    }

    private void runLeaveTimer(Guild guild, int seconds, boolean defeaned) {
        AtomicInteger amount = new AtomicInteger();
        TaskScheduler.async(() -> {
            while (true) {
                Guild guild2 = guild.getJDA().asBot().getShardManager().getGuildById(guild.getIdLong());
                if (guild2 == null || !guild2.getAudioManager().isConnected())
                    break;
                if (someoneIsListening(guild2)) {
                    manager.getPlayer(guild2).getAudioPlayer().setPaused(false);
                    break;
                } else if (guild2.getAudioManager().getConnectedChannel().getMembers().size() == 1 && defeaned) {
                    manager.getPlayer(guild2).getAudioPlayer().setPaused(false);
                    manager.getPlayer(guild2).getAudioPlayer().stopTrack();
                    Helpers.scheduleClose(guild2.getAudioManager());
                    break;
                }
                if (amount.getAndIncrement() == seconds) {
                    manager.getPlayer(guild2).getAudioPlayer().setPaused(false);
                    manager.getPlayer(guild2).stopTrack();
                    Helpers.scheduleClose(guild2.getAudioManager());
                    break;
                }
                try {
                    Thread.sleep(1_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    private void tryPlayStreamUrl(Guild guild) {
        if (manager.getPlayer(guild).getAudioPlayer().getPlayingTrack() == null) {
            String url = Melijn.mySQL.getStreamUrl(guild.getIdLong());
            if (!"".equals(url)) {
                manager.getPlayer(guild).getListener().tracks.clear();
                manager.loadSimpelTrack(guild, url);
            }
        }
    }

    private boolean someoneIsListening(Guild guild) {
        AudioManager audioManager = guild.getAudioManager();
        if (audioManager.isConnected()) {
            int doveDuiven = 0;
            for (Member member : guild.getAudioManager().getConnectedChannel().getMembers()) {
                if ((member.getVoiceState().isDeafened() || member.getUser().isBot() || member.getVoiceState().isGuildDeafened()) && member != guild.getSelfMember())
                    doveDuiven++;
            }
            return (audioManager.getConnectedChannel().getMembers().size() - doveDuiven) > 1;
        }
        return false;
    }
}
