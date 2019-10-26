package me.melijn.melijnbot

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory
import me.melijn.melijnbot.objects.events.EventManager
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
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
        val eventManager = EventManager(container)
        val defaultShardManagerBuilder = DefaultShardManagerBuilder()
            .setShardsTotal(container.settings.shardCount)
            .setToken(container.settings.tokens.melijn)
            .setActivity(Activity.listening("commands | ${container.settings.prefix}help"))
            .setAutoReconnect(true)
            .setEventManagerProvider { eventManager }

        if (!container.settings.lavalink.enabled) {
            defaultShardManagerBuilder.setAudioSendFactory(NativeAudioSendFactory())
        }

        shardManager = defaultShardManagerBuilder.build()

        container.start(shardManager)
        eventManager.start()
    }
}

fun main(args: Array<String>) {
    MelijnBot()
}