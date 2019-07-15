package me.melijn.melijnbot

import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder

class MelijnBot {

    private var instance: MelijnBot

    init {
        val container: Container = Container()
        instance = this
        container.settings

        val shardManager = DefaultShardManagerBuilder()
                .setShardsTotal(container.settings.shardCount)
                .setToken(container.settings.tokens.melijn)
                .setActivity(Activity.listening("commands of users"))
                .setAutoReconnect(true)
                .addEventListeners()
                .build()

    }

    fun getInstance(): MelijnBot? {
        return instance
    }
}

fun main(args: Array<String>) {
    MelijnBot()
}