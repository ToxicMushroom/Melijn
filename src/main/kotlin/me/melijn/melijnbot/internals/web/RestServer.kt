package me.melijn.melijnbot.internals.web

import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.models.EmbedEditor
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.message.sendPrivateMessageExtra
import me.melijn.melijnbot.internals.web.rest.codes.VerificationCodeResponseHandler
import me.melijn.melijnbot.internals.web.rest.commands.CommandMapResponseHandler
import me.melijn.melijnbot.internals.web.rest.commands.FullCommandsResponseHandler
import me.melijn.melijnbot.internals.web.rest.convert.UpgradeGuildsResponseHandler
import me.melijn.melijnbot.internals.web.rest.info.GetGuildResponseHandler
import me.melijn.melijnbot.internals.web.rest.info.PostGuildResponseHandler
import me.melijn.melijnbot.internals.web.rest.member.MemberInfoResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.GetUserSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.PostUserSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.general.GetGeneralSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.general.PostGeneralSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.logging.GetLoggingSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.logging.PostLoggingSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.starboard.GetStarboardSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.settings.starboard.PostStarboardSettingsResponseHandler
import me.melijn.melijnbot.internals.web.rest.shutdown.ShutdownResponseHandler
import me.melijn.melijnbot.internals.web.rest.stats.EventStatsResponseHandler
import me.melijn.melijnbot.internals.web.rest.stats.PublicStatsResponseHandler
import me.melijn.melijnbot.internals.web.rest.stats.StatsResponseHandler
import me.melijn.melijnbot.internals.web.rest.voted.VotedResponseHandler
import me.melijn.melijnbot.objectMapper
import net.dv8tion.jda.api.utils.data.DataArray
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.entities.UserImpl
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.TimeUnit


class RestServer(container: Container) {

    private val jsonType = ContentType.parse("Application/JSON")

    private val logger = LoggerFactory.getLogger(RestServer::class.java)

    private val server: NettyApplicationEngine = embeddedServer(Netty, container.settings.restServer.port) {
        install(ContentNegotiation) {
            jackson()
        }

        routing {
            get("/podinfo") {
                call.respondText(ContentType.Application.Json) {
                    objectMapper.createObjectNode()
                        .put("podId", PodInfo.podId)
                        .put("podCount", PodInfo.podCount)
                        .put("shardCount", PodInfo.shardCount)
                        .toString()
                }
            }

            post("/dblvote") {
                call.respondText { "pogu" }
                logger.info("Go dblvote vote:\n" + call.receiveText())
            }

            get("/guildCount") {
                call.respondText {
                    "${MelijnBot.shardManager.guildCache.size()}"
                }
            }

            get("/events") {
                try {
                    EventStatsResponseHandler.handleEventStatsResponse(RequestContext(call, container))
                } catch (t: Throwable) {
                    t.printStackTrace()
                    call.respondText { t.message + "\n" + t.stackTraceToString() }
                }
            }
            get("/stats") {
                try {
                    StatsResponseHandler.handleStatsResponse(RequestContext(call, container))
                } catch (t: Throwable) {
                    t.printStackTrace()
                    call.respondText { t.message + "\n" + t.stackTraceToString() }
                }
            }
            get("/publicStats") {
                try {
                    PublicStatsResponseHandler.handlePublicStatsResponse(RequestContext(call, container))
                } catch (t: Throwable) {
                    t.printStackTrace()
                    call.respondText { t.message + "\n" + t.stackTraceToString() }
                }
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

                call.respondText(
                    DataObject.empty()
                        .put("isSame", path == translation)
                        .put("translation", translation)
                        .toString()
                )
            }

            get("/translations/{language}") {
                val lang = call.parameters["language"] ?: return@get
                val data = i18n.getTranslations(lang)
                call.respondText(data.toString(), jsonType)
            }

            get("/fullCommands") {
                FullCommandsResponseHandler.handleFullCommandsResponse(RequestContext(call, container))
            }

            get("/commandMap") {
                CommandMapResponseHandler.handleCommandMapResponse(RequestContext(call, container))
            }

            get("/timezones") {
                call.respondText(DataArray.fromCollection(TimeZone.getAvailableIDs().toList()).toString(), jsonType)
            }

            post("/voted") {
                VotedResponseHandler.handleVotedResponse(RequestContext(call, container))
            }

            post("/senddm/{userId}/{extra}") {
                val id = call.parameters["userId"] ?: run {
                    call.respond(false)
                    return@post
                }
                val extra = call.parameters["extra"] ?: run {
                    call.respond(false)
                    return@post
                }

                val content = call.receiveText()
                val embedEditor = objectMapper.readValue<EmbedEditor>(content)

                val user = MelijnBot.shardManager.retrieveUserById(id).awaitOrNull() as UserImpl?
                if (user == null) {
                    call.respond(false)
                    return@post
                }

                val result: Boolean = sendPrivateMessageExtra(user, embedEditor, extra)
                call.respond(result)
            }

            post("/unverified/guilds") {
                VerificationCodeResponseHandler.handleUnverifiedGuilds(RequestContext(call, container))
            }

            post("/unverified/verify") {
                VerificationCodeResponseHandler.handleGuildVeriifcation(RequestContext(call, container))
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

            post("/getsettings/logging/{guildId}") {
                GetLoggingSettingsResponseHandler.handleGetLoggingSettings(RequestContext(call, container))
            }

            post("/setsettings/logging/{guildId}") {
                PostLoggingSettingsResponseHandler.handleSetLoggingSettings(RequestContext(call, container))
            }

            post("/getsettings/starboard/{guildId}") {
                GetStarboardSettingsResponseHandler.handleGetStarboardSettings(RequestContext(call, container))
            }

            post("/setsettings/starboard/{guildId}") {
                PostStarboardSettingsResponseHandler.handleSetStarboardSettings(RequestContext(call, container))
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