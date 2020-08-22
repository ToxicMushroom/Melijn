package me.melijn.melijnbot.internals.web

import com.sun.management.OperatingSystemMXBean
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit


class RestServer(container: Container) {


    private val jsonType = ContentType.parse("Application/JSON")

    private val server: NettyApplicationEngine = embeddedServer(Netty, container.settings.restPort) {
        routing {
            get("/guildCount") {
                call.respondText {
                    "${MelijnBot.shardManager.guildCache.size()}"
                }
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
                val threadPoolExecutor = TaskManager.executorService as ThreadPoolExecutor
                val scheduledExecutorService = TaskManager.scheduledExecutorService as ThreadPoolExecutor

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

                call.respondText(dataObject.toString(), jsonType)
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
                        .put("unavailable", shard.unavailableGuilds.size)

                    dataArray.add(dataObject)
                }
                call.respondText(dataArray.toString(), jsonType)
            }


            get("/guild/{id}") {
                val id = call.parameters["id"] ?: return@get
                if (!id.isPositiveNumber()) return@get
                val guild = MelijnBot.shardManager.getGuildById(id)
                if (guild == null) {
                    call.respondText(DataObject.empty()
                        .put("isBotMember", false)
                        .toString(), jsonType)
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

                call.respondText(DataObject.empty()
                    .put("name", guild.name)
                    .put("iconUrl", if (guild.iconUrl == null) MISSING_IMAGE_URL else guild.iconUrl)
                    .put("memberCount", guild.memberCount)
                    .put("ownerId", guild.ownerId)
                    .put("isBotMember", true)
                    .put("voiceChannels", voiceChannels)
                    .put("textChannels", textChannels)
                    .put("roles", roles)
                    .toString(), jsonType)
            }

            post("/guild/{id}") {
                val id = call.parameters["id"] ?: return@post
                if (!id.isPositiveNumber()) return@post

                val guild = MelijnBot.shardManager.getGuildById(id)

                val userId = call.receiveText()
                val member = guild?.retrieveMemberById(userId)?.awaitOrNull()
                if (member == null) {
                    call.respondText(DataObject.empty()
                        .put("error", "guild invalidated")
                        .toString(), jsonType)
                    return@post
                }

                val hasPerm = (member.hasPermission(Permission.ADMINISTRATOR) ||
                    member.hasPermission(Permission.MANAGE_SERVER) ||
                    member.isOwner)

                if (!hasPerm) {
                    call.respondText(DataObject.empty()
                        .put("error", "guild invalidated")
                        .toString(), jsonType)
                    return@post
                }

                val guildData = DataObject.empty()
                    .put("id", id)
                    .put("name", guild.name)
                    .put("icon", guild.iconId)

                call.respondText { guildData.toString() }
            }

            post("/upgradeGuilds") {
                val partialGuilds = DataArray.fromJson(call.receiveText())
                val shardManager = MelijnBot.shardManager

                val upgradedGuilds = DataArray.empty()

                for (i in 0 until partialGuilds.length()) {
                    val upgradedGuild = DataObject.empty()
                    val partialGuild = partialGuilds.getObject(i)
                    val isOwner = partialGuild.getBoolean("owner")
                    val name = partialGuild.getString("name")
                    val icon = partialGuild.getString("icon", "null")
                    val permissions = partialGuild.getInt("permissions")
                    val id = partialGuild.getLong("id")

                    // Owner, admin or manage_guild
                    val hasPerms = isOwner || (permissions and 0x00000020) != 0 || (permissions and 0x00000008) != 0
                    if (!hasPerms) continue

                    val fullGuild = shardManager.getGuildById(id)
                    val hasMelijn = fullGuild?.let { true } ?: false
                    if (!hasMelijn) continue

                    upgradedGuild.put("id", "$id")
                    upgradedGuild.put("name", name)
                    upgradedGuild.put("icon", icon)

                    upgradedGuilds.add(upgradedGuild)
                }


                call.respondText(upgradedGuilds.toString())
            }

            get("/member/{guildId}/{userId}") {

                val shardManager = MelijnBot.shardManager
                val guild = call.parameters["guildId"]?.let { guildId ->
                    if (guildId.isPositiveNumber()) shardManager.getGuildById(guildId) else null
                }

                if (guild == null) {
                    call.respondText(DataObject.empty()
                        .put("error", "Invalid guildId")
                        .put("isMember", false)
                        .toString(), jsonType)
                    return@get
                }


                val user = context.parameters["userId"]?.let { userId ->
                    if (userId.isPositiveNumber()) shardManager.retrieveUserById(userId).awaitOrNull() else null
                }

                val member = user?.let {
                    guild.retrieveMember(it).awaitOrNull()
                }
                if (member == null) {
                    call.respondText(DataObject.empty()
                        .put("error", "Member not found")
                        .put("isMember", false)
                        .toString(), jsonType)
                    return@get
                }


                call.respondText(DataObject.empty()
                    .put("isMember", true)
                    .put("isAdmin", member.hasPermission(Permission.ADMINISTRATOR) || member.isOwner)
                    .toString(), jsonType)
            }

            get("/translate/{language}/{path}") {
                val lang = call.parameters["language"] ?: return@get
                val path = call.parameters["path"] ?: return@get
                val translation = i18n.getTranslation(lang, path)

                call.respondText(DataObject.empty()
                    .put("isSame", path == translation)
                    .put("translation", translation)
                    .toString())
            }

            get("/translations/{language}") {
                val lang = call.parameters["language"] ?: return@get
                val data = i18n.getTranslations(lang)
                call.respondText(data.toString(), jsonType)
            }

            get("/fullCommands") {
                val dataObject = DataObject.empty()
                for ((_, root) in container.commandMap) {
                    if (root.commandCategory == CommandCategory.DEVELOPER) continue
                    val dataArray = if (dataObject.hasKey(root.commandCategory.toString())) {
                        dataObject.getArray(root.commandCategory.toString())
                    } else {
                        DataArray.empty()
                    }
                    val darr = getDataArrayArrayFrom(arrayOf(root)).getArray(0)
                    dataArray.add(darr)
                    dataObject.put(root.commandCategory.toString(), dataArray)
                }
                call.respondText(dataObject.toString(), jsonType)
            }

            get("/timezones") {
                call.respondText(DataArray.fromCollection(TimeZone.getAvailableIDs().toList()).toString(), jsonType)
            }

            post("/voted") {
                if (call.request.header("Authorization") != container.settings.tokens.melijnRest) {
                    call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
                    return@post
                }

                val body = DataObject.fromJson(call.receiveText())
                TaskManager.async {
                    val credits = body.getLong("mel")
                    val userId = body.getLong("user")
                    val votes = body.getInt("votes")
                    val streak = body.getInt("streak")
                    val wrapper = container.daoManager.balanceWrapper

                    val newBalance = wrapper.getBalance(userId) + credits
                    wrapper.setBalance(userId, newBalance)

                    LogUtils.sendReceivedVoteRewards(container, userId, newBalance, credits, streak, votes)
                }

                call.respondText { "success" }
            }

            post("/getsettings/general/{guildId}") {
                val id = call.parameters["guildId"] ?: return@post
                if (!id.isPositiveNumber()) return@post

                val guild = MelijnBot.shardManager.getGuildById(id)

                val userId = call.receiveText()
                val member = guild?.retrieveMemberById(userId)?.awaitOrNull()
                if (member == null) {
                    call.respondText(DataObject.empty()
                        .put("error", "guild invalidated")
                        .toString(), jsonType)
                    return@post
                }

                val idLong = guild.idLong

                val hasPerm = (member.hasPermission(Permission.ADMINISTRATOR) ||
                    member.hasPermission(Permission.MANAGE_SERVER) ||
                    member.isOwner)

                if (!hasPerm) {
                    call.respondText(DataObject.empty()
                        .put("error", "guild invalidated")
                        .toString(), jsonType)
                    return@post
                }

                val guildData = DataObject.empty()
                    .put("id", id)
                    .put("name", guild.name)
                    .put("icon", guild.iconId)

                val jobs = mutableListOf<Job>()
                val settings = DataObject.empty()
                val daoManager = container.daoManager

                jobs.add(TaskManager.async {
                    val prefixes = DataArray.fromCollection(daoManager.guildPrefixWrapper.getPrefixes(idLong))
                    settings.put("prefixes", prefixes)
                })

                jobs.add(TaskManager.async {
                    val allowed = daoManager.allowSpacedPrefixWrapper.getGuildState(idLong)
                    settings.put("allowSpacePrefix", allowed)
                })

                jobs.add(TaskManager.async {
                    val ec = daoManager.embedColorWrapper.getColor(idLong)
                    settings.put("embedColor", ec)
                })

                jobs.add(TaskManager.async {
                    val tz = daoManager.timeZoneWrapper.getTimeZone(idLong)
                    settings.put("timeZone", tz)
                })

                jobs.add(TaskManager.async {
                    val language = daoManager.guildLanguageWrapper.getLanguage(idLong)
                    settings.put("language", language)
                })

                val disabled = daoManager.embedDisabledWrapper.embedDisabledCache.contains(idLong)
                settings.put("embedsDisabled", disabled)

                val provided = DataObject.empty()
                provided.put("timezones", DataArray.fromCollection(TimeZone.getAvailableIDs().toList()))

                jobs.joinAll()

                call.respondText {
                    DataObject.empty()
                        .put("guild", guildData)
                        .put("settings", settings)
                        .put("provided", provided)
                        .toString()
                }
            }

            //Has to be registered last to not override other paths
            get("*") {
                call.respondText("blub")
            }
        }
    }

    var i = 0
    private fun getDataArrayArrayFrom(children: Array<AbstractCommand>): DataArray {
        val dataArray = DataArray.empty()
        for (c in children) {
            i++
            val innerDataArray = DataArray.empty()

            innerDataArray.add(c.name) // 0
            innerDataArray.add(i18n.getTranslation("en", c.description)) // 1
            innerDataArray.add(i18n.getTranslation("en", c.syntax)) // 2
            innerDataArray.add(DataArray.fromCollection(c.aliases.toList())) // 3
            val argumentsHelp = i18n.getTranslationN("en", c.arguments, false)
            innerDataArray.add(argumentsHelp?.replace("%help\\.arg\\..*?%".toRegex()) {
                it.groups[0]?.let { (value) ->
                    i18n.getTranslation("en", value.substring(1, value.length - 1))
                } ?: "report to devs it's BROKEN :c"
            } ?: "") // 4
            innerDataArray.add(
                DataArray.fromCollection(c.discordChannelPermissions.map { it.toString() })
            ) // 5
            innerDataArray.add(
                DataArray.fromCollection(c.discordPermissions.map { it.toString() })
            ) // 6
            innerDataArray.add(
                DataArray.fromCollection(c.runConditions.map { it.toString() })
            ) // 7
            innerDataArray.add(
                c.permissionRequired
            ) // 8
            innerDataArray.add(getDataArrayArrayFrom(c.children)) // 9
            innerDataArray.add(i18n.getTranslationN("en", c.help, false) ?: "") // 10
            innerDataArray.add(i18n.getTranslationN("en", c.examples, false) ?: "") // 11
            dataArray.add(innerDataArray)
        }
        return dataArray
    }

    fun stop() {
        server.stop(0, 2, TimeUnit.SECONDS)
    }

    fun start() {
        server.start(false)
    }
}