package me.melijn.jda.events;

import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.SetMusicChannelCommand;
import me.melijn.jda.commands.management.SetStreamerModeCommand;
import me.melijn.jda.commands.music.LoopCommand;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.concurrent.atomic.AtomicInteger;

public class Channels extends ListenerAdapter {

    private AudioLoader manager = AudioLoader.getManagerInstance();
    private Lava lava = Lava.lava;

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong())) return;
        Guild guild = event.getGuild();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        long guildId = guild.getIdLong();
        if (lava.isConnected(guildId)) {
            if (event.getChannelLeft() != lava.getConnectedChannel(guild)) return;
            if (someoneIsListening(guild)) {
                String url = Melijn.mySQL.getStreamUrl(guildId);
                if (!url.isBlank()) {
                    manager.getPlayer(guild).getTrackManager().clear();
                    manager.loadSimpleTrack(manager.getPlayer(guild), url);
                }
            } else {
                manager.getPlayer(guild).getAudioPlayer().setPaused(true);
                if (lava.getConnectedChannel(guild).getMembers().size() > 1) {
                    runLeaveTimer(guild, 300, true);
                } else {
                    runLeaveTimer(guild, 60, false);
                }
            }
        } else if (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) &&
                !lava.isConnected(guildId) &&
                event.getChannelJoined().getIdLong() == SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)) {
            lava.openConnection(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)));
            tryPlayStreamUrl(guild.getIdLong());
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
        if (SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId) &&
                !lava.isConnected(guildId) &&
                event.getChannelJoined().getIdLong() == (SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId))) {
            lava.openConnection(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)));
            tryPlayStreamUrl(guild.getIdLong());
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
        if (!lava.isConnected(guild.getIdLong())) return;
        if (!someoneIsListening(guild)) {
            manager.getPlayer(guild).getAudioPlayer().setPaused(true);
            if (lava.getConnectedChannel(guild).getMembers().size() > 1) {
                runLeaveTimer(guild, 300, true);
            } else {
                runLeaveTimer(guild, 60, false);
            }
        }
    }

    private void runLeaveTimer(Guild guild, int seconds, boolean defeaned) {
        AtomicInteger amount = new AtomicInteger();
        TaskScheduler.async(() -> {
            while (true) {
                Guild guild2 = guild.getJDA().asBot().getShardManager().getGuildById(guild.getIdLong());
                MusicPlayer player = manager.getPlayer(guild2);
                if (guild2 == null || !lava.isConnected(guild.getIdLong()))
                    break;
                if (someoneIsListening(guild2)) {
                    player.getAudioPlayer().setPaused(false);
                    break;
                } else if ((lava.getConnectedChannel(guild2).getMembers().size() == 1 && defeaned) || (amount.getAndIncrement() == seconds)) {
                    LoopCommand.looped.remove(guild2.getIdLong());
                    player.getAudioPlayer().setPaused(false);
                    player.stopTrack();
                    player.getTrackManager().clear();
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

    private void tryPlayStreamUrl(long guildId) {
        if (manager.getPlayer(guildId).getAudioPlayer().getPlayingTrack() == null) {
            String url = Melijn.mySQL.getStreamUrl(guildId);
            if (!url.isBlank()) {
                manager.getPlayer(guildId).getTrackManager().clear();
                manager.loadSimpleTrack(manager.getPlayer(guildId), url);
            }
        }
    }

    private boolean someoneIsListening(Guild guild) {
        if (!lava.isConnected(guild.getIdLong())) return false;
        int doveDuiven = 0;
        for (Member member : lava.getConnectedChannel(guild).getMembers()) {
            if ((member.getVoiceState().isDeafened() || member.getUser().isBot() || member.getVoiceState().isGuildDeafened()) && member != guild.getSelfMember())
                doveDuiven++;
        }
        return (lava.getConnectedChannel(guild).getMembers().size() - doveDuiven) > 1;
    }
}
