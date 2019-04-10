package me.melijn.jda.rest;

import com.sun.management.OperatingSystemMXBean;
import me.melijn.jda.Melijn;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.requests.ratelimit.IBucket;
import org.jooby.Jooby;
import org.jooby.json.Jackson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.management.ManagementFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;

public class Application extends Jooby {

    private final String token;

    public Application(Melijn melijn) {
        token = melijn.getConfig().getValue("cacheToken");
        use(new Jackson());
        get("/guildCount", (request, response) -> response.send(melijn.getShardManager().getGuildCache().size()));
        get("/stats", (req, response) -> {
            OperatingSystemMXBean bean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            long totalMem = bean.getTotalPhysicalMemorySize() >> 20;
            long usedMem = totalMem - (bean.getFreePhysicalMemorySize() >> 20);
            long totalJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax() >> 20;
            long usedJVMMem = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed() >> 20;
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) melijn.getTaskManager().getExecutorService();
            ThreadPoolExecutor scheduledExecutorService = (ThreadPoolExecutor) melijn.getTaskManager().getScheduledExecutorService();

            JSONObject object = new JSONObject();
            object.put("bot", new JSONObject()
                    .put("uptime", melijn.getMessageHelper().getDurationBreakdown(ManagementFactory.getRuntimeMXBean().getUptime()))
                    .put("melijnThreads", (threadPoolExecutor.getActiveCount() + scheduledExecutorService.getActiveCount() + scheduledExecutorService.getQueue().size()))
                    .put("ramUsage",  usedJVMMem)
                    .put("ramTotal", totalJVMMem)
                    .put("jvmThreads", Thread.activeCount())
                    .put("cpuUsage", bean.getProcessCpuLoad())
            );

            object.put("server", new JSONObject()
                    .put("uptime", melijn.getHelpers().getSystemUptime())
                    .put("ramUsage", usedMem)
                    .put("ramTotal", totalMem)
            );


            response.send(object.toMap());
        });
        get("/shards", (request, response) -> {
            JSONObject object = new JSONObject();
            for (JDA shard : melijn.getShardManager().getShardCache())
                object.put(String.valueOf(shard.getShardInfo().getShardId()), new JSONObject()
                        .put("guildCount", shard.getGuildCache().size())
                        .put("userCount", shard.getUserCache().size())
                        .put("connectedVoiceChannels", shard.getGuildCache().stream().filter(guild -> guild.getSelfMember().getVoiceState().inVoiceChannel()).count())
                        .put("ping", shard.getPing())
                        .put("status", shard.getStatus())
                        .put("queuedMessages", QUEUE_SIZE.apply(shard).longValue())
                        .put("responses", shard.getResponseTotal())
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
            String providedToken = request.header("token").toString();
            providedToken = providedToken.substring(1, providedToken.length() - 1);
            if (!providedToken.equals(token)) {
                response.send(new JSONObject()
                        .put("state", "unauthorized")
                        .toMap());
                return;
            }

            //block 1
            melijn.getVariables().prefixes.invalidate(id);
            melijn.getVariables().joinMessages.invalidate(id);
            melijn.getVariables().leaveMessages.invalidate(id);

            //block 2
            melijn.getVariables().joinRoleCache.invalidate(id);
            melijn.getVariables().muteRoleCache.invalidate(id);
            melijn.getVariables().unverifiedRoleCache.invalidate(id);

            //block 3
            melijn.getVariables().joinChannelCache.invalidate(id);
            melijn.getVariables().leaveChannelCache.invalidate(id);
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

    private final Function<JDA, Integer> QUEUE_SIZE = jda -> {
        int sum = 0;
        for (final IBucket bucket : ((JDAImpl) jda).getRequester().getRateLimiter().getRouteBuckets()) {
            sum += bucket.getRequests().size();
        }
        return sum;
    };
}
