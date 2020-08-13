package me.melijn.melijnbot.internals.events

import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.hooks.EventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractListener(protected val container: Container) : EventListener {

    val logger: Logger = LoggerFactory.getLogger(AbstractListener::class.java.name)

}