package me.melijn.jda.events;

import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.blub.ChannelType;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.discordbots.api.client.DiscordBotListAPI;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class JoinLeave extends ListenerAdapter {

    private boolean started = false;

    private final Melijn melijn;

    public JoinLeave(Melijn melijn) {
        this.melijn = melijn;
    }

    @Override
    public void onReady(ReadyEvent event) {
        melijn.getVariables().startTime = System.currentTimeMillis();
        ShardManager shardManager = event.getJDA().asBot().getShardManager();
        if (started || shardManager.getShardCache().stream().filter(shard -> shard.getStatus().equals(JDA.Status.CONNECTED)).count() == shardManager.getShardsTotal())
            return;
        started = true;

        melijn.getVariables().dblAPI = new DiscordBotListAPI.Builder()
                .token(melijn.getConfig().getValue("dbltoken"))
                .botId(event.getJDA().getSelfUser().getId())
                .build();

        melijn.getHelpers().startTimer(event.getJDA(), 0);

        AudioLoader audioLoader = melijn.getLava().getAudioLoader();
        List<JSONObject> tracks = melijn.getMySQL().getQueues();
        melijn.getMySQL().clearQueues();

        for (JSONObject queue : tracks) {
            Guild guild = shardManager.getGuildById(queue.getLong("guildId"));
            if (guild == null) return;
            VoiceChannel vc = guild.getVoiceChannelById(queue.getLong("channelId"));
            if (vc == null) return;

            if (melijn.getLava().tryToConnectToVCSilent(vc)) {
                boolean pause = queue.getBoolean("paused");
                String[] urls = queue.getString("urls").split("\n");
                audioLoader.getPlayer(guild).getAudioPlayer().setPaused(pause);
                for (String url : urls) {
                    if (!url.startsWith("#0 "))
                        audioLoader.loadSimpleTrack(audioLoader.getPlayer(guild), url.replaceFirst("#\\d+ ", ""));
                }
            }
        }

    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong())) {
            return;
        }
        Guild guild = event.getGuild();
        User joinedUser = event.getUser();
        if (joinedUser.isBot() && joinedUser.equals(guild.getSelfMember().getUser()) &&
                melijn.getVariables().blockedGuildIds.contains(guild.getOwnerIdLong()))
            guild.leave().queue();
        if (melijn.getVariables().blockedUserIds.contains(guild.getOwnerIdLong())) return;
        if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) &&
                melijn.getVariables().verificationChannelsCache.getUnchecked(guild.getIdLong()) != -1) {
            TextChannel verificationChannel = guild.getTextChannelById(melijn.getVariables().verificationChannelsCache.getUnchecked(guild.getIdLong()));
            if (verificationChannel != null) {
                Map<Long, Long> newList = melijn.getVariables().unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong());
                long nanoTime = System.nanoTime();
                newList.put(joinedUser.getIdLong(), nanoTime);
                melijn.getMySQL().addUnverifiedUser(guild.getIdLong(), joinedUser.getIdLong(), nanoTime);
                melijn.getVariables().unVerifiedGuildMembersCache.put(guild.getIdLong(), newList);

                Role role = guild.getRoleById(melijn.getVariables().unverifiedRoleCache.getUnchecked(guild.getIdLong()));
                if (role != null && guild.getSelfMember().canInteract(role))
                    guild.getController().addSingleRoleToMember(event.getMember(), role).reason("unverified user").queue();
            } else {
                melijn.getTaskManager().async(() -> {
                    melijn.getMySQL().removeChannel(guild.getIdLong(), ChannelType.VERIFICATION);
                    melijn.getVariables().verificationChannelsCache.invalidate(guild.getIdLong());
                });
            }
        } else {
            melijn.getHelpers().joinCode(guild, joinedUser);
        }
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        User leftUser = event.getUser();
        if (melijn.getVariables().blockedUserIds.contains(guild.getOwnerIdLong())) return;
        if (melijn.getVariables().unVerifiedGuildMembersCache.getUnchecked(guild.getIdLong()).keySet().contains(leftUser.getIdLong())) {
            melijn.getHelpers().removeUnverified(guild, leftUser);
        } else {
            melijn.getTaskManager().async(() -> {
                String message = melijn.getVariables().leaveMessages.getUnchecked(guild.getIdLong());
                if (message.isEmpty()) return;
                TextChannel leaveChannel = guild.getTextChannelById(melijn.getVariables().leaveChannelCache.getUnchecked(guild.getIdLong()));
                if (leaveChannel == null || !guild.getSelfMember().hasPermission(leaveChannel, Permission.MESSAGE_WRITE))
                    return;
                leaveChannel.sendMessage(melijn.getMessageHelper().variableFormat(message, guild, leftUser)).queue();
            });
        }
    }
}
