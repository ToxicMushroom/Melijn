package me.melijn.jda.events;

import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.RoleType;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.commands.music.LoopCommand;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.concurrent.atomic.AtomicInteger;

public class Channels extends ListenerAdapter {

    private AudioLoader manager = AudioLoader.getManagerInstance();
    private Lava lava = Lava.lava;

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        TaskScheduler.async(() -> {
            long guildId = event.getGuild().getIdLong();
            long channelId = event.getChannel().getIdLong();

            if (channelId == SetVerificationChannelCommand.verificationChannelsCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.VERIFICATION);
                SetVerificationChannelCommand.verificationChannelsCache.invalidate(guildId);

            } else if (channelId == SetJoinLeaveChannelCommand.welcomeChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.WELCOME);
                SetJoinLeaveChannelCommand.welcomeChannelCache.invalidate(guildId);

            } else if (channelId == SetSelfRoleChannelCommand.selfRolesChannel.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.SELF_ROLE);
                SetSelfRoleChannelCommand.selfRolesChannel.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.banLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.BAN_LOG);
                SetLogChannelCommand.banLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.kickLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.KICK_LOG);
                SetLogChannelCommand.kickLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.warnLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.WARN_LOG);
                SetLogChannelCommand.warnLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.muteLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.MUTE_LOG);
                SetLogChannelCommand.muteLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.SDM_LOG);
                SetLogChannelCommand.sdmLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.odmLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.ODM_LOG);
                SetLogChannelCommand.odmLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.pmLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.PM_LOG);
                SetLogChannelCommand.pmLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.emLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.EM_LOG);
                SetLogChannelCommand.emLogChannelCache.invalidate(guildId);

            } else if (channelId == SetLogChannelCommand.musicLogChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.MUSIC_LOG);
                SetLogChannelCommand.musicLogChannelCache.invalidate(guildId);
            }
        });
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        TaskScheduler.async(() -> {
            long guildId = event.getGuild().getIdLong();
            long channelId = event.getChannel().getIdLong();

            if (channelId == SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeChannel(guildId, ChannelType.MUSIC);
                SetMusicChannelCommand.musicChannelCache.invalidate(guildId);
            }
        });
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        TaskScheduler.async(() -> {
            long guildId = event.getGuild().getIdLong();
            long roleId = event.getRole().getIdLong();

            if (roleId == SetMuteRoleCommand.muteRoleCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeRole(guildId, RoleType.MUTE);
                SetMuteRoleCommand.muteRoleCache.invalidate(roleId);

            } else if (roleId == SetJoinRoleCommand.joinRoleCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeRole(guildId, RoleType.JOIN);
                SetJoinRoleCommand.joinRoleCache.invalidate(roleId);

            } else if (roleId == SetUnverifiedRoleCommand.unverifiedRoleCache.getUnchecked(guildId)) {
                Melijn.mySQL.removeRole(guildId, RoleType.UNVERIFIED);
                SetUnverifiedRoleCommand.unverifiedRoleCache.invalidate(roleId);
            }

        });
    }

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
                        (guild.getSelfMember().hasPermission(guild.getVoiceChannelById(SetMusicChannelCommand.musicChannelCache.getUnchecked(guildId)), Permission.VOICE_MUTE_OTHERS) &&
                        (!guild.getSelfMember().getVoiceState().inVoiceChannel()) || (guild.getAfkChannel().getIdLong() == guild.getSelfMember().getVoiceState().getChannel().getIdLong()))) {
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
