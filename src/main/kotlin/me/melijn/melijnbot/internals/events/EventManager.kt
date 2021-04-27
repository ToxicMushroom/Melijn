package me.melijn.melijnbot.internals.events

import bot.pylon.proto.discord.v1.event.EventEnvelope
import lol.up.pylon.gateway.client.entity.event.Event
import lol.up.pylon.gateway.client.event.AbstractEventReceiver
import lol.up.pylon.gateway.client.event.EventDispatcher
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.command.CommandClientBuilder
import me.melijn.melijnbot.internals.events.eventlisteners.*
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.message.sendInGuild
import java.util.*

class EventManager(val container: Container) : EventDispatcher {

    private val eventListeners: ArrayList<SuspendListener> = ArrayList()

    fun start() {
        registerEvents()
    }

    private fun registerEvents() {
        val botJoinLeaveListener = BotJoinLeaveListener(container)
        val botStartShutdownListener = BotStartShutdownListener(container)
        val messageDeletedListener = MessageDeletedListener(container)
        val messageReceivedListener = MessageReceivedListener(container)
        val messageUpdateListener = MessageUpdateListener(container)
        val messageReactionAddedListener = MessageReactionAddedListener(container)
        val messageReactionRemovedListener = MessageReactionRemovedListener(container)
        val voiceJoinListener = VoiceJoinListener(container)
        val voiceLeaveListener = VoiceLeaveListener(container)
        val voiceMoveListener = VoiceMoveListener(container)
        val joinLeaveListener = JoinLeaveListener(container)
        val boostListener = BoostListener(container)
        val lavaEventListener = container.jdaLavaLink
        val commandListener = CommandClientBuilder(container)
            .loadCommands()
            .build()

        // ORDER WILL AFFECT ORDER IN WICH EVENTS ARE CALLED
        eventListeners.add(container.eventWaiter)
        eventListeners.add(commandListener)
        eventListeners.add(messageReceivedListener)
        eventListeners.add(messageDeletedListener)
        eventListeners.add(messageReactionAddedListener)
        eventListeners.add(messageReactionRemovedListener)
        eventListeners.add(botJoinLeaveListener)
        eventListeners.add(botStartShutdownListener)
        eventListeners.add(messageUpdateListener)
        eventListeners.add(joinLeaveListener)
        eventListeners.add(voiceJoinListener)
        eventListeners.add(voiceLeaveListener)
        eventListeners.add(voiceMoveListener)
        eventListeners.add(boostListener)

        // eventListeners.add(roleAddedListener)
        // eventListeners.add(roleRemovedListener)

        lavaEventListener?.let {
            eventListeners.add(
                Lavalistener(it)
            )
        }
    }

    companion object {
        val eventCountMap = mutableMapOf<String, Long>()
    }

    fun getRegisteredListeners(): MutableList<Any> {
        return Collections.singletonList(eventListeners)
    }

    fun register(listener: Any) {
        throw IllegalArgumentException()
    }

    fun unregister(listener: Any) {
        throw IllegalArgumentException()
    }

    override fun <E : Event<E>?> registerReceiver(eventClass: Class<E>?, receiver: AbstractEventReceiver<E>?) {
        TODO("Not yet implemented")
    }

    override fun dispatchEvent(headerData: EventEnvelope.HeaderData?, event: Event<out Event<Event<*>>>?) {
        if (container.shuttingDown) return
        TaskManager.async {
            try {
                for (eventListener in eventListeners) {
                    eventListener.onEvent(event)
                }
            } catch (e: Exception) {
                when (event) {
                    is MessageEvent -> e.sendInGuild(event.guild, event.channel)
                    is GenericPrivateMessageEvent -> e.sendInGuild(channel = event.channel)
                    is GenericGuildEvent -> e.sendInGuild(event.guild)
                    else -> e.sendInGuild()
                }
            }
        }
    }
}