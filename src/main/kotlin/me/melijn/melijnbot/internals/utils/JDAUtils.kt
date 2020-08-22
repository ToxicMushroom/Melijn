package me.melijn.melijnbot.internals.utils

import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.ShardManager
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

suspend fun <T> Task<T>.await(failure: ((Throwable) -> Unit)? = null) = suspendCoroutine<T> {
    onSuccess { success ->
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


fun getUserByArgsN(context: CommandContext, index: Int): User? {//With null
    val shardManager = context.shardManager
    return if (context.args.size > index) {
        getUserByArgsN(shardManager, context.guildN, context.args[index])
    } else {
        null
    }
}

fun getUserByArgsN(shardManager: ShardManager, guild: Guild?, arg: String): User? {
    return if (DISCORD_ID.matches(arg)) {
        shardManager.getUserById(arg)
    } else if (USER_MENTION.matches(arg)) {
        shardManager.getUserById((USER_MENTION.find(arg) ?: return null).groupValues[1])
    } else if (guild != null && FULL_USER_REF.matches(arg)) {
        shardManager.getUserByTag(arg)
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

suspend fun retrieveUserByArgsN(context: CommandContext, index: Int): User? {
    val user1: User? = getUserByArgsN(context, index)
    return when {
        user1 != null -> user1
        context.args.size > index -> {
            val arg = context.args[index]

            when {
                DISCORD_ID.matches(arg) -> {
                    context.shardManager.retrieveUserById(arg).awaitOrNull()
                }
                USER_MENTION.matches(arg) -> {
                    val id = (USER_MENTION.find(arg) ?: return null).groupValues[1]
                    context.shardManager.retrieveUserById(id).awaitOrNull()
                }
                else -> context.guild.retrieveMembersByPrefix(arg, 1).awaitOrNull()?.firstOrNull()?.user
            }
        }
        else -> null
    }
}

suspend fun retrieveUserByArgsN(guild: Guild, arg: String): User? {
    val shardManager = guild.jda.shardManager ?: return null
    val user1: User? = getUserByArgsN(shardManager, guild, arg)
    if (user1 != null) {
        return user1
    }

    return when {
        DISCORD_ID.matches(arg) -> shardManager.retrieveUserById(arg)
        USER_MENTION.matches(arg) -> {
            shardManager.retrieveUserById((USER_MENTION.find(arg) ?: return null).groupValues[1])
        }

        else -> {
            return guild.retrieveMembersByPrefix(arg, 1).awaitOrNull()?.firstOrNull()?.user
        }
    }.awaitOrNull()
}

suspend fun retrieveUserByArgsNMessage(context: CommandContext, index: Int): User? {
    if (argSizeCheckFailed(context, index)) return null
    val possibleUser = retrieveUserByArgsN(context, index)
    if (possibleUser == null) {
        val msg = context.getTranslation(MESSAGE_UNKNOWN_USER)
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return possibleUser
}


fun getRoleByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): Role? {
    var role: Role? = null

    if (!context.isFromGuild && sameGuildAsContext) return role
    if (context.args.size <= index) return role

    val arg = context.args[index]

    role = if (arg.isPositiveNumber() && context.jda.shardManager?.getRoleById(arg) != null) {
        context.shardManager.getRoleById(arg)

    } else if (context.isFromGuild && context.guild.getRolesByName(arg, true).size > 0) {
        context.guild.getRolesByName(arg, true)[0]

    } else if (ROLE_MENTION.matches(arg)) {
        val id = (ROLE_MENTION.find(arg) ?: return null).groupValues[1]
        context.shardManager.getRoleById(id)

    } else role

    if (sameGuildAsContext && !context.guild.roles.contains(role)) return null
    return role
}

suspend fun getRoleByArgsNMessage(
    context: CommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true,
    canInteract: Boolean = false
): Role? {
    if (argSizeCheckFailed(context, index)) return null
    val role = getRoleByArgsN(context, index, sameGuildAsContext)
    if (role == null) {
        val msg = context.getTranslation("message.unknown.role")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)

    } else if (canInteract) {
        if (!context.guild.selfMember.canInteract(role)) {
            val msg = context.getTranslation(MESSAGE_SELFINTERACT_ROLE_HIARCHYEXCEPTION)
                .withVariable(PLACEHOLDER_ROLE, context.args[index])
            sendRsp(context, msg)
            return null
        }
    }
    return role
}

suspend fun getStringFromArgsNMessage(
    context: CommandContext,
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
            .withVariable("arg", arg)
            .withVariable("min", min)
            .withVariable("length", arg.length)
        sendRsp(context, msg)
        return null
    }
    if (arg.length > max) {
        val msg = context.getTranslation("message.string.maxfailed")
            .withVariable("arg", arg)
            .withVariable("max", max)
            .withVariable("length", arg.length)
        sendRsp(context, msg)
        return null
    }
    if (mustMatch != null && !mustMatch.matches(arg)) {
        val msg = context.getTranslation("message.string.matchfailed")
            .withVariable("arg", arg)
            .withVariable("pattern", mustMatch)
        sendRsp(context, msg)
        return null
    }
    for (char in cantContainChars) {
        if (arg.contains(char, ignoreCase)) {
            val msg = context.getTranslation("message.string.cantcontaincharfailed")
                .withVariable("arg", arg)
                .withVariable("chars", cantContainChars)
                .withVariable("char", char)
                .withVariable("ignorecase", ignoreCase)
            sendRsp(context, msg)
            return null
        }
    }
    for (word in cantContainWords) {
        if (arg.contains(word, ignoreCase)) {
            val msg = context.getTranslation("message.string.cantcontainwordfailed")
                .withVariable("arg", arg)
                .withVariable("words", cantContainWords)
                .withVariable("word", word)
                .withVariable("ignorecase", ignoreCase)
            sendRsp(context, msg)
            return null
        }
    }

    return arg
}

suspend fun getEmotejiByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = false): Pair<Emote?, String?>? {
    if (argSizeCheckFailed(context, index)) return null
    val emoteji = getEmotejiByArgsN(context, index, sameGuildAsContext)
    if (emoteji == null) {
        val msg = context.getTranslation("message.unknown.emojioremote")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }

    return emoteji
}


suspend fun getEmotejiByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = false): Pair<Emote?, String?>? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val emoji = if (SupportedDiscordEmoji.helpMe.contains(arg)) {
        arg
    } else {
        null
    }

    val emote = if (emoji == null) {
        getEmoteByArgsN(context, index, sameGuildAsContext)
    } else null

    if (emoji == null && emote == null) {
        return null
    }
    return Pair(emote, emoji)
}

suspend fun getEmoteByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean): Emote? {
    if (argSizeCheckFailed(context, index)) return null
    val emote = getEmoteByArgsN(context, index, sameGuildAsContext)
    if (emote == null) {
        val msg = context.getTranslation("message.unknown.emote")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return emote
}


suspend fun getEmoteByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean): Emote? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    var emote: Emote? = null

    if (DISCORD_ID.matches(arg)) {
        emote = context.shardManager.getEmoteById(arg)

    } else if (EMOTE_MENTION.matches(arg)) {
        val id = (EMOTE_MENTION.find(arg) ?: return null).groupValues[2].toLong()
        emote = context.shardManager.getEmoteById(id)

    } else {
        var emotes: List<Emote>? = context.guild.getEmotesByName(arg, false)
        if (emotes?.isNotEmpty() == true) emote = emotes[0]

        emotes = context.guild.getEmotesByName(arg, true)
        if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

        emotes = context.shardManager.getEmotesByName(arg, false)
        if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

        emotes = context.shardManager.getEmotesByName(arg, true)
        if (emotes.isNotEmpty() && emote == null) emote = emotes[0]

    }

    return if (sameGuildAsContext && (emote?.guild?.idLong != context.guildId)) {
        null
    } else {
        emote
    }
}

val hexColorRegex = Regex("#?([a-f]|\\d){3}(([a-f]|\\d){3})?", RegexOption.IGNORE_CASE)
val rgbColorRegex = Regex("\\s*\\d+,\\s*\\d+,\\s*\\d+")

suspend fun getColorFromArgNMessage(context: CommandContext, index: Int): Color? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val color: Color? = when {
        hexColorRegex.matches(arg) -> {
            if (arg.startsWith("#")) Color.decode(arg)
            else Color.decode("#$arg")
        }
        rgbColorRegex.matches(arg) -> {
            val parts = arg.split(",")
            val r = parts[0]
                .trim()
                .toIntOrNull()
            val g = parts[1]
                .trim()
                .toIntOrNull()
            val b = parts[2]
                .trim()
                .toIntOrNull()

            if (r == null || g == null || b == null) null
            else {
                Color(r, g, b)
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
            .withVariable(PLACEHOLDER_ARG, arg)
        sendRsp(context, msg)
    }
    return color
}

fun getTextChannelByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): TextChannel? {
    var channel: TextChannel? = null
    if (!context.isFromGuild && sameGuildAsContext) return channel
    if (context.args.size > index && context.isFromGuild) {
        val arg = context.args[index]

        channel = if (arg.isPositiveNumber()) {
            context.shardManager.getTextChannelById(arg)

        } else if (context.isFromGuild && context.guild.getTextChannelsByName(arg, true).size > 0) {
            context.guild.getTextChannelsByName(arg, true)[0]

        } else if (CHANNEL_MENTION.matches(arg)) {
            val id = (CHANNEL_MENTION.find(arg) ?: return null).groupValues[1]
            context.shardManager.getTextChannelById(id)

        } else channel
    }
    if (sameGuildAsContext && !context.guild.textChannels.contains(channel)) return null
    return channel
}

suspend fun getTextChannelByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): TextChannel? {
    if (argSizeCheckFailed(context, index)) return null
    val textChannel = getTextChannelByArgsN(context, index, sameGuildAsContext)
    if (textChannel == null) {
        val msg = context.getTranslation("message.unknown.textchannel")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return textChannel
}


fun getVoiceChannelByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): VoiceChannel? {
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

suspend fun getVoiceChannelByArgNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): VoiceChannel? {
    if (argSizeCheckFailed(context, index)) return null
    val voiceChannel = getVoiceChannelByArgsN(context, index, sameGuildAsContext)
    if (voiceChannel == null) {
        val msg = context.getTranslation("message.unknown.voicechannel")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
    }
    return voiceChannel
}


suspend fun retrieveMemberByArgsN(guild: Guild, arg: String): Member? {
    val user = retrieveUserByArgsN(guild, arg)

    return if (user == null) null
    else guild.retrieveMember(user).awaitOrNull()
}


suspend fun retrieveMemberByArgsNMessage(context: CommandContext, index: Int, interactable: Boolean = false, botAllowed: Boolean = true): Member? {
    if (argSizeCheckFailed(context, index)) return null
    val user = retrieveUserByArgsN(context, index)
    val member =
        if (user == null) null
        else context.guild.retrieveMember(user).awaitOrNull()

    if (member == null) {
        val msg = context.getTranslation("message.unknown.member")
            .withVariable(PLACEHOLDER_ARG, context.args[index])
        sendRsp(context, msg)
        return null
    }

    if (interactable && !member.guild.selfMember.canInteract(member)) {
        val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
            .withVariable(PLACEHOLDER_USER, member.asTag)
        sendRsp(context, msg)
        return null
    }

    if (!botAllowed && member.user.isBot) {
        val msg = context.getTranslation("message.interact.member.isbot")
            .withVariable(PLACEHOLDER_USER, member.asTag)
        sendRsp(context, msg)
        return null
    }


    return member
}


suspend fun notEnoughPermissionsAndMessage(context: CommandContext, channel: GuildChannel, vararg perms: Permission): Boolean {
    val member = channel.guild.selfMember
    val result = notEnoughPermissions(member, channel, perms.toList())
    if (result.first.isNotEmpty()) {
        val more = if (result.first.size > 1) "s" else ""
        val msg = context.getTranslation("message.discordpermission$more.missing")
            .withVariable("permissions", result.first.joinToString(separator = "") { perm ->
                "\n    ⁎ `${perm.toUCSC()}`"
            })
            .withVariable("channel", channel.name)

        sendRsp(context, msg)
        return true
    } else if (result.second.isNotEmpty()) {
        val more = if (result.second.size > 1) "s" else ""
        val msg = context.getTranslation("message.discordcategorypermission$more.missing")
            .withVariable("permissions", result.second.joinToString(separator = "") { perm ->
                "\n    ⁎ `${perm.toUCSC()}`"
            })
            .withVariable("category", channel.parent?.name ?: "Yhea go to support and report this bug lol")

        sendRsp(context, msg)
        return true
    }
    return false
}

fun notEnoughPermissions(member: Member, channel: GuildChannel, perms: Collection<Permission>): Pair<List<Permission>, List<Permission>> {
    val missingPerms = mutableListOf<Permission>()
    //val missingParentPerms = mutableListOf<Permission>()
    //val parent = channel.parent
    for (perm in perms) {
        if (!member.hasPermission(channel, perm)) missingPerms.add(perm)
        //  if (parent != null && checkParent && !member.hasPermission(parent, perm)) missingParentPerms.add(perm)
    }
    return Pair(missingPerms, emptyList())
}


fun getTimespanFromArgNMessage(context: CommandContext, beginIndex: Int): Pair<Long, Long> {
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
            //val timeStamps = timeStamp.split(">".toRegex())
            Pair(0, context.contextTime)
        }
    }

}

fun listeningMembers(vc: VoiceChannel, alwaysListeningUser: Long = -1L): Int {
    return vc.members.count { member ->
        // isDeafened checks both guild and self deafened (no worries)
        !member.user.isBot && (member.voiceState?.isDeafened == false) && (member.idLong != alwaysListeningUser)
    }
}


suspend fun getTimeFromArgsNMessage(context: CommandContext, start: Long = Long.MIN_VALUE, end: Long = Long.MAX_VALUE): Long? {
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
            .withVariable(PLACEHOLDER_ARG, workingPart)
        sendRsp(context, msg)
        return null
    }
    if (start > time || end < time) {
        val msg = context.getTranslation("command.seek.notinrange")
            .withVariable(PLACEHOLDER_ARG, workingPart)
        sendRsp(context, msg)
        return null
    }
    return time
}