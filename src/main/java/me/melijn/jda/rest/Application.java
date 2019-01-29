package me.melijn.jda.rest;

import me.melijn.jda.Melijn;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.jooby.Jooby;
import org.jooby.json.Jackson;
import org.json.JSONArray;
import org.json.JSONObject;

public class Application extends Jooby {


    public Application(Melijn melijn) {
        use(new Jackson());
        get("/guildCount", (request, response) -> response.send(melijn.getShardManager().getGuildCache().size()));
        get("/shards", (request, response) -> {
            JSONObject object = new JSONObject();
            for (JDA shard : melijn.getShardManager().getShardCache())
                object.put(String.valueOf(shard.getShardInfo().getShardId()), new JSONObject()
                        .put("guildCount", shard.getGuildCache().size())
                        .put("userCount", shard.getUserCache().size())
                        .put("connectedVoiceChannels", shard.getGuildCache().stream().filter(guild -> guild.getSelfMember().getVoiceState().inVoiceChannel()).count())
                );
            response.send(object.toMap());
        });
        get("/guild/{id:\\d+}", (request, response) -> {
            long id = request.param("id").longValue();
            Guild guild = melijn.getShardManager().getGuildById(id);
            if (guild == null) {
                response.send(new JSONObject().put("isBotMember", false).toMap());
                return;
            }
            JSONObject channels = new JSONObject()
                    .put("textChannels", new JSONArray())
                    .put("voiceChannels", new JSONArray());
            JSONArray roles = new JSONArray();

            guild.getRoleCache().forEach(role -> roles.put(new JSONObject().put("id", role.getId()).put("name", role.getName())));
            guild.getVoiceChannelCache().forEach(channel -> channels.put("voiceChannels", channels.getJSONArray("voiceChannels").put(channel.getPosition(), new JSONObject().put("id", channel.getId()).put("name", channel.getName()))));
            guild.getTextChannelCache().forEach(channel -> channels.put("textChannels", channels.getJSONArray("textChannels").put(channel.getPosition(), new JSONObject().put("id", channel.getId()).put("name", channel.getName()))));
            response.send(new JSONObject()
                    .put("name", guild.getName())
                    .put("iconUrl", guild.getIconUrl() == null ? "https://melijn.com/data/discord.jpg" : guild.getIconUrl())
                    .put("memberCount", guild.getMemberCache().size())
                    .put("ownerId", guild.getOwnerId())
                    .put("isBotMember", true)
                    .put("channels", channels)
                    .put("roles", roles)
                    .toMap());
        });
        get("/guild/{id:\\d+}/refreshCache", (request, response) -> {
            long id = request.param("id").longValue();

            //block 1
            melijn.getVariables().prefixes.invalidate(id);
            melijn.getVariables().joinMessages.invalidate(id);
            melijn.getVariables().leaveMessages.invalidate(id);

            //block 2
            melijn.getVariables().joinRoleCache.invalidate(id);
            melijn.getVariables().muteRoleCache.invalidate(id);
            melijn.getVariables().unverifiedRoleCache.invalidate(id);

            //block 3
            melijn.getVariables().welcomeChannelCache.invalidate(id);
            melijn.getVariables().verificationChannelsCache.invalidate(id);
            melijn.getVariables().musicLogChannelCache.invalidate(id);
            melijn.getVariables().banLogChannelCache.invalidate(id);
            melijn.getVariables().muteLogChannelCache.invalidate(id);
            melijn.getVariables().kickLogChannelCache.invalidate(id);
            melijn.getVariables().warnLogChannelCache.invalidate(id);
            melijn.getVariables().sdmLogChannelCache.invalidate(id);
            melijn.getVariables().odmLogChannelCache.invalidate(id);
            melijn.getVariables().pmLogChannelCache.invalidate(id);
            melijn.getVariables().fmLogChannelCache.invalidate(id);

            //block 4
            melijn.getVariables().verificationCodeCache.invalidate(id);
            melijn.getVariables().verificationThresholdCache.invalidate(id);

            //block 5
            melijn.getVariables().musicChannelCache.invalidate(id);
            melijn.getVariables().streamerModeCache.invalidate(id);
            response.send(new JSONObject()
                    .put("state", "refreshed")
                    .toMap());
        });


        get("/member/{guildId:\\d+}/{userId:\\d+}", (request, response) -> {
            Guild guild = melijn.getShardManager().getGuildById(request.param("guildId").longValue());
            if (guild == null) {
                response.send(new JSONObject()
                        .put("error", "Invalid guildId")
                        .put("isMember", false)
                        .toMap());
                return;
            }
            User user = melijn.getShardManager().getUserById(request.param("userId").longValue());
            if (user == null || guild.getMember(user) == null) {
                response.send(new JSONObject()
                        .put("error", "Member not found")
                        .put("isMember", false)
                        .toMap());
                return;
            }
            Member member = guild.getMember(user);
            response.send(new JSONObject()
                    .put("isMember", true)
                    .put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner())
                    .toMap());
        });
        get("*", () -> "blub");
    }
}
