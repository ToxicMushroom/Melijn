package me.melijn.melijnbot.internals.events.eventlisteners

import me.melijn.melijnbot.internals.threading.TaskManager
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener


class EventWaiter : EventListener {

    private val eventHandlers = mutableMapOf<Class<*>, Set<EventHandler<GenericEvent>>>()

    fun <T : Event> waitFor(
        eventType: Class<T>, condition: suspend (T) -> Boolean,
        action: suspend (T) -> Unit, expired: (() -> Unit)? = null, seconds: Long = 120
    ) {
        val newHandler = EventHandler(condition, action)
        val set = eventHandlers.getOrDefault(eventType, emptySet()).toMutableSet()

        tryCast<EventHandler<GenericEvent>>(newHandler) {
            set.add(this)
        }

        TaskManager.asyncAfter(seconds * 1000) {
            tryCast<EventHandler<GenericEvent>>(newHandler) {
                val newSet = eventHandlers.getOrDefault(eventType, emptySet()).toMutableSet()
                if (newSet.remove(this)) {
                    expired?.invoke()
                }
                eventHandlers[eventType] = newSet
            }
        }

        eventHandlers[eventType] = set
    }

    private inline fun <reified T> tryCast(instance: Any?, block: T.() -> Unit) {
        if (instance is T) {
            block(instance)
        }
    }


    override fun onEvent(event: GenericEvent) {
        val found = eventHandlers[event::class.java] ?: return
        TaskManager.async {
            for (handler in found) {
                if (handler.tryRun(event)) {
                    val set = eventHandlers.getOrDefault(event::class.java, emptySet()).toMutableSet()

                    tryCast<EventHandler<GenericEvent>>(handler) {
                        set.remove(this)
                    }

                    eventHandlers[event::class.java] = set
                }
            }
        }
    }

    private class EventHandler<T : GenericEvent>(
        val condition: suspend (T) -> Boolean,
        val action: suspend (T) -> Unit
    ) {

        suspend fun tryRun(event: T): Boolean {
            if (condition(event)) {
                action(event)
                return true
            }
            return false
        }
    }
}