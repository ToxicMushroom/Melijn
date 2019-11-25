package me.melijn.melijnbot.objects.web

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.getDurationString
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import org.jooby.Jooby
import org.jooby.json.Jackson
import java.lang.management.ManagementFactory
import java.util.concurrent.ThreadPoolExecutor


class RestServer(container: Container) : Jooby() {
    init {
        val token = container.settings.tokens.melijnRest
        use(Jackson())

        get("/guildCount") { _, rsp ->
            rsp.send(MelijnBot.shardManager.guildCache.size())
        }


        get("/stats") { _, rsp ->
            val bean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
            val totalMem: Long = bean.totalPhysicalMemorySize shr 20
            val usedMem: Long = totalMem - (bean.freePhysicalMemorySize shr 20)
            val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
            val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
            val threadPoolExecutor = container.taskManager.executorService as ThreadPoolExecutor
            val scheduledExecutorService = container.taskManager.scheduledExecutorService as ThreadPoolExecutor

            val dataObject = DataObject.empty()
            dataObject.put("bot", DataObject.empty()
                .put("uptime", getDurationString(ManagementFactory.getRuntimeMXBean().uptime))
                .put("melijnThreads", threadPoolExecutor.activeCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size)
                .put("ramUsage", usedJVMMem)
                .put("ramTotal", totalJVMMem)
                .put("jvmThreads", Thread.activeCount())
                .put("cpuUsage", bean.processCpuLoad)
            )

            dataObject.put("server", DataObject.empty()
                .put("uptime", container.uptimeMillis)
                .put("ramUsage", usedMem)
                .put("ramTotal", totalMem)
            )


            rsp.send(dataObject.toMap())
        }


        get("/shards") { _, rsp ->
            val dataObject = DataObject.empty()
            val shardManager = MelijnBot.shardManager
            for (shard in shardManager.shardCache) {

                dataObject.put(shard.shardInfo.shardId.toString(), DataObject.empty()
                    .put("guildCount", shard.guildCache.size())
                    .put("userCount", shard.userCache.size())
                    .put("connectedVoiceChannels", VoiceUtil.getConnectedChannelsAmount(shardManager))
                    .put("ping", shard.gatewayPing)
                    .put("status", shard.status)
                    .put("queuedMessages", queueSize(shard))
                    .put("responses", shard.responseTotal)
                )
            }
            rsp.send(dataObject.toMap())
        }


        get("/guild/{id:\\d+}") { req, rsp ->
            val id = req.param("id").longValue()
            val guild = MelijnBot.shardManager.getGuildById(id)
            if (guild == null) {
                rsp.send(DataObject.empty()
                    .put("isBotMember", false)
                    .toMap())
                return@get
            }

            val voiceChannels = DataArray.empty()
            val textChannels = DataArray.empty()
            val roles = DataArray.empty()

            guild.voiceChannelCache.forEach { voiceChannel ->
                voiceChannels.add(DataObject.empty()
                    .put("position", voiceChannel.position)
                    .put("id", voiceChannel.idLong)
                    .put("name", voiceChannel.name)
                )
            }

            guild.textChannelCache.forEach { textChannel ->
                textChannels.add(DataObject.empty()
                    .put("position", textChannel.position)
                    .put("id", textChannel.idLong)
                    .put("name", textChannel.name)
                )
            }

            guild.roleCache.forEach { role ->
                roles.add(DataObject.empty()
                    .put("id", role.idLong)
                    .put("name", role.name)
                )
            }
            rsp.send(DataObject.empty()
                .put("name", guild.name)
                .put("iconUrl", if (guild.iconUrl == null) MISSING_IMAGE_URL else guild.iconUrl)
                .put("memberCount", guild.memberCache.size())
                .put("ownerId", guild.ownerId)
                .put("isBotMember", true)
                .put("voiceChannels", voiceChannels)
                .put("textChannels", textChannels)
                .put("roles", roles)
                .toMap())
        }


        get("/member/{guildId:\\d+}/{userId:\\d+}") { req, rsp ->
            val shardManager = MelijnBot.shardManager
            val guild = shardManager.getGuildById(req.param("guildId").longValue())
            if (guild == null) {
                rsp.send(DataObject.empty()
                    .put("error", "Invalid guildId")
                    .put("isMember", false)
                    .toMap())
                return@get
            }

            val user = shardManager.getUserById(req.param("userId").longValue())
            val member = user?.let { guild.getMember(it) }
            if (member == null) {
                rsp.send(DataObject.empty()
                    .put("error", "Member not found")
                    .put("isMember", false)
                    .toMap())
                return@get
            }

            rsp.send(DataObject.empty()
                .put("isMember", true)
                .put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner)
                .toMap())
        }


        get("/translate/{language:.*}/{path:.*}") { req, rsp ->
            val lang = req.param("language").value()
            val path = req.param("path").value()
            val translation = i18n.getTranslation(lang, path)
            rsp.send(DataObject.empty()
                .put("isSame", path == translation)
                .put("translation", translation)
                .toMap())
        }

        get("/translations/{language:.*}") { req, rsp ->
            val lang = req.param("language").value()
            val data =   i18n.getTranslations(lang)
            rsp.send(data.toMap())
        }

        //Has to be registered last to not override other paths
        get("*") { -> "blub" }
    }

    private fun queueSize(jda: JDA): Long {
        var sum = 0
        for (bucket in (jda as JDAImpl).requester.rateLimiter.routeBuckets) {
            sum += bucket.requests.size
        }
        return sum.toLong()
    }
}