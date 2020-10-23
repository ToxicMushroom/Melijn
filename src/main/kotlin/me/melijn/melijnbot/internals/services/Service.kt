package me.melijn.melijnbot.internals.services

import me.melijn.melijnbot.internals.threading.RunnableTask
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

abstract class Service(
    val name: String,
    private val period: Long,
    private val initialDelay: Long = 0,
    private val unit: TimeUnit = TimeUnit.SECONDS
) {

    companion object {
        private val serviceThreadFactory = { name: String ->
            { r: Runnable ->
                Thread(r, "[$name-Service]")
            }
        }
    }

    private val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(serviceThreadFactory(name))
    private lateinit var future: ScheduledFuture<*>
    val logger: Logger = LoggerFactory.getLogger(name)

    abstract val service: RunnableTask

    open fun start() {
        future = scheduledExecutor.scheduleAtFixedRate(service, initialDelay, period, unit)
        logger.info("Started $name-Service")
    }

    open fun stop() {
        future.cancel(false)
        logger.info("Stopped $name-Service")
    }
}

