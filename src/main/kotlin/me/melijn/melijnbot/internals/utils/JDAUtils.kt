package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.database.ban.BotBannedWrapper.Companion.isBotBanned
import me.melijn.melijnbot.database.locking.EntityType
import me.melijn.melijnbot.database.statesync.LiteEmote
import me.melijn.melijnbot.database.statesync.toLite
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import net.dv8tion.jda.api.utils.concurrent.Task
import java.awt.Color
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


val DISCORD_ID = Regex("\\d{17,20}") // ID
val FULL_USER_REF = Regex("(\\S.{0,30}\\S)\\s*#(\\d{4})") // $1 -> username, $2 -> discriminator
val USER_MENTION = Regex("<@!?(\\d{17,20})>") // $1 -> ID
val CHANNEL_MENTION = Regex("<#(\\d{17,20})>") // $1 -> ID
val ROLE_MENTION = Regex("<@&(\\d{17,20})>") // $1 -> ID
val EMOTE_MENTION = Regex("<a?:(.{2,32}):(\\d{17,20})>") // $1 -> NAME, $2 -> ID

val Member.asTag: String
    get() = this.user.asTag

val TextChannel.asTag: String
    get() = "#${this.name}"

val GuildChannel.asTag: String
    get() {
        return when (this) {
            is TextChannel -> this.asTag
            else -> this.name
        }
    }

suspend fun <T> Task<T>.await(failure: ((Throwable) -> Unit)? = null) = suspendCoroutine<T> {
    onSuccess { success: T ->
        it.resume(success)
    }

    onError { throwable ->
        if (failure == null) {
            it.resumeWithException(throwable)
        } else {
            failure.invoke(throwable)
        }
    }
}

suspend fun <T> Task<T>.awaitOrNull() = suspendCoroutine<T?> { continuation ->
    onSuccess { success ->
        continuation.resume(success)
    }

    onError {
        continuation.resume(null)
    }
}

suspend fun <T> RestAction<T>.await(failure: ((Throwable) -> Unit)? = null) = suspendCoroutine<T> {
    queue(
        { success ->
            it.resume(success)
        }, { failed ->
            if (failure == null) {
                it.resumeWithException(failed)
            } else {
                failure.invoke(failed)
            }
        })
}

suspend fun <T> RestAction<T>.awaitOrNull() = suspendCoroutine<T?> {
    queue(
        { success ->
            it.resume(success)
        },
        { _ ->
            it.resume(null)
        }
    )
}

suspend fun <T> RestAction<T>.awaitEX() = suspendCoroutine<Throwable?> {
    queue(
        { _ -> it.resume(null) },
        { throwable -> it.resume(throwable) }
    )
}

suspend fun <T> RestAction<T>.awaitBool() = suspendCoroutine<Boolean> {
    queue(
        { _ -> it.resume(true) },
        { _ -> it.resume(false) }
    )
}


suspend fun <T> RestAction<T>.async(success: suspend (T) -> Unit, failure: suspend (Throwable) -> Unit) {
    this.queue(
        { t -> TaskManager.async { success(t) } },
        { e ->
            TaskManager.async {
                failure(e)
            }
        }
    )
}

fun <T> RestAction<T>.async(success: suspend (T) -> Unit) {
    this.queue { t -> TaskManager.async { success(t) } }
}


fun getUserByArgsN(context: ICommandContext, index: Int): User? {//With null
    val shardManager = context.shardManager

    return if (context.args.size > index) {
        getUserByArgsN(shardManager, context.guildN, context.args[index], context.message)
    } else {
        null
    }
}

fun getUserByArgsN(shardManager: ShardManager, guild: Guild?, arg: String, message: Message? = null): User? {
    if (arg.isBlank()) return null
    return if (DISCORD_ID.matches(arg)) {
        shardManager.getUserById(arg)
    } else if (USER_MENTION.matches(arg)) {
        val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
        message?.mentionedUsers?.firstOrNull { it.id == id } ?: shardManager.getUserById(id)
    } else if (guild != null && FULL_USER_REF.matches(arg)) {
        getUserByTag(shardManager, arg)
    } else if (guild != null && guild.getMembersByName(arg, true).isNotEmpty()) {
        guild.getMembersByName(arg, true)[0].user
    } else if (guild != null && guild.getMembersByNickname(arg, true).isNotEmpty()) {
        guild.getMembersByNickname(arg, true)[0].user
    } else if (guild != null) {
        val users = guild.jda.users
        val startsWith = users.filter { user -> user.name.startsWith(arg, true) }
        if (startsWith.isNotEmpty()) {
            startsWith[0]
        } else {
            null
        }
    } else null
}

fun getUserByTag(shardManager: ShardManager, arg: String): User? {
    val discriminator = arg.takeLast(4)
    val name = arg.dropLast(5)
    return shardManager.userCache.firstOrNull { user ->
        user.discriminator == discriminator &&
                user.name == name
    }
}

suspend fun retrieveUserByArgsN(context: ICommandContext, index: Int): User? {
    val user1: User? = getUserByArgsN(context, index)
    return when {
        user1 != null -> user1
        context.args.size > index -> {
            val arg = context.args[index]
            if (arg.isBlank()) null
            else when {
                DISCORD_ID.matches(arg) -> {
                    context.shardManager.retrieveUserById(arg).awaitOrNull()
                }
                USER_MENTION.matches(arg) -> {
                    val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
                    context.message.mentionedUsers.firstOrNull { it.id == id }
                        ?: context.shardManager.retrieveUserById(id).awaitOrNull()
                }
                else -> context.guildN?.retrieveMembersByPrefix(arg, 1)?.awaitOrNull()?.run {
                    this.firstOrNull { it.effectiveName.length == arg.length }?.let {
                        return@run it.user
                    }
                    this.minByOrNull { it.effectiveName.length }?.user
                }
            }
        }
        else -> null
    }
}

suspend fun retrieveUserByArgsN(guild: Guild, arg: String): User? {
    if (arg.isBlank()) return null
    val shardManager = guild.jda.shardManager ?: return null
    val user1: User? = getUserByArgsN(shardManager, guild, arg)
    if (user1 != null) {
        return user1
    }

    return when {
        DISCORD_ID.matches(arg) -> shardManager.retrieveUserById(arg)
        USER_MENTION.matches(arg) -> {
            val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
            shardManager.retrieveUserById(id)
        }

        else -> {
            return guild.retrieveMembersByPrefix(arg, 1).awaitOrNull()?.firstOrNull()?.user
        }
    }.awaitOrNull()
}

suspend fun retrieveUserByArgsNMessage(context: ICommandContext, index: Int): User? {
    if (argSizeCheckFailed(context, index)) return null
    val possibleUser = retrieveUserByArgsN(context, index)
    if (possibleUser == null) {
        val msg = context.getTranslation(MESSAGE_UNKNOWN_USER)
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return possibleUser
}


fun getRoleByArgsN(context: ICommandContext, index: Int, sameGuildAsContext: Boolean = true): Role? {
    var role: Role? = null

    if (!context.isFromGuild && sameGuildAsContext) return role
    if (context.args.size <= index) return role

    val arg = context.args[index]

    role = if (DISCORD_ID.matches(arg) && context.jda.shardManager?.getRoleById(arg) != null) {
        context.shardManager.getRoleById(arg)

    } else if (context.isFromGuild && context.guild.getRolesByName(arg, true).size > 0) {
        context.guild.getRolesByName(arg, true)[0]

    } else if (ROLE_MENTION.matches(arg)) {
        val id = (ROLE_MENTION.find(arg) ?: return null).groupValues[1]
        context.message.mentionedRoles.firstOrNull { it.id == id } ?: context.shardManager.getRoleById(id)

    } else {
        if (arg == "everyone") context.guild.publicRole
        else role
    }

    if (sameGuildAsContext && !context.guild.roles.contains(role)) return null
    return role
}

suspend fun getTextChannelsByArgsNMessage(
    context: ICommandContext,
    indexFrom: Int,
    indexTo: Int,
    sameGuildAsContext: Boolean = true
): List<TextChannel>? {
    val roles = mutableListOf<TextChannel>()
    for (i in indexFrom until indexTo) {
        val channel = getTextChannelByArgsNMessage(context, i, sameGuildAsContext) ?: return null
        roles.add(channel)
    }
    return roles
}


suspend fun getRolesByArgsNMessage(
    context: ICommandContext,
    indexFrom: Int,
    indexTo: Int,
    sameGuildAsContext: Boolean = true,
    canInteract: Boolean = false
): List<Role>? {
    val roles = mutableListOf<Role>()
    for (i in indexFrom until indexTo) {
        val role = getRoleByArgsNMessage(context, i, sameGuildAsContext, canInteract) ?: return null
        roles.add(role)
    }
    return roles
}


suspend fun getRoleByArgsNMessage(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true,
    canInteract: Boolean = false
): Role? {
    if (argSizeCheckFailed(context, index)) return null
    val role = getRoleByArgsN(context, index, sameGuildAsContext)
    if (role == null) {
        val msg = context.getTranslation("message.unknown.role")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)

    } else if (canInteract && !context.guild.selfMember.canInteract(role)) {
        val msg = context.getTranslation(MESSAGE_SELFINTERACT_ROLE_HIARCHYEXCEPTION)
            .withSafeVariable(PLACEHOLDER_ROLE, context.args[index])
        sendRsp(context, msg)
        return null
    }
    return role
}

// Returns pair with boolean (true if place is used in args, false if attachments were provided)
suspend fun getImageUrlFromArgsNMessage(
    context: ICommandContext,
    index: Int
): Pair<Boolean, String>? {
    val attachments = context.message.attachments
    when {
        attachments.size > 0 -> {
            return Pair(false, attachments[0].url)
        }
        context.args.isNotEmpty() -> {
            val arg = context.args[index]
            when {
                URL_PATTERN.matches(arg) -> {
                    return Pair(true, arg)
                }
                EMOTE_MENTION.matches(arg) -> {
                    val emoteType = if (arg.startsWith("<a")) "gif" else "png"
                    val emoteId = EMOTE_MENTION.find(arg)?.groupValues?.get(2)
                    return Pair(true, "https://cdn.discordapp.com/emojis/$emoteId.$emoteType?v=1")
                }
                else -> {
                    val msg = "The text you provided `${MarkdownSanitizer.sanitize(arg)}` is not a valid url"
                    sendRsp(context, msg)
                }
            }
        }
        else -> {
            val msg = "No image was provided as an attachment or as a url"
            sendRsp(context, msg)
        }
    }
    return null
}

suspend fun getStringFromArgsNMessage(
    context: ICommandContext,
    index: Int,
    min: Int,
    max: Int,
    mustMatch: Regex? = null,
    cantContainChars: Array<Char> = emptyArray(),
    cantContainWords: Array<String> = emptyArray(),
    ignoreCase: Boolean = false
): String? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    if (arg.length < min) {
        val msg = context.getTranslation("message.string.minfailed")
            .withSafeVariable("arg", arg)
            .withVariable("min", min)
            .withVariable("length", arg.length)
        sendRsp(context, msg)
        return null
    }
    if (arg.length > max) {
        val msg = context.getTranslation("message.string.maxfailed")
            .withSafeVariable("arg", arg)
            .withVariable("max", max)
            .withVariable("length", arg.length)
        sendRsp(context, msg)
        return null
    }
    if (mustMatch != null && !mustMatch.matches(arg)) {
        val msg = context.getTranslation("message.string.matchfailed")
            .withSafeVariable("arg", arg)
            .withVariable("pattern", mustMatch)
        sendRsp(context, msg)
        return null
    }
    for (char in cantContainChars) {
        if (arg.contains(char, ignoreCase)) {
            val msg = context.getTranslation("message.string.cantcontaincharfailed")
                .withSafeVariable("arg", arg)
                .withSafeVariable("chars", cantContainChars.joinToString("") { "$it"})
                .withVariable("char", "$char")
                .withVariable("ignorecase", ignoreCase)
            sendRsp(context, msg)
            return null
        }
    }
    for (word in cantContainWords) {
        if (arg.contains(word, ignoreCase)) {
            val msg = context.getTranslation("message.string.cantcontainwordfailed")
                .withSafeVariable("arg", arg)
                .withVariable("words", cantContainWords)
                .withVariable("word", word)
                .withVariable("ignorecase", ignoreCase)
            sendRsp(context, msg)
            return null
        }
    }

    return arg
}

suspend fun getEmotejiByArgsNMessage(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = false
): Pair<LiteEmote?, String?>? {
    if (argSizeCheckFailed(context, index)) return null
    val emoteji = getEmotejiByArgsN(context, index, sameGuildAsContext)
    if (emoteji == null) {
        val msg = context.getTranslation("message.unknown.emojioremote")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }

    return emoteji
}


suspend fun getEmotejiByArgsN(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = false
): Pair<LiteEmote?, String?>? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val emoji = if (SupportedDiscordEmoji.helpMe.contains(arg)) {
        arg
    } else {
        null
    }

    val emote = if (emoji == null) {
        getEmoteByArgsN(context, index, sameGuildAsContext)?.toLite()
    } else null

    if (emoji == null && emote == null) {
        return null
    }
    return Pair(emote, emoji)
}

suspend fun getEmoteByArgsNMessage(context: ICommandContext, index: Int, sameGuildAsContext: Boolean): Emote? {
    if (argSizeCheckFailed(context, index)) return null
    val emote = getEmoteByArgsN(context, index, sameGuildAsContext)
    if (emote == null) {
        val msg = context.getTranslation("message.unknown.emote")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return emote
}


suspend fun getEmoteByArgsN(context: ICommandContext, index: Int, sameGuildAsContext: Boolean): Emote? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    var emote: Emote? = null

    if (DISCORD_ID.matches(arg)) {
        emote = context.shardManager.getEmoteById(arg)

    } else if (EMOTE_MENTION.matches(arg)) {
        val id = (EMOTE_MENTION.find(arg) ?: return null).groupValues[2]
        emote = context.message.emotes.firstOrNull { it.id == id }
            ?: context.shardManager.getEmoteById(id)

    } else {
        var emotes: List<Emote>? = context.guildN?.getEmotesByName(arg, false)
        if (emotes?.isNotEmpty() == true) emote = emotes[0]

        emotes = context.guildN?.getEmotesByName(arg, true)
        if (emotes != null && emotes.isNotEmpty() && emote == null) emote = emotes[0]

        emotes = context.guildN?.getEmotesByName(arg, false) ?: emptyList()
        if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

        emotes = context.guildN?.getEmotesByName(arg, true) ?: emptyList()
        if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

    }

    return if (sameGuildAsContext && (emote?.guild?.idLong != context.guildId)) {
        null
    } else {
        emote
    }
}

val hexColorRegex = Regex("(?:0x)?#?((?:[a-f]|\\d){6});?", RegexOption.IGNORE_CASE)
val rgbColorRegex = Regex("(?:rgb\\()?\\s*(\\d+),\\s*(\\d+),\\s*(\\d+)\\)?;?", RegexOption.IGNORE_CASE)
val hsbColorRegex = Regex("(?:hsb\\()?(\\d+),\\s*(\\d+)%?,\\s*(\\d+)%?\\)?;?", RegexOption.IGNORE_CASE)

suspend fun getColorFromArgNMessage(context: ICommandContext, index: Int): Color? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val color: Color? = when {
        hexColorRegex.matches(arg) -> {
            hexColorRegex.find(arg)?.groupValues?.get(1)?.let {
                Color.decode("#$it")
            }
        }

        rgbColorRegex.matches(arg) -> {
            rgbColorRegex.find(arg)?.groupValues?.let {
                val r = it[1].toIntOrNull()
                val g = it[2].toIntOrNull()
                val b = it[3].toIntOrNull()
                if (r == null || g == null || b == null) null
                else try {
                    Color(r, g, b)
                } catch (t: Throwable) {
                    null
                }
            }
        }
        hsbColorRegex.matches(arg) -> {
            hsbColorRegex.find(arg)?.groupValues?.let {
                val h = it[1].toIntOrNull()?.div(360.0f)
                val s = it[2].toIntOrNull()?.div(100.0f)
                val l = it[3].toIntOrNull()?.div(100.0f)
                if (h == null || s == null || l == null) null
                else Color.getHSBColor(h, s, l)
            }
        }
        arg.isNumber() -> {
            if (arg.toIntOrNull() == null) null
            else Color(arg.toInt())
        }
        else -> {
            Color.getColor(arg)
        }
    }
    if (color == null) {
        val msg = context.getTranslation("message.unknown.color")
            .withSafeVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    return color
}


fun getChannelByArgsN(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): GuildChannel? {
    var channel: GuildChannel? = null
    if (context.args.size <= index) return null

    if (!context.isFromGuild && sameGuildAsContext) return channel
    val arg = context.args[index]

    channel = if (DISCORD_ID.matches(arg)) {
        if (sameGuildAsContext) {
            context.guild.getCategoryById(arg)
        } else {
            context.shardManager.getGuildChannelById(arg)
        }
    } else if (context.isFromGuild && context.guild.getTextChannelsByName(arg, true).size > 0) {
        context.guild.getTextChannelsByName(arg, true)[0]

    } else if (context.isFromGuild && context.guild.getVoiceChannelsByName(arg, true).size > 0) {
        context.guild.getVoiceChannelsByName(arg, true)[0]

    } else if (context.isFromGuild && context.guild.getStoreChannelsByName(arg, true).size > 0) {
        context.guild.getStoreChannelsByName(arg, true)[0]

    } else if (CHANNEL_MENTION.matches(arg)) {
        val id = (CHANNEL_MENTION.find(arg) ?: return null).groupValues[1]
        context.message.mentionedChannels.firstOrNull { it.id == id }
            ?: context.shardManager.getGuildChannelById(id)

    } else channel

    if (sameGuildAsContext && !context.guild.channels.contains(channel)) return null
    return channel
}

suspend fun getChannelByArgsNMessage(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): GuildChannel? {
    if (argSizeCheckFailed(context, index)) return null
    val channel = getChannelByArgsN(context, index, sameGuildAsContext)
    if (channel == null) {
        val msg = context.getTranslation(MESSAGE_UNKNOWN_CHANNEL)
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return channel
}


fun getCategoryByArgsN(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): Category? {
    var category: Category? = null
    if (context.args.size <= index) return null

    if (!context.isFromGuild && sameGuildAsContext) return category
    val arg = context.args[index]

    category = if (DISCORD_ID.matches(arg)) {
        if (sameGuildAsContext) {
            context.guild.getCategoryById(arg)
        } else {
            context.shardManager.getCategoryById(arg)
        }
    } else if (context.isFromGuild && context.guild.getCategoriesByName(arg, true).size > 0) {
        context.guild.getCategoriesByName(arg, true)[0]

    } else category

    if (sameGuildAsContext && !context.guild.categories.contains(category)) return null
    return category
}

suspend fun getCategoryByArgsNMessage(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): Category? {
    if (argSizeCheckFailed(context, index)) return null
    val category = getCategoryByArgsN(context, index, sameGuildAsContext)
    if (category == null) {
        val msg = context.getTranslation(MESSAGE_UNKNOWN_CATEGORY)
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return category
}


fun getTextChannelByArgsN(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): TextChannel? {
    var channel: TextChannel? = null
    if (context.args.size <= index) return null

    if (!context.isFromGuild && sameGuildAsContext) return channel
    val arg = context.args[index]

    channel = if (DISCORD_ID.matches(arg)) {
        context.shardManager.getTextChannelById(arg)

    } else if (context.isFromGuild && context.guild.getTextChannelsByName(arg, true).size > 0) {
        context.guild.getTextChannelsByName(arg, true)[0]

    } else if (CHANNEL_MENTION.matches(arg)) {
        val id = (CHANNEL_MENTION.find(arg) ?: return null).groupValues[1]
        context.message.mentionedChannels.firstOrNull { it.id == id }
            ?: context.shardManager.getTextChannelById(id)

    } else channel

    if (sameGuildAsContext && !context.guild.textChannels.contains(channel)) return null
    return channel
}

suspend fun getTextChannelByArgsNMessage(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): TextChannel? {
    if (argSizeCheckFailed(context, index)) return null
    val textChannel = getTextChannelByArgsN(context, index, sameGuildAsContext)
    if (textChannel == null) {
        val msg = context.getTranslation("message.unknown.textchannel")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return textChannel
}


fun getVoiceChannelByArgsN(context: ICommandContext, index: Int, sameGuildAsContext: Boolean = true): VoiceChannel? {
    var channel: VoiceChannel? = null
    if (!context.isFromGuild && sameGuildAsContext) return channel
    if (context.args.size > index && context.isFromGuild) {
        val arg = context.args[index]

        channel = if (DISCORD_ID.matches(arg)) {
            context.shardManager.getVoiceChannelById(arg)

        } else if (context.isFromGuild && context.guild.getVoiceChannelsByName(arg, true).size > 0) {
            context.guild.getVoiceChannelsByName(arg, true)[0]

        } else if (CHANNEL_MENTION.matches(arg)) {
            val id = (CHANNEL_MENTION.find(arg) ?: return null).groupValues[1]
            context.shardManager.getVoiceChannelById(id)

        } else channel
    }
    if (sameGuildAsContext && !context.guild.voiceChannels.contains(channel)) return null
    return channel
}

suspend fun getVoiceChannelByArgNMessage(
    context: ICommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true
): VoiceChannel? {
    if (argSizeCheckFailed(context, index)) return null
    val voiceChannel = getVoiceChannelByArgsN(context, index, sameGuildAsContext)
    if (voiceChannel == null) {
        val msg = context.getTranslation("message.unknown.voicechannel")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return voiceChannel
}


suspend fun retrieveMemberByArgsN(guild: Guild, arg: String): Member? {
    val user = retrieveUserByArgsN(guild, arg)

    return if (user == null) null
    else guild.retrieveMember(user).awaitOrNull()
}


suspend fun retrieveMemberByArgsNMessage(
    context: ICommandContext,
    index: Int,
    interactable: Boolean = false,
    botAllowed: Boolean = true
): Member? {
    if (!context.isFromGuild) throw IllegalStateException("Trying to get members in dms")
    if (argSizeCheckFailed(context, index)) return null
    val user = retrieveUserByArgsN(context, index)
    val member =
        if (user == null) null
        else {
            context.message.mentionedMembers.firstOrNull { it.id == user.id }
                ?: context.guild.retrieveMember(user).awaitOrNull()
        }

    if (member == null) {
        val msg = context.getTranslation("message.unknown.member")
            .withSafeVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
        return null
    }

    if (interactable && !member.guild.selfMember.canInteract(member)) {
        val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
            .withSafeVariable(PLACEHOLDER_USER, member.asTag)
        sendRsp(context, msg)
        return null
    }

    if (!botAllowed && member.user.isBot) {
        val msg = context.getTranslation("message.interact.member.isbot")
            .withSafeVariable(PLACEHOLDER_USER, member.asTag)
        sendRsp(context, msg)
        return null
    }


    return member
}


suspend fun notEnoughPermissionsAndMessage(
    context: ICommandContext,
    channel: GuildChannel,
    vararg perms: Permission
): Boolean {
    val member = channel.guild.selfMember
    val result = notEnoughPermissions(member, channel, perms.toList())
    if (result.first.isNotEmpty()) {
        val more = if (result.first.size > 1) "s" else ""
        val msg = context.getTranslation("message.discordchannelpermission$more.missing")
            .withVariable("permissions", result.first.joinToString(separator = "") { perm ->
                "\n    ⁎ `${perm.toUCSC()}`"
            })
            .withSafeVariable("channel", channel.name)

        sendRsp(context, msg)
        return true
    } else if (result.second.isNotEmpty()) {
        val more = if (result.second.size > 1) "s" else ""
        val msg = context.getTranslation("message.discordcategorypermission$more.missing")
            .withVariable("permissions", result.second.joinToString(separator = "") { perm ->
                "\n    ⁎ `${perm.toUCSC()}`"
            })
            .withSafeVariable("category", channel.parent?.name ?: "error")

        sendRsp(context, msg)
        return true
    }
    return false
}

fun notEnoughPermissions(
    member: Member,
    channel: GuildChannel,
    perms: Collection<Permission>
): Pair<List<Permission>, List<Permission>> {
    val missingPerms = mutableListOf<Permission>()
    for (perm in perms) {
        if (!member.hasPermission(channel, perm)) missingPerms.add(perm)
    }
    return Pair(missingPerms, emptyList())
}


fun getTimespanFromArgNMessage(context: ICommandContext, beginIndex: Int): Pair<Long, Long> {
    return when (context.getRawArgPart(beginIndex, -1)) {
        "hour" -> {
            Pair(context.contextTime - 3_600_000, context.contextTime)
        }
        "day" -> {
            Pair(context.contextTime - 86_400_000, context.contextTime)
        }
        "week" -> {
            Pair(context.contextTime - 604_800_000, context.contextTime)
        }
        "month" -> {
            Pair(context.contextTime - 2_592_000_000L, context.contextTime)
        }
        "year" -> {
            Pair(context.contextTime - 31_449_600_000, context.contextTime)
        }
        "thishour" -> {
            val c: Calendar = Calendar.getInstance()
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            Pair(c.timeInMillis, context.contextTime)
        }
        "today", "thisday" -> {
            val c: Calendar = Calendar.getInstance()
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            Pair(c.timeInMillis, context.contextTime)
        }
        "thisweek" -> {
            val c: Calendar = Calendar.getInstance()
            c.set(Calendar.DAY_OF_WEEK, 0)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            Pair(c.timeInMillis, context.contextTime)
        }
        "thismonth" -> {
            val c: Calendar = Calendar.getInstance()
            c.set(Calendar.WEEK_OF_MONTH, 0)
            c.set(Calendar.DAY_OF_WEEK, 0)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            Pair(c.timeInMillis, context.contextTime)
        }
        "thisyear" -> {
            val c: Calendar = Calendar.getInstance()
            c.set(Calendar.MONTH, 0)
            c.set(Calendar.WEEK_OF_MONTH, 0)
            c.set(Calendar.DAY_OF_WEEK, 0)
            c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0)
            c.set(Calendar.SECOND, 0)
            c.set(Calendar.MILLISECOND, 0)
            Pair(c.timeInMillis, context.contextTime)
        }
        "all" -> {
            Pair(0, context.contextTime)
        }
        else -> {
            Pair(0, context.contextTime)
        }
    }

}

fun listeningMembers(vc: VoiceChannel, alwaysListeningUser: Long = -1L): Int {
    return vc.members.count { member ->
        // isDeafened checks both guild and self deafened (no worries)
        !member.user.isBot &&
            (member.voiceState?.isDeafened == false) &&
            (member.idLong != alwaysListeningUser) &&
            !isBotBanned(EntityType.USER, member.idLong)
    }
}


suspend fun getTimeFromArgsNMessage(
    context: ICommandContext,
    start: Long = Long.MIN_VALUE,
    end: Long = Long.MAX_VALUE
): Long? {
    val parts = context.rawArg
        .replace(":", " ")
        .split(SPACE_PATTERN).toMutableList()

    parts.reverse()
    var time: Long = 0
    var workingPart = ""
    try {
        for ((index, part) in parts.withIndex()) {
            workingPart = part
            time += part.toShort() * when (index) {
                0 -> 1000
                1 -> 60_000
                2 -> 3_600_000
                else -> 0
            }
        }
    } catch (ex: NumberFormatException) {
        val path = if (workingPart.isPositiveNumber()) {
            "message.numbertobig"
        } else {
            "message.unknown.number"
        }
        val msg = context.getTranslation(path)
            .withSafeVariable(PLACEHOLDER_ARG, workingPart)
        sendRsp(context, msg)
        return null
    }
    if (start > time || end < time) {
        val msg = context.getTranslation("command.seek.notinrange")
            .withSafeVariable(PLACEHOLDER_ARG, workingPart)
        sendRsp(context, msg)
        return null
    }
    return time
}
