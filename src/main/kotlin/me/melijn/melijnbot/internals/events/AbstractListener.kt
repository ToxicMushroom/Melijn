package me.melijn.melijnbot.internals.events

import me.melijn.melijnbot.Container
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class AbstractListener(protected val container: Container) : SuspendListener() {

    val logger: Logger = LoggerFactory.getLogger(AbstractListener::class.java.name)

}