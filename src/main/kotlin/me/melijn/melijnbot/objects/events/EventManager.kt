package me.melijn.melijnbot.objects.events

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.command.CommandClientBuilder
import me.melijn.melijnbot.objects.events.eventlisteners.*
import me.melijn.melijnbot.objects.utils.sendInGuild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GenericGuildEvent
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent
import net.dv8tion.jda.api.events.message.priv.GenericPrivateMessageEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.hooks.IEventManager
import java.util.*
import kotlin.collections.ArrayList

class EventManager(val container: Container) : IEventManager {

    private val eventListeners: ArrayList<EventListener> = ArrayList()

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
//        val roleAddedListener = RoleAddedListener(container)
//        val roleRemovedListener = RoleRemovedListener(container)
        val boostListener = BoostListener(container)
        val lavaEventListener = container.jdaLavaLink
        val premiumEventListener = container.premiumJdaLavaLink
        val commandListener = CommandClientBuilder(container)
            .loadCommands()
            .build()


        eventListeners.add(commandListener)
        eventListeners.add(botJoinLeaveListener)
        eventListeners.add(botStartShutdownListener)
        eventListeners.add(messageDeletedListener)
        eventListeners.add(messageReceivedListener)
        eventListeners.add(messageUpdateListener)
        eventListeners.add(messageReactionAddedListener)
        eventListeners.add(messageReactionRemovedListener)
        eventListeners.add(joinLeaveListener)
        eventListeners.add(voiceJoinListener)
        eventListeners.add(voiceLeaveListener)
        eventListeners.add(voiceMoveListener)
        eventListeners.add(boostListener)
        eventListeners.add(container.eventWaiter)
        // eventListeners.add(roleAddedListener)
        // eventListeners.add(roleRemovedListener)

        premiumEventListener?.let {
            eventListeners.add(it)
        }

        lavaEventListener?.let {
            eventListeners.add(it)
        }
    }

    override fun handle(event: GenericEvent) {
        if (container.shuttingDown) return
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