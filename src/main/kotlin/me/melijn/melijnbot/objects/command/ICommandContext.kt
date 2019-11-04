package me.melijn.melijnbot.objects.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager


interface ICommandContext {

    /**
     * Returns the [net.dv8tion.jda.api.entities.Guild] for the current command/event
     *
     * @return the [net.dv8tion.jda.api.entities.Guild] for this command/event or throws an [IllegalArgumentException] when not executed in a guild
     */
    val guild: Guild


    /**
     * Returns the [guildId][Long] for the guild
     *
     * @return the [guildId][Long] for this guild or throws an [IllegalArgumentException] when not executed in a guild
     */
    val guildId: Long
        get() = guild.idLong


    /**
     * Returns the [message event][net.dv8tion.jda.api.events.message.MessageReceivedEvent] that was received for this instance
     *
     * @return the [message event][net.dv8tion.jda.api.events.message.MessageReceivedEvent] that was received for this instance
     */
    val event: MessageReceivedEvent


    /**
     * Returns the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     *
     * @return the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     */
    val message: Message
        get() = this.event.message

    /**
     * Returns the [messageId][Long] of the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     *
     * @return the [messageId][Long] of the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     */
    val messageId: Long
        get() = this.event.messageIdLong


    /**
     * Returns the [author][net.dv8tion.jda.api.entities.User] of the message as user
     *
     * @return the [author][net.dv8tion.jda.api.entities.User] of the message as user
     */
    val author: User
        get() = this.event.author

    /**
     * Returns the [authorId][Long] of the user that authored the message
     *
     * @return the [authorId][Long] of the user that authored the message
     */
    val authorId: Long
        get() = author.idLong


    /**
     * Returns the [author][net.dv8tion.jda.api.entities.Member] of the message as member
     *
     * @return the [author][net.dv8tion.jda.api.entities.Member] of the message as member
     */
    val member: Member
        get() = this.event.member ?: throw IllegalArgumentException("Event is not from a guild")


    /**
     * Returns the current [jda][net.dv8tion.jda.api.JDA] instance
     *
     * @return the current [jda][net.dv8tion.jda.api.JDA] instance
     */
    val jda: JDA
        get() = this.event.jda


    /**
     * Returns the current [net.dv8tion.jda.api.sharding.ShardManager] instance
     *
     * @return the current [net.dv8tion.jda.api.sharding.ShardManager] instance
     */
    val shardManager: ShardManager
        get() = this.jda.shardManager ?: throw IllegalArgumentException("Sharding is disabled!")


    /**
     * Returns the [user][net.dv8tion.jda.api.entities.User] for the currently logged in account
     *
     * @return the [user][net.dv8tion.jda.api.entities.User] for the currently logged in account
     */
    val selfUser: User
        get() = this.jda.selfUser

    /**
     * Returns the [userId][Long] for the currently logged in account
     *
     * @return the [userId][Long] for the currently logged in account
     */
    val selfUserId: Long
        get() = this.jda.selfUser.idLong

    /**
     * Returns the [member][net.dv8tion.jda.api.entities.Member] in the guild for the currently logged in account
     *
     * @return the [member][net.dv8tion.jda.api.entities.Member] in the guild for the currently logged in account or throws an [IllegalArgumentException]
     */
    val selfMember: Member
        get() = this.guild.selfMember


    /**
     * Returns the [Boolean] for where the message was sent
     *
     * @return the [Boolean] for where the message was sent
     */
    val isFromGuild: Boolean
        get() = this.event.isFromGuild


    /**
     * Returns the [textChannel][TextChannel] that the message for this event was
     * send in
     *
     * @return the [textChannel][TextChannel] that the message for this event was send in
     */
    val textChannel: TextChannel
        get() = this.event.textChannel

    /**
     * Returns the [channel][net.dv8tion.jda.api.entities.PrivateChannel] that the message for this event was send in
     *
     * @return the [channel][net.dv8tion.jda.api.entities.PrivateChannel] that the message for this event was send in
     */
    val privateChannel: PrivateChannel
        get() = this.event.privateChannel

    /**
     * Returns the [messageChannel][MessageChannel] that the message for this event was send in
     *
     * @return the [messageChannel][MessageChannel] that the message for this event was send in
     */
    val messageChannel: MessageChannel
        get() = this.event.channel

    /**
     * Returns the [channelId][Long] of the [messageChannel][MessageChannel] that the message for this event was send in
     *
     * @return the [channelId][Long] of the [messageChannel][MessageChannel] that the message for this event was send in
     */
    val channelId: Long
        get() = this.event.channel.idLong
}