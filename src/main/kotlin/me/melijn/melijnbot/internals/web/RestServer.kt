package me.melijn.melijnbot.internals.web

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.web.rest.commands.FullCommandsResponseHandler
import me.melijn.melijnbot.internals.web.rest.convert.UpgradeGuildsResponseHandler
import me.melijn.melijnbot.internals.web.rest.info.GetGuildResponseHandler
import me.melijn.melijnbot.internals.web.rest.info.PostGuildResponseHandler
import me.melijn.melijnbot.internals.web.rest.member.MemberInfoResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.GetGeneralSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.GetUserSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.PostGeneralSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.PostUserSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.shutdown.ShutdownResponseHandler
import me.melijn.melijnbot.internals.web.rest.stats.StatsResponseHandler
import me.melijn.melijnbot.internals.web.rest.voted.VotedResponseHandler
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import java.util.*
import java.util.concurrent.TimeUnit


class RestServer(container: Container) {


    private val jsonType = ContentType.parse("Application/JSON")


    private val server = embeddedServer(CIO, container.settings.restServer.port) {
        routing {
            get("/guildCount") {
                call.respondText {
                    "${MelijnBot.shardManager.guildCache.size()}"
                }
            }


            get("/stats") {
                StatsResponseHandler.handleStatsResponse(RequestContext(call, container))
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
                FullCommandsResponseHandler.handleFullCommandsResponse(RequestContext(call, container))
            }

            get("/timezones") {
                call.respondText(DataArray.fromCollection(TimeZone.getAvailableIDs().toList()).toString(), jsonType)
            }

            post("/voted") {
                VotedResponseHandler.handleVotedResponse(RequestContext(call, container))
            }

            post("/getsettings/user/{userId}") {
                GetUserSettingsResponseHandler.handleUserSettingsGet(RequestContext(call, container))
            }

            post("/postsettings/user/{userId}") {
                PostUserSettingsResponseHandler.handleUserSettingsPost(RequestContext(call, container))
            }

            post("/getsettings/general/{guildId}") {
                GetGeneralSettingsResponseHandler.handleGeneralSettingsGet(RequestContext(call, container))
            }

            post("/postsettings/general/{guildId}") {
                PostGeneralSettingsResponseHandler.handleGeneralSettingsPost(RequestContext(call, container))
            }

            get("/shutdown") {
                ShutdownResponseHandler.handleShutdownResponse(RequestContext(call, container))
                stop()
            }

            //Has to be registered last to not override other paths
            get("*") {
                call.respondText("blub")
            }
        }
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