package me.melijn.melijnbot

import me.melijn.melijnbot.objects.events.EventManager
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.*


class MelijnBot {

    companion object {
        lateinit var instance: MelijnBot
        var shardManager: ShardManager? = null
    }

    init {
        instance = this
        Locale.setDefault(Locale.ENGLISH)
        val container = Container()

        val eventManager = EventManager(container)
        shardManager = DefaultShardManagerBuilder()
            .setShardsTotal(container.settings.shardCount)
            .setToken(container.settings.tokens.melijn)
            .setActivity(Activity.listening("commands | ${container.settings.prefix}help"))
            .setAutoReconnect(true)
            .setEventManagerProvider { eventManager }
            .build()
    }
}

fun main(args: Array<String>) {
    MelijnBot()
}