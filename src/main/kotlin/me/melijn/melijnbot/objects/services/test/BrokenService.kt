package me.melijn.melijnbot.objects.services.test

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.threading.Task
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BrokenService(val container: Container, val shardManager: ShardManager) : Service("broken") {


    private var scheduledFuture: ScheduledFuture<*>? = null

    private val statService = Task(Runnable {
        runBlocking {
            println("ran")
            throw IllegalArgumentException("break")
            println("ran after ex")
        }
    })

    override fun start() {
        logger.info("Started BrokenService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(statService, 1, 1, TimeUnit.SECONDS)
    }

    override fun stop() {
        logger.info("Stopping BrokenService")
        scheduledFuture?.cancel(false)
    }
}