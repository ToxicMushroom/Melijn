package me.melijn.jda.events;

import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.RoleType;
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

    private final Melijn melijn;

    public Channels(Melijn melijn) {
        this.melijn = melijn;
    }

    @Override
    public void onTextChannelDelete(TextChannelDeleteEvent event) {
        melijn.getTaskManager().async(() -> {
            long guildId = event.getGuild().getIdLong();
            long channelId = event.getChannel().getIdLong();

            if (channelId == melijn.getVariables().verificationChannelsCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.VERIFICATION);
                melijn.getVariables().verificationChannelsCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().welcomeChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.WELCOME);
                melijn.getVariables().welcomeChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().selfRolesChannels.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.SELF_ROLE);
                melijn.getVariables().selfRolesChannels.invalidate(guildId);

            } else if (channelId == melijn.getVariables().banLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.BAN_LOG);
                melijn.getVariables().banLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().kickLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.KICK_LOG);
                melijn.getVariables().kickLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().warnLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.WARN_LOG);
                melijn.getVariables().warnLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().muteLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.MUTE_LOG);
                melijn.getVariables().muteLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().sdmLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.SDM_LOG);
                melijn.getVariables().sdmLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().odmLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.ODM_LOG);
                melijn.getVariables().odmLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().pmLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.PM_LOG);
                melijn.getVariables().pmLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().emLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.EM_LOG);
                melijn.getVariables().emLogChannelCache.invalidate(guildId);

            } else if (channelId == melijn.getVariables().musicLogChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.MUSIC_LOG);
                melijn.getVariables().musicLogChannelCache.invalidate(guildId);
            }
        });
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        melijn.getTaskManager().async(() -> {
            long guildId = event.getGuild().getIdLong();
            long channelId = event.getChannel().getIdLong();

            if (channelId == melijn.getVariables().musicChannelCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeChannel(guildId, ChannelType.MUSIC);
                melijn.getVariables().musicChannelCache.invalidate(guildId);
            }
        });
    }

    @Override
    public void onRoleDelete(RoleDeleteEvent event) {
        melijn.getTaskManager().async(() -> {
            long guildId = event.getGuild().getIdLong();
            long roleId = event.getRole().getIdLong();

            if (roleId == melijn.getVariables().muteRoleCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeRole(guildId, RoleType.MUTE);
                melijn.getVariables().muteRoleCache.invalidate(roleId);

            } else if (roleId == melijn.getVariables().joinRoleCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeRole(guildId, RoleType.JOIN);
                melijn.getVariables().joinRoleCache.invalidate(roleId);

            } else if (roleId == melijn.getVariables().unverifiedRoleCache.getUnchecked(guildId)) {
                melijn.getMySQL().removeRole(guildId, RoleType.UNVERIFIED);
                melijn.getVariables().unverifiedRoleCache.invalidate(roleId);
            }

        });
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getGuild() == null || melijn.getVariables().serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        Lava lava = melijn.getLava();
        AudioLoader loader = lava.getAudioLoader();
        if (melijn.getVariables().userBlackList.contains(guild.getOwnerIdLong())) return;
        long guildId = guild.getIdLong();
        if (lava.isConnected(guildId)) {
            if (event.getChannelLeft() != lava.getConnectedChannel(guild)) return;
            if (someoneIsListening(guild)) {
                String url = melijn.getMySQL().getStreamUrl(guildId);
                if (!url.isEmpty()) {
                    loader.getPlayer(guild).getTrackManager().clear();
                    loader.loadSimpleTrack(loader.getPlayer(guild), url);
                }
            } else {
                loader.getPlayer(guild).getAudioPlayer().setPaused(true);
                if (lava.getConnectedChannel(guild).getMembers().size() > 1) {
                    runLeaveTimer(guild, 300, true);
                } else {
                    runLeaveTimer(guild, 60, false);
                }
            }
        } else if (melijn.getVariables().streamerModeCache.getUnchecked(guildId) &&
                !lava.isConnected(guildId) &&
                event.getChannelJoined().getIdLong() == melijn.getVariables().musicChannelCache.getUnchecked(guildId)) {
            lava.openConnection(guild.getVoiceChannelById(melijn.getVariables().musicChannelCache.getUnchecked(guildId)));
            tryPlayStreamUrl(guild.getIdLong());
            melijn.getTaskManager().async(() -> {
                //Hacky way to unmute bot in afk channel
                if (guild.getAfkChannel() != null && (
                        guild.getSelfMember().hasPermission(guild.getVoiceChannelById(melijn.getVariables().musicChannelCache.getUnchecked(guildId)), Permission.VOICE_MUTE_OTHERS) &&
                                guild.getSelfMember().getVoiceState().getChannel() == null ||
                                guild.getAfkChannel().getIdLong() == guild.getSelfMember().getVoiceState().getChannel().getIdLong())) {
                    guild.getController().setMute(guild.getSelfMember(), true).queue(done ->
                            event.getGuild().getController().setMute(event.getGuild().getSelfMember(), false).queue());
                }
            }, event.getJDA().getPing() + 1800);
        }
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (event.getGuild() == null || melijn.getVariables().serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        Lava lava = melijn.getLava();
        if (melijn.getVariables().userBlackList.contains(guild.getOwnerIdLong())) return;
        long guildId = guild.getIdLong();
        if (melijn.getVariables().streamerModeCache.getUnchecked(guildId) &&
                !lava.isConnected(guildId) &&
                event.getChannelJoined().getIdLong() == (melijn.getVariables().musicChannelCache.getUnchecked(guildId))) {
            lava.openConnection(guild.getVoiceChannelById(melijn.getVariables().musicChannelCache.getUnchecked(guildId)));
            tryPlayStreamUrl(guild.getIdLong());
            melijn.getTaskManager().async(() -> {
                if (guild.getAfkChannel() != null &&
                        (guild.getSelfMember().hasPermission(guild.getVoiceChannelById(melijn.getVariables().musicChannelCache.getUnchecked(guildId)), Permission.VOICE_MUTE_OTHERS) &&
                                guild.getSelfMember().getVoiceState().inVoiceChannel() && (guild.getAfkChannel().getIdLong() == guild.getSelfMember().getVoiceState().getChannel().getIdLong()))) {
                    guild.getController().setMute(guild.getSelfMember(), true).queue(done -> guild.getController().setMute(guild.getSelfMember(), false).queue());
                }
            }, 2000);
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getGuild() == null || melijn.getVariables().serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        if (!event.getMember().equals(event.getGuild().getSelfMember()))
            whenListeningDoActions(event.getGuild());
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        if (event.getGuild() == null || melijn.getVariables().userBlackList.contains(event.getGuild().getIdLong()))
            return;
        if (!event.getMember().equals(event.getGuild().getSelfMember()))
            whenListeningDoActions(event.getGuild());
    }

    private void whenListeningDoActions(Guild guild) {
        Lava lava = melijn.getLava();
        if (!lava.isConnected(guild.getIdLong())) return;
        if (!someoneIsListening(guild)) {
            lava.getAudioLoader().getPlayer(guild).getAudioPlayer().setPaused(true);
            if (lava.getConnectedChannel(guild).getMembers().size() > 1) {
                runLeaveTimer(guild, 300, true);
            } else {
                runLeaveTimer(guild, 60, false);
            }
        }
    }

    private void runLeaveTimer(Guild guild, int seconds, boolean defeaned) {
        AtomicInteger amount = new AtomicInteger();
        Lava lava = melijn.getLava();
        melijn.getTaskManager().async(() -> {
            while (true) {
                Guild guild2 = guild.getJDA().asBot().getShardManager().getGuildById(guild.getIdLong());
                if (guild2 == null)
                    break;
                MusicPlayer player = lava.getAudioLoader().getPlayer(guild2);
                if (!lava.isConnected(guild.getIdLong()))
                    break;
                if (someoneIsListening(guild2)) {
                    player.getAudioPlayer().setPaused(false);
                    break;
                } else if ((lava.getConnectedChannel(guild2).getMembers().size() == 1 && defeaned) || (amount.getAndIncrement() == seconds)) {
                    melijn.getVariables().looped.remove(guild2.getIdLong());
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
        AudioLoader audioLoader = melijn.getLava().getAudioLoader();
        if (audioLoader.getPlayer(guildId).getAudioPlayer().getPlayingTrack() == null) {
            String url = melijn.getMySQL().getStreamUrl(guildId);
            if (!url.isEmpty()) {
                audioLoader.getPlayer(guildId).getTrackManager().clear();
                audioLoader.loadSimpleTrack(audioLoader.getPlayer(guildId), url);
            }
        }
    }

    private boolean someoneIsListening(Guild guild) {
        Lava lava = melijn.getLava();
        if (!lava.isConnected(guild.getIdLong())) return false;
        int doveDuiven = 0;
        for (Member member : lava.getConnectedChannel(guild).getMembers()) {
            if ((member.getVoiceState().isDeafened() || member.getUser().isBot() || member.getVoiceState().isGuildDeafened()) && member != guild.getSelfMember())
                doveDuiven++;
        }
        return (lava.getConnectedChannel(guild).getMembers().size() - doveDuiven) > 1;
    }
}
