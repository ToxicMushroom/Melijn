package me.melijn.jda.events;

import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.blub.RoleType;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceDeafenEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.role.RoleDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

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
            melijn.getMySQL().removeTextChannelEverywhere(guildId, channelId);
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
            melijn.getMySQL().removeRoleEverywhere(guildId, roleId);
        });
    }

    @Override
    public void onGuildVoiceMove(GuildVoiceMoveEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        Lava lava = melijn.getLava();
        AudioLoader loader = lava.getAudioLoader();
        if (melijn.getVariables().blockedUserIds.contains(guild.getOwnerIdLong())) return;
        long guildId = guild.getIdLong();
        if (lava.isConnected(guildId)) {
            VoiceChannel botVC = lava.getConnectedChannel(guild);
            if (event.getChannelJoined() != botVC && event.getChannelLeft() != botVC) return;
            if (event.getChannelJoined() == lava.getConnectedChannel(guild) && someoneIsListening(guild))
                melijn.getVariables().toLeaveTimeMap.remove(guildId);
            if (someoneIsListening(guild)) {
                lava.getAudioLoader().getPlayer(guild).getAudioPlayer().setPaused(false);
                if (loader.getPlayer(guildId).getAudioPlayer().getPlayingTrack() != null) return;
                String url = melijn.getMySQL().getStreamUrl(guildId);
                if (!url.isEmpty()) {
                    loader.getPlayer(guild).getTrackManager().clear();
                    loader.loadSimpleTrack(loader.getPlayer(guild), url);
                }
            } else {
                loader.getPlayer(guild).getAudioPlayer().setPaused(true);
                if (lava.getConnectedChannel(guild).getMembers().size() > 1) {
                    melijn.getVariables().toLeaveTimeMap.put(guildId, System.currentTimeMillis() + 300_000);
                } else {
                    melijn.getVariables().toLeaveTimeMap.put(guildId, System.currentTimeMillis() + 60_000);
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
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        Lava lava = melijn.getLava();
        if (melijn.getVariables().blockedUserIds.contains(guild.getOwnerIdLong())) return;
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
            }, event.getJDA().getPing() + 2000);
        } else if (lava.isConnected(guildId) && someoneIsListening(guild)) {
            melijn.getVariables().toLeaveTimeMap.remove(guildId);
            lava.getAudioLoader().getPlayer(guild).getAudioPlayer().setPaused(false);
        }
    }

    @Override
    public void onGuildVoiceLeave(GuildVoiceLeaveEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()))
            return;
        if (!event.getMember().equals(event.getGuild().getSelfMember())) {
            whenListeningDoActions(event.getGuild());
        } else {
            melijn.getVariables().toLeaveTimeMap.remove(event.getGuild().getIdLong());
        }
    }

    @Override
    public void onGuildVoiceDeafen(GuildVoiceDeafenEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedUserIds.contains(event.getGuild().getIdLong()))
            return;
        if (!event.getMember().equals(event.getGuild().getSelfMember())) {
            whenListeningDoActions(event.getGuild());
        } else {
            melijn.getVariables().toLeaveTimeMap.remove(event.getGuild().getIdLong());
        }
    }

    private void whenListeningDoActions(Guild guild) {
        Lava lava = melijn.getLava();
        if (!lava.isConnected(guild.getIdLong())) return;
        if (!someoneIsListening(guild)) {
            lava.getAudioLoader().getPlayer(guild).getAudioPlayer().setPaused(true);
            if (lava.getConnectedChannel(guild).getMembers().size() > 1) {
                melijn.getVariables().toLeaveTimeMap.put(guild.getIdLong(), System.currentTimeMillis() + 300_000);
            } else {
                melijn.getVariables().toLeaveTimeMap.put(guild.getIdLong(), System.currentTimeMillis() + 60_000);
            }
        } else {
            melijn.getVariables().toLeaveTimeMap.remove(guild.getIdLong());
        }
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
