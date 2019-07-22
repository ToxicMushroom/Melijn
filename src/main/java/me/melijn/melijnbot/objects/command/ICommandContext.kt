package me.melijn.melijnbot.objects.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager


interface ICommandContext {
    /**
     * Returns the [net.dv8tion.jda.api.entities.Guild] for the current command/event
     *
     * @return the [net.dv8tion.jda.api.entities.Guild] for this command/event
     */
    fun getGuild(): Guild

    /**
     * Returns the [message event][net.dv8tion.jda.api.events.message.MessageReceivedEvent] that was received for this instance
     *
     * @return the [message event][net.dv8tion.jda.api.events.message.MessageReceivedEvent] that was received for this instance
     */
    fun getEvent(): MessageReceivedEvent

    /**
     * Returns the [channel][net.dv8tion.jda.api.entities.PrivateChannel] that the message for this event was send in
     *
     * @return the [channel][net.dv8tion.jda.api.entities.PrivateChannel] that the message for this event was send in
     */
    fun getPrivateChannel(): PrivateChannel {
        return this.getEvent().privateChannel
    }

    /**
     * Returns the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     *
     * @return the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     */
    fun getMessage(): Message {
        return this.getEvent().message
    }

    /**
     * Returns the [author][net.dv8tion.jda.api.entities.User] of the message as user
     *
     * @return the [author][net.dv8tion.jda.api.entities.User] of the message as user
     */
    fun getAuthor(): User {
        return this.getEvent().author
    }

    /**
     * Returns the [author][net.dv8tion.jda.api.entities.Member] of the message as member
     *
     * @return the [author][net.dv8tion.jda.api.entities.Member] of the message as member
     */
    fun getMember(): Member? {
        return this.getEvent().member
    }

    /**
     * Returns the current [jda][net.dv8tion.jda.api.JDA] instance
     *
     * @return the current [jda][net.dv8tion.jda.api.JDA] instance
     */
    fun getJDA(): JDA {
        return this.getEvent().jda
    }

    /**
     * Returns the current [net.dv8tion.jda.api.sharding.ShardManager] instance
     *
     * @return the current [net.dv8tion.jda.api.sharding.ShardManager] instance
     */
    fun getShardManager(): ShardManager? {
        return this.getJDA().shardManager
    }

    /**
     * Returns the [user][net.dv8tion.jda.api.entities.User] for the currently logged in account
     *
     * @return the [user][net.dv8tion.jda.api.entities.User] for the currently logged in account
     */
    fun getSelfUser(): User {
        return this.getJDA().selfUser
    }

    /**
     * Returns the [member][net.dv8tion.jda.api.entities.Member] in the guild for the currently logged in account
     *
     * @return the [member][net.dv8tion.jda.api.entities.Member] in the guild for the currently logged in account
     */
    fun getSelfMember(): Member {
        return this.getGuild().selfMember
    }

    /**
     * Returns the [Boolean] for where the message was sent
     *
     * @return the [Boolean] for where the message was sent
     */
    fun isFromGuild(): Boolean {
        return this.getEvent().isFromType(ChannelType.TEXT)
    }

    /**
     * Returns the [textChannel][TextChannel] that the message for this event was
     * send in
     *
     * @return the [textChannel][TextChannel] that the message for this event was send in
     */
    fun getTextChannel(): TextChannel {
        return this.getEvent().textChannel
    }

    /**
     * Returns the [messageChannel][MessageChannel] that the message for this event was send in
     *
     * @return the [messageChannel][MessageChannel] that the message for this event was send in
     */
    fun getMessageChannel(): MessageChannel {
        return this.getEvent().channel
    }
}