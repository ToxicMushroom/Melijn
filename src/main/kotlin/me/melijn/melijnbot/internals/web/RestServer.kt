package me.melijn.melijnbot.internals.web

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
import me.melijn.melijnbot.commands.administration.PREFIXES_LIMIT
import me.melijn.melijnbot.commands.administration.PREMIUM_PREFIXES_LIMIT
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.events.eventutil.VoiceUtil
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.web.rest.convert.UpgradeGuildsResponseHandler
import me.melijn.melijnbot.internals.web.rest.info.GetGuildResponseHandler
import me.melijn.melijnbot.internals.web.rest.info.PostGuildResponseHandler
import me.melijn.melijnbot.internals.web.rest.member.MemberInfoResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.GetGeneralSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.PostGeneralSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.stats.StatsResponseHandler
import me.melijn.melijnbot.internals.web.rest.voted.VotedResponseHandler
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit


class RestServer(container: Container) {


    private val jsonType = ContentType.parse("Application/JSON")


    private val server: NettyApplicationEngine = embeddedServer(Netty, container.settings.restServer.port) {
        routing {
            get("/guildCount") {
                call.respondText {
                    "${MelijnBot.shardManager.guildCache.size()}"
                }
            }


            get("/stats") {
                StatsResponseHandler.handleStatsResponse(RequestContext(call, container))
            }

            get("/shards") {
                StatsResponseHandler.handleShardsResponse(RequestContext(call, container))
            }


            get("/guild/{id}") {
                GetGuildResponseHandler.handeGuildGetResponse(RequestContext(call, container))
            }

            post("/guild/{id}") {
                PostGuildResponseHandler.handleGuildPostResponse(RequestContext(call, container))
            }

            post("/upgradeGuilds") {
                UpgradeGuildsResponseHandler.handleUpgradeGuildsResponse(RequestContext(call, container))
            }

            get("/member/{guildId}/{userId}") {
                MemberInfoResponseHandler.handleMemberInfoResponse(RequestContext(call, container))
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
                VotedResponseHandler.handleVotedResponse(RequestContext(call, container))
            }

            post("/getsettings/general/{guildId}") {
                GetGeneralSettingsResponseHandler.handleGeneralSettingsGet(RequestContext(call, container))
            }

            post("/postsettings/general/{guildId}") {
                PostGeneralSettingsResponseHandler.handleGeneralSettingsPostResponse(RequestContext(call, container))
            }

            get("/shutdown") {
                val context = RequestContext(call, container)
                val players = container.lavaManager.musicPlayerManager.getPlayers()
                val wrapper = container.daoManager.tracksWrapper

                if (call.request.header("Authorization") != context.restToken) {
                    call.respondText(status = HttpStatusCode.Forbidden) { "bruh" }
                    return@get
                }

                container.shuttingDown = true

                for ((guildId, player) in HashMap(players)) {
                    val guild = MelijnBot.shardManager.getGuildById(guildId) ?: continue
                    val channel = context.lavaManager.getConnectedChannel(guild) ?: continue
                    val trackManager = player.guildTrackManager
                    val pTrack = trackManager.playingTrack ?: continue

                    pTrack.position = trackManager.iPlayer.trackPosition

                    wrapper.put(guildId, container.settings.botInfo.id, pTrack, trackManager.tracks)
                    wrapper.addChannel(guildId, channel.idLong)

                    trackManager.stopAndDestroy()
                }

                call.respondText { "Shutted down!" }
                stop()
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

class RequestContext(val call: ApplicationCall, val container: Container) {
    val daoManager = container.daoManager
    val lavaManager = container.lavaManager
    val restToken = container.settings.restServer.token
}