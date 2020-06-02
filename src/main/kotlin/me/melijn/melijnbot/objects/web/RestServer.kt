package me.melijn.melijnbot.objects.web

import com.sun.management.OperatingSystemMXBean
import io.jooby.Context
import io.jooby.Jooby
import io.jooby.json.JacksonModule
import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.events.eventutil.VoiceUtil
import me.melijn.melijnbot.objects.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.OSValidator
import me.melijn.melijnbot.objects.utils.awaitOrNull
import me.melijn.melijnbot.objects.utils.getSystemUptime
import me.melijn.melijnbot.objects.utils.getUnixRam
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.ThreadPoolExecutor


class RestServer(container: Container) : Jooby() {

    fun Context.send(any: Any) {
        this.send(any.toString())
    }

    init {
        //val token = container.settings.tokens.melijnRest
        install(JacksonModule())

        get("/guildCount") { context ->
            context.send(MelijnBot.shardManager.guildCache.size())
        }


        get("/stats") {
            val bean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
            val totalMem = bean.totalPhysicalMemorySize shr 20

            val usedMem = if (OSValidator.isUnix) {
                totalMem - getUnixRam()
            } else {
                totalMem - (bean.freeSwapSpaceSize shr 20)
            }
            val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
            val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
            val threadPoolExecutor = container.taskManager.executorService as ThreadPoolExecutor
            val scheduledExecutorService = container.taskManager.scheduledExecutorService as ThreadPoolExecutor

            val dataObject = DataObject.empty()
            dataObject.put("bot", DataObject.empty()
                .put("uptime", ManagementFactory.getRuntimeMXBean().uptime)
                .put("melijnThreads", threadPoolExecutor.activeCount + scheduledExecutorService.activeCount + scheduledExecutorService.queue.size)
                .put("ramUsage", usedJVMMem)
                .put("ramTotal", totalJVMMem)
                .put("jvmThreads", Thread.activeCount())
                .put("cpuUsage", bean.processCpuLoad * 100)
            )

            dataObject.put("server", DataObject.empty()
                .put("uptime", getSystemUptime())
                .put("ramUsage", usedMem)
                .put("ramTotal", totalMem)
            )

            dataObject.toMap()
        }


        get("/shards") {

            val shardManager = MelijnBot.shardManager
            val dataArray = DataArray.empty()
            val players = container.lavaManager.musicPlayerManager.getPlayers()

            for (shard in shardManager.shardCache) {
                var queuedTracks = 0
                var musicPlayers = 0
                for (player in players.values) {
                    if (shard.guildCache.getElementById(player.guildId) != null) {
                        if (player.guildTrackManager.iPlayer.playingTrack != null) {
                            musicPlayers++
                        }
                        queuedTracks += player.guildTrackManager.trackSize()
                    }
                }

                val dataObject = DataObject.empty()
                dataObject
                    .put("guildCount", shard.guildCache.size())
                    .put("userCount", shard.userCache.size())
                    .put("connectedVoiceChannels", VoiceUtil.getConnectedChannelsAmount(shard))
                    .put("listeningVoiceChannels", VoiceUtil.getConnectedChannelsAmount(shard, true))
                    .put("ping", shard.gatewayPing)
                    .put("status", shard.status)
                    .put("queuedTracks", queuedTracks)
                    .put("musicPlayers", musicPlayers)
                    .put("responses", shard.responseTotal)
                    .put("id", shard.shardInfo.shardId)

                dataArray.add(dataObject)
            }
            dataArray.toList()
        }


        get("/guild/{id:\\d+}") { context ->

            val id = context.path("id").longValue()
            val guild = MelijnBot.shardManager.getGuildById(id)
                ?: return@get DataObject.empty()
                    .put("isBotMember", false)
                    .toMap()

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

            DataObject.empty()
                .put("name", guild.name)
                .put("iconUrl", if (guild.iconUrl == null) MISSING_IMAGE_URL else guild.iconUrl)
                .put("memberCount", guild.memberCount)
                .put("ownerId", guild.ownerId)
                .put("isBotMember", true)
                .put("voiceChannels", voiceChannels)
                .put("textChannels", textChannels)
                .put("roles", roles)
                .toMap()
        }


        get("/member/{guildId:\\d+}/{userId:\\d+}") { context ->

            val shardManager = MelijnBot.shardManager
            val guild = shardManager.getGuildById(context.path("guildId").longValue())
                ?: return@get DataObject.empty()
                    .put("error", "Invalid guildId")
                    .put("isMember", false)
                    .toMap()

            runBlocking {
                val user = shardManager.retrieveUserById(context.path("userId").longValue()).awaitOrNull()

                val member = user?.let {
                    guild.retrieveMember(it).awaitOrNull()
                } ?: return@runBlocking DataObject.empty()
                    .put("error", "Member not found")
                    .put("isMember", false)
                    .toMap()


                DataObject.empty()
                    .put("isMember", true)
                    .put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner)
                    .toMap()
            }
        }


        get("/translate/{language:.+}/{path:.+}") { context ->
            val lang = context.path("language").value()
            val path = context.path("path").value()
            val translation = i18n.getTranslation(lang, path)
            DataObject.empty()
                .put("isSame", path == translation)
                .put("translation", translation)
                .toMap()
        }

        get("/translations/{language:.+}") { context ->
            val lang = context.path("language").value()
            val data = i18n.getTranslations(lang)
            data.toMap()
        }

        get("/commands") {
            container.commandMap
        }

        get("/fullCommands") {
            val dataObject = DataObject.empty()
            for ((_, root) in container.commandMap) {
                val dataArray = if (dataObject.hasKey(root.commandCategory.toString())) {
                    dataObject.getArray(root.commandCategory.toString())
                } else {
                    DataArray.empty()
                }
                val darr = getDataArrayArrayFrom(arrayOf(root)).getArray(0)
                dataArray.add(darr)
                dataObject.put(root.commandCategory.toString(), dataArray)
            }
            dataObject.toMap()
        }

        get("/timezones") {
            TimeZone.getAvailableIDs()
        }

        //Has to be registered last to not override other paths
        get("*") { "blub" }
    }

    var i = 0
    private fun getDataArrayArrayFrom(children: Array<AbstractCommand>): DataArray {
        val dataArray = DataArray.empty()
        for (c in children) {
            i++
            val innerDataArray = DataArray.empty()
            innerDataArray.add(c.name)
            innerDataArray.add(i18n.getTranslation("en", c.description))
            innerDataArray.add(i18n.getTranslation("en", c.syntax))
            innerDataArray.add(DataArray.fromCollection(c.aliases.toList()))
            val translatedHelp = i18n.getTranslation("en", c.help)
            innerDataArray.add(translatedHelp.replace("%help\\.arg\\..*?%".toRegex()) {
                it.groups[0]?.let { (value) ->
                    i18n.getTranslation("en", value.substring(1, value.length -1))
                } ?: "report to devs it's BROKEN :c"
            })
            innerDataArray.add(
                DataArray.fromCollection(c.discordChannelPermissions.map { it.toString() })
            )
            innerDataArray.add(
                DataArray.fromCollection(c.discordPermissions.map { it.toString() })
            )
            innerDataArray.add(
                DataArray.fromCollection(c.runConditions.map { it.toString() })
            )
            innerDataArray.add(
                c.permissionRequired
            )
            innerDataArray.add(getDataArrayArrayFrom(c.children))
            dataArray.add(innerDataArray)
        }
        return dataArray
    }
}