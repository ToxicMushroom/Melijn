package me.melijn.melijnbot.objects.services

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.melijn.melijnbot.objects.utils.toUpperWordCase
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

abstract class Service(name: String) {
    private val threadFactory = ThreadFactoryBuilder().setNameFormat("[${name.toUpperWordCase()}-Service] ").build()
    val scheduledExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(threadFactory)
    val logger = LoggerFactory.getLogger(name)

    abstract fun start()
    abstract fun stop()
}