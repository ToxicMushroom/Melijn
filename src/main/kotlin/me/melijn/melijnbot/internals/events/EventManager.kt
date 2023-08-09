package me.melijn.melijnbot.internals.events

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.command.CommandClientBuilder
import me.melijn.melijnbot.internals.events.eventlisteners.*
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.utils.message.sendInGuild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.RawGatewayEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.api.events.message.priv.GenericPrivateMessageEvent
import net.dv8tion.jda.api.hooks.IEventManager
import java.util.*

class EventManager(val container: Container) : IEventManager {

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
        val bannedEventListener = BannedEventListener(container)
        val joinLeaveListener = JoinLeaveListener(container)
//        val roleAddedListener = RoleAddedListener(container)
//        val roleRemovedListener = RoleRemovedListener(container)
        val boostListener = BoostListener(container)
        val commandListener = CommandClientBuilder(container)
            .loadCommands()
            .build()

        // ORDER WILL AFFECT ORDER IN WHICH EVENTS ARE CALLED
        eventListeners.add(container.eventWaiter)
        eventListeners.add(commandListener)
        eventListeners.add(messageReceivedListener)
        eventListeners.add(messageDeletedListener)
        eventListeners.add(messageReactionAddedListener)
        eventListeners.add(messageReactionRemovedListener)
        eventListeners.add(botJoinLeaveListener)
        eventListeners.add(botStartShutdownListener)
        eventListeners.add(messageUpdateListener)
        eventListeners.add(bannedEventListener)
        eventListeners.add(joinLeaveListener)
        eventListeners.add(voiceJoinListener)
        eventListeners.add(voiceLeaveListener)
        eventListeners.add(voiceMoveListener)
        eventListeners.add(boostListener)

        // eventListeners.add(roleAddedListener)
        // eventListeners.add(roleRemovedListener)
    }

    companion object {
        val eventCountMap = mutableMapOf<String, Long>()
    }

    override fun handle(event: GenericEvent) {
        if (container.shuttingDown || container.ratelimiting) return
        if (event is RawGatewayEvent) {
            eventCountMap[event.type] = eventCountMap.getOrDefault(event.type, 0) + 1
            return
        }
        TaskManager.async {
            try {
                for (eventListener in eventListeners) {
                    eventListener.onEvent(event)
                }
            } catch (e: Exception) {
                when (event) {
                    is GenericGuildMessageEvent -> e.sendInGuild(event.guild, event.channel)
                    is GenericPrivateMessageEvent -> e.sendInGuild(channel = event.channel)
                    is GenericGuildEvent -> e.sendInGuild(event.guild)
                    else -> e.sendInGuild()
                }
            }
        }
    }

    override fun getRegisteredListeners(): MutableList<Any> {
        return Collections.singletonList(eventListeners)
    }

    override fun register(listener: Any) {
        throw IllegalArgumentException()
    }

    override fun unregister(listener: Any) {
        throw IllegalArgumentException()
    }
}