package me.melijn.melijnbot.internals.command

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.music.AudioLoader
import me.melijn.melijnbot.internals.music.GuildMusicPlayer
import me.melijn.melijnbot.internals.music.LavaManager
import me.melijn.melijnbot.internals.music.MusicPlayerManager
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.sharding.ShardManager
import java.time.ZoneId


interface ICommandContext {

    /**
     * Returns the [net.dv8tion.jda.api.entities.Guild] for the current command/event
     *
     * @return the [net.dv8tion.jda.api.entities.Guild] for this command/event or throws an [IllegalArgumentException] when not executed in a guild
     */
    val guild: Guild

    /**
     * Returns the [net.dv8tion.jda.api.entities.Guild] for the current command/event
     *
     * @return the [net.dv8tion.jda.api.entities.Guild] for this command/event or throws an [IllegalArgumentException] when not executed in a guild
     */
    val guildN: Guild?


    /**
     * Returns the [guildId][Long] for the guild
     *
     * @return the [guildId][Long] for this guild or throws an [IllegalArgumentException] when not executed in a guild
     */
    val guildId: Long
        get() = guild.idLong


    /**
     * Returns the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     *
     * @return the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     */
    val message: Message

    /**
     * Returns the [messageId][Long] of the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     *
     * @return the [messageId][Long] of the [message][net.dv8tion.jda.api.entities.Message] that triggered this event
     */
    val messageId: Long
        get() = message.idLong


    /**
     * Returns the [author][net.dv8tion.jda.api.entities.User] of the message as user
     *
     * @return the [author][net.dv8tion.jda.api.entities.User] of the message as user
     */
    val author: User

    /**
     * Returns the [authorId][Long] of the user that authored the message
     *
     * @return the [authorId][Long] of the user that authored the message
     */
    val authorId: Long
        get() = author.idLong


    /**
     * @return the [author][net.dv8tion.jda.api.entities.Member] of the message as member
     */
    val member: Member


    /**
     * @return the current [jda][net.dv8tion.jda.api.JDA] instance
     */
    val jda: JDA


    /**
     * @return the current [net.dv8tion.jda.api.sharding.ShardManager] instance
     */
    val shardManager: ShardManager
        get() = this.jda.shardManager ?: throw IllegalArgumentException("Sharding is disabled!")


    /**
     * @return the [user][net.dv8tion.jda.api.entities.User] for the currently logged in account
     */
    val selfUser: User
        get() = this.jda.selfUser

    /**
     * @return the [userId][Long] for the currently logged in account
     */
    val selfUserId: Long
        get() = this.jda.selfUser.idLong

    /**
     * @return the [member][net.dv8tion.jda.api.entities.Member] in the guild for the currently logged in account or throws an [IllegalArgumentException]
     */
    val selfMember: Member
        get() = this.guild.selfMember


    /**
     * @return the [Boolean] for where the message was sent
     */
    val isFromGuild: Boolean
        get() = this.guildN != null


    /**
     * @return the [textChannel][TextChannel] that the message for this event was send in
     */
    val textChannel: TextChannel

    /**
     * @return the [channel][net.dv8tion.jda.api.entities.PrivateChannel] that the message for this event was send in
     */
    val privateChannel: PrivateChannel

    /**
     * @return the [messageChannel][MessageChannel] that the message for this event was send in
     */
    val channel: MessageChannel

    /**
     * @return the [channelId][Long] of the [messageChannel][MessageChannel] that the message for this event was send in
     */
    val channelId: Long

    /**
     * @return the [webManager][WebManager] container instance of the bot
     */
    val webManager: WebManager

    /**
     * @return the [usedPrefix][String] raw prefix that is used or if it's a mention, the displayed content
     */
    val usedPrefix: String

    val commandParts: List<String>
    val container: Container
    val commandList: Set<AbstractCommand>
    val partSpaceMap: MutableMap<String, Int>
    val aliasMap: MutableMap<String, List<String>>
    var searchedAliases: Boolean

    val prefix: String
    var commandOrder: List<AbstractCommand>
    var args: List<String>
    var oldArgs: List<String>
    val daoManager: DaoManager
    var rawArg: String
    val contextTime: Long
    val lavaManager: LavaManager
    val musicPlayerManager: MusicPlayerManager
    val audioLoader: AudioLoader
    var fullArg: String
    val botDevIds: LongArray

    var calculatedRoot: String
    var calculatedCommandPartsOffset: Int

    fun reply(something: Any)
    fun reply(embed: MessageEmbed)
    suspend fun getLanguage(): String
    fun getRawArgPart(beginIndex: Int, endIndex: Int = -1): String
    suspend fun getTranslation(path: String): String
    suspend fun getTimeZoneId(): ZoneId
    fun getGuildMusicPlayer(): GuildMusicPlayer
    fun initCooldown()
    fun initArgs()
}