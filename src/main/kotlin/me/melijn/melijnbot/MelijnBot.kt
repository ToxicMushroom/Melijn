package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import kotlinx.coroutines.runBlocking
import lavalink.client.io.jda.JdaLavalink
import me.melijn.melijnbot.commands.music.MusicNodeCommand
import me.melijn.melijnbot.objects.events.EventManager
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.net.URI
import java.util.*


class MelijnBot {

    companion object {
        lateinit var instance: MelijnBot
        lateinit var shardManager: ShardManager
    }

    init {
        instance = this
        Locale.setDefault(Locale.ENGLISH)

        val container = Container()

        val jdaLavaLink = generateJdaLinkFromNodes(container, container.settings.lavalink.verified_nodes)
        val premiumJdaLavaLink = generateJdaLinkFromNodes(container, container.settings.lavalink.http_nodes)

        container.initLava(jdaLavaLink, premiumJdaLavaLink)

        val eventManager = EventManager(container)

        val defaultShardManagerBuilder = DefaultShardManagerBuilder
            .create(
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_BANS,
                GatewayIntent.GUILD_EMOJIS,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.GUILD_VOICE_STATES
            )
            .setShardsTotal(container.settings.shardCount)
            .setToken(container.settings.tokens.discord)
            .setActivity(Activity.listening("commands | ${container.settings.prefix}help"))
            .setAutoReconnect(true)
            .disableCache(CacheFlag.CLIENT_STATUS, CacheFlag.ACTIVITY)
            .setChunkingFilter(ChunkingFilter.NONE)
            .setEventManagerProvider { eventManager }

        if (!container.settings.lavalink.enabled) {
            defaultShardManagerBuilder.setAudioSendFactory(NativeAudioSendFactory())
        } else if (jdaLavaLink != null) {
            defaultShardManagerBuilder.setVoiceDispatchInterceptor(
                MyVoiceInterceptor(
                    container
                )
            )
        }

        eventManager.start()
        shardManager = defaultShardManagerBuilder.build()
        container.initShardManager(shardManager)
    }

    private fun generateJdaLinkFromNodes(container: Container, httpNodes: Array<Settings.Lavalink.Node>): JdaLavalink? {
        return if (container.settings.lavalink.enabled) {
            val linkBuilder = JdaLavalink(
                container.settings.id.toString(),
                container.settings.shardCount
            ) { id ->
                shardManager.getShardById(id)
            }

            linkBuilder.autoReconnect = true

            for (node in httpNodes) {
                linkBuilder.addNode(URI.create("ws://${node.host}"), node.password)
            }
            linkBuilder
        } else {
            null
        }
    }
}

class MyVoiceInterceptor(val container: Container) : VoiceDispatchInterceptor {

    override fun onVoiceServerUpdate(update: VoiceDispatchInterceptor.VoiceServerUpdate) {
        container.taskManager.async {
            val guildId = update.guildIdLong

            val link = if (MusicNodeCommand.connectionMap[guildId] == false && container.daoManager.musicNodeWrapper.isPremium(guildId)) {
                MusicNodeCommand.connectionMap.remove(guildId)
                container.jdaLavaLink
            } else if (MusicNodeCommand.connectionMap[guildId] == true && !container.daoManager.musicNodeWrapper.isPremium(guildId)) {
                MusicNodeCommand.connectionMap.remove(guildId)
                container.premiumJdaLavaLink
            } else if (container.daoManager.musicNodeWrapper.isPremium(guildId)) {
                container.premiumJdaLavaLink
            } else {
                container.jdaLavaLink
            }

            link?.voiceInterceptor?.onVoiceServerUpdate(update)
        }
    }

    override fun onVoiceStateUpdate(update: VoiceDispatchInterceptor.VoiceStateUpdate): Boolean = runBlocking {
        val guildId = update.guildIdLong

        val link = if (MusicNodeCommand.connectionMap[guildId] == false && container.daoManager.musicNodeWrapper.isPremium(guildId)) {
            MusicNodeCommand.connectionMap.remove(guildId)
            container.jdaLavaLink
        } else if (MusicNodeCommand.connectionMap[guildId] == true && !container.daoManager.musicNodeWrapper.isPremium(guildId)) {
            MusicNodeCommand.connectionMap.remove(guildId)
            container.premiumJdaLavaLink
        } else if (container.daoManager.musicNodeWrapper.isPremium(guildId)) {
            container.premiumJdaLavaLink
        } else {
            container.jdaLavaLink
        }

        link?.voiceInterceptor?.onVoiceStateUpdate(update) ?: false
    }
}

fun main() {
    MelijnBot()
}