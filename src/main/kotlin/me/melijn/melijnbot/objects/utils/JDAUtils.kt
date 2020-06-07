package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.concurrent.Task
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import java.awt.Color
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


val DISCORD_ID: Pattern = Pattern.compile("\\d{17,20}") // ID
val FULL_USER_REF: Pattern = Pattern.compile("(\\S.{0,30}\\S)\\s*#(\\d{4})") // $1 -> username, $2 -> discriminator
val USER_MENTION: Pattern = Pattern.compile("<@!?(\\d{17,20})>") // $1 -> ID
val CHANNEL_MENTION: Pattern = Pattern.compile("<#(\\d{17,20})>") // $1 -> ID
val ROLE_MENTION: Pattern = Pattern.compile("<@&(\\d{17,20})>") // $1 -> ID
val EMOTE_MENTION: Pattern = Pattern.compile("<a?:(.{2,32}):(\\d{17,20})>") // $1 -> NAME, $2 -> ID

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


fun getUserByArgsN(context: CommandContext, index: Int): User? {//With null
    val shardManager = context.shardManager
    return if (context.args.size > index) {
        getUserByArgsN(shardManager, context.guildN, context.args[index])
    } else {
        null
    }
}

fun getUserByArgsN(shardManager: ShardManager, guild: Guild?, arg: String): User? {

    val idMatcher = DISCORD_ID.matcher(arg)
    val argMentionMatcher = USER_MENTION.matcher(arg)
    val fullUserRefMatcher = FULL_USER_REF.matcher(arg)

    return if (idMatcher.matches()) {
        shardManager.getUserById(arg)
    } else if (argMentionMatcher.matches()) {
        shardManager.getUserById(argMentionMatcher.group(1))
    } else if (guild != null && fullUserRefMatcher.matches()) {
        val byName = guild.jda.getUsersByName(fullUserRefMatcher.group(1), true)
        val matches = byName.filter { user -> user.discriminator == fullUserRefMatcher.group(2) }
        if (matches.isEmpty()) {
            null
        } else if (matches.size == 1) {
            matches[0]
        } else {
            val perfect = matches.filter { user -> user.name == fullUserRefMatcher.group(1) }
            if (perfect.isEmpty()) {
                matches[0]
            } else {
                perfect[0]
            }
        }
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

suspend fun retrieveUserByArgsN(context: CommandContext, index: Int): User? = suspendCoroutine {
    val user1: User? = getUserByArgsN(context, index)
    when {
        user1 != null -> it.resume(user1)
        context.args.size > index -> {
            val arg = context.args[index]
            val idMatcher = DISCORD_ID.matcher(arg)
            val mentionMatcher = USER_MENTION.matcher(arg)

            when {
                idMatcher.matches() -> context.jda.shardManager?.retrieveUserById(arg)
                mentionMatcher.find() -> {
                    val id = mentionMatcher.group(1).toLong()
                    context.jda.shardManager?.retrieveUserById(id)
                }
                else -> null
            }?.queue({ user ->
                it.resume(user)
            }, { _ ->
                it.resume(null)
            }) ?: it.resume(null)

        }
        else -> it.resume(null)
    }

}

suspend fun retrieveUserByArgsN(guild: Guild, arg: String): User? = suspendCoroutine {
    val shardManager = guild.jda.shardManager
    if (shardManager == null) {
        it.resume(null)
        return@suspendCoroutine
    }
    val user1: User? = getUserByArgsN(shardManager, guild, arg)
    if (user1 != null) {
        it.resume(user1)
        return@suspendCoroutine
    }

    val idMatcher = DISCORD_ID.matcher(arg)
    val mentionMatcher = USER_MENTION.matcher(arg)

    when {
        idMatcher.matches() -> shardManager.retrieveUserById(arg)
        mentionMatcher.matches() -> {
            shardManager.retrieveUserById(mentionMatcher.group(1))
        }

        else -> {
            it.resume(null)
            return@suspendCoroutine
        }
    }.queue({ user ->
        it.resume(user)
    }, { _ ->
        it.resume(null)
    })
}

suspend fun retrieveUserByArgsNMessage(context: CommandContext, index: Int): User? {
    val possibleUser = retrieveUserByArgsN(context, index)
    if (possibleUser == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_USER)
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    }
    return possibleUser
}

suspend fun getUserByArgsNMessage(context: CommandContext, index: Int): User? {
    val user = getUserByArgsN(context, index)
    if (user == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, MESSAGE_UNKNOWN_USER)

            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    }
    return user
}

fun getRoleByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): Role? {
    var role: Role? = null
    if (!context.isFromGuild && sameGuildAsContext) return role
    if (context.args.size > index) {
        val arg = context.args[index]

        role = if (arg.matches(Regex("\\d+")) && context.jda.shardManager?.getRoleById(arg) != null)
            context.jda.shardManager?.getRoleById(arg)
        else if (context.isFromGuild && context.guild.getRolesByName(arg, true).size > 0)
            context.guild.getRolesByName(arg, true)[0]
        else if (arg.matches(Regex("<@&\\d+>"))) {
            var role2: Role? = null
            val pattern = Pattern.compile("<@&(\\d+)>")
            val matcher = pattern.matcher(arg)
            while (matcher.find()) {
                val id = matcher.group(1)
                val role3 = context.jda.shardManager?.getRoleById(id)
                if (role2 != null && role3 == null) continue
                role2 = role3
            }
            role2
        } else role
    }
    if (sameGuildAsContext && !context.guild.roles.contains(role)) return null
    return role
}

suspend fun getRoleByArgsNMessage(
    context: CommandContext,
    index: Int,
    sameGuildAsContext: Boolean = true,
    canInteract: Boolean = false
): Role? {
    val role = getRoleByArgsN(context, index, sameGuildAsContext)
    if (role == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.role")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    } else if (canInteract) {
        if (!context.guild.selfMember.canInteract(role)) {
            val language = context.getLanguage()
            val msg = i18n.getTranslation(language, MESSAGE_SELFINTERACT_ROLE_HIARCHYEXCEPTION)
                .replace(PLACEHOLDER_ROLE, context.args[index])
            sendMsg(context, msg)
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
            .replace("%min%", min.toString())
        sendMsg(context, msg)
        return null
    }
    if (arg.length > max) {
        val msg = context.getTranslation("message.string.maxfailed")
            .replace("%max%", max.toString())
        sendMsg(context, msg)
        return null
    }
    if (mustMatch != null && !mustMatch.matches(arg)) {
        val msg = context.getTranslation("message.string.matchfailed")
            .replace("%pattern%", mustMatch.toString())
        sendMsg(context, msg)
        return null
    }
    for (char in cantContainChars) {
        if (arg.contains(char, ignoreCase)) {
            val msg = context.getTranslation("message.string.cantcontaincharfailed")
                .replace("%chars%", cantContainChars.toString())
                .replace("%char%", char.toString())
                .replace("%ignorecase%", ignoreCase.toString())
            sendMsg(context, msg)
            return null
        }
    }
    for (word in cantContainWords) {
        if (arg.contains(word, ignoreCase)) {
            val msg = context.getTranslation("message.string.cantcontainwordfailed")
                .replace("%words%", cantContainWords.toString())
                .replace("%word%", word.toString())
                .replace("%ignorecase%", ignoreCase.toString())
            sendMsg(context, msg)
            return null
        }
    }

    return arg
}

suspend fun getEmoteByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): Emote? {
    val emote = getEmoteByArgsN(context, index, sameGuildAsContext)
    if (emote == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.emote")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    }
    return emote
}

suspend fun getEmotejiByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = false): Pair<Emote?, String?>? {
    val emoteji = getEmotejiByArgsN(context, index, sameGuildAsContext)
    if (emoteji == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.emojioremote")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
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


suspend fun getEmoteByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean): Emote? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val matcher = DISCORD_ID.matcher(arg)
    val emoteMatcher = EMOTE_MENTION.matcher(arg)
    var emote: Emote? = null
    if (matcher.matches()) {
        emote = context.shardManager.getEmoteById(arg)

    } else if (emoteMatcher.find()) {
        val id = emoteMatcher.group(2).toLong()
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

suspend fun getColorFromArgNMessage(context: CommandContext, index: Int): Color? {
    if (argSizeCheckFailed(context, index)) return null
    val arg = context.args[index]
    val color: Color? = when {
        arg.matches("(?i)#?([a-f]|\\d){6}".toRegex()) -> {
            if (arg.startsWith("#")) Color.decode(arg)
            else Color.decode("#$arg")
        }
        arg.matches("\\s*\\d+,\\s*\\d+,\\s*\\d+".toRegex()) -> {
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
        arg.matches("-?\\d+".toRegex()) -> {
            if (arg.toIntOrNull() == null) null
            else Color(arg.toInt())
        }
        else -> {
            Color.getColor(arg)
        }
    }
    if (color == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.color")
            .replace(PLACEHOLDER_ARG, arg)
        sendMsg(context, msg)
    }
    return color
}

suspend fun JDA.messageByJSONNMessage(context: CommandContext, json: String): MessageEmbed? {
    val jdaImpl = (this as JDAImpl)

    return try {
        jdaImpl.entityBuilder.createMessageEmbed(DataObject.fromJson(json))
    } catch (e: Exception) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.invalidJSONStruct")
            .replace("%cause%", e.message ?: "unknown")
        sendMsg(context, msg)
        null
    }
}

fun getTextChannelByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): TextChannel? {
    var channel: TextChannel? = null
    if (!context.isFromGuild && sameGuildAsContext) return channel
    if (context.args.size > index && context.isFromGuild) {
        val arg = context.args[index]

        channel = if (arg.matches(Regex("\\d+"))) {
            context.jda.shardManager?.getTextChannelById(arg)
        } else if (context.isFromGuild && context.guild.getTextChannelsByName(arg, true).size > 0) {
            context.guild.getTextChannelsByName(arg, true)[0]
        } else if (arg.matches(Regex("<#\\d+>"))) {
            var textChannel1: TextChannel? = null
            val pattern = Pattern.compile("<#(\\d+)>")
            val matcher = pattern.matcher(arg)
            while (matcher.find()) {
                val id = matcher.group(1)
                val textChannel2 = context.jda.shardManager?.getTextChannelById(id)
                if (textChannel1 != null && textChannel2 == null) continue
                textChannel1 = textChannel2
            }
            textChannel1
        } else channel
    }
    if (sameGuildAsContext && !context.guild.textChannels.contains(channel)) return null
    return channel
}

suspend fun getTextChannelByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): TextChannel? {
    if (argSizeCheckFailed(context, index)) return null
    val textChannel = getTextChannelByArgsN(context, index, sameGuildAsContext)
    if (textChannel == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.textchannel")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    }
    return textChannel
}


fun getVoiceChannelByArgsN(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): VoiceChannel? {
    var channel: VoiceChannel? = null
    if (!context.isFromGuild && sameGuildAsContext) return channel
    if (context.args.size > index && context.isFromGuild) {
        val arg = context.args[index]

        channel = if (arg.matches(Regex("\\d+"))) {
            context.jda.shardManager?.getVoiceChannelById(arg)
        } else if (context.isFromGuild && context.guild.getVoiceChannelsByName(arg, true).size > 0) {
            context.guild.getVoiceChannelsByName(arg, true)[0]
        } else if (arg.matches(Regex("<#\\d+>"))) {
            var voiceChannel1: VoiceChannel? = null
            val pattern = Pattern.compile("<#(\\d+)>")
            val matcher = pattern.matcher(arg)
            while (matcher.find()) {
                val id = matcher.group(1)
                val voiceChannel2 = context.jda.shardManager?.getVoiceChannelById(id)
                if (voiceChannel1 != null && voiceChannel2 == null) continue
                voiceChannel1 = voiceChannel2
            }
            voiceChannel1
        } else channel
    }
    if (sameGuildAsContext && !context.guild.voiceChannels.contains(channel)) return null
    return channel
}

suspend fun getVoiceChannelByArgNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): VoiceChannel? {
    val voiceChannel = getVoiceChannelByArgsN(context, index, sameGuildAsContext)
    if (voiceChannel == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.voicechannel")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
    }
    return voiceChannel
}

suspend fun retrieveMemberByArgsN(context: CommandContext, index: Int): Member? {
    val user = retrieveUserByArgsN(context, index)
    val member =
        if (user == null) null
        else context.guild.retrieveMember(user).awaitOrNull()

    return member
}

suspend fun retrieveMemberByArgsN(guild: Guild, arg: String): Member? {
    val user = retrieveUserByArgsN(guild, arg)
    val member =
        if (user == null) null
        else guild.retrieveMember(user).awaitOrNull()

    return member
}


suspend fun retrieveMemberByArgsNMessage(context: CommandContext, index: Int, interactable: Boolean = false, botAllowed: Boolean = true): Member? {
    val user = retrieveUserByArgsN(context, index)
    val member =
        if (user == null) null
        else context.guild.retrieveMember(user).awaitOrNull()

    if (member == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.member")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
        return null
    }

    if (interactable && !member.guild.selfMember.canInteract(member)) {
        val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
            .replace(PLACEHOLDER_USER, member.asTag)
        sendMsg(context, msg)
        return null
    }

    if (!botAllowed && member.user.isBot) {
        val msg = context.getTranslation("message.interact.member.isbot")
            .replace(PLACEHOLDER_USER, member.asTag)
        sendMsg(context, msg)
        return null
    }


    return member
}

suspend fun getMemberByArgsNMessage(context: CommandContext, index: Int, interactable: Boolean = false, botAllowed: Boolean = true): Member? {
    val user = getUserByArgsN(context, index)
    val member =
        if (user == null) null
        else context.guild.retrieveMember(user).awaitOrNull()

    if (member == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.member")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg)
        return null
    }

    if (interactable && !member.guild.selfMember.canInteract(member)) {
        val msg = context.getTranslation(MESSAGE_SELFINTERACT_MEMBER_HIARCHYEXCEPTION)
            .replace(PLACEHOLDER_USER, member.asTag)
        sendMsg(context, msg)
        return null
    }

    if (!botAllowed && member.user.isBot) {
        val msg = context.getTranslation("message.interact.member.isbot")
            .replace(PLACEHOLDER_USER, member.asTag)
        sendMsg(context, msg)
        return null
    }


    return member
}

suspend fun getMemberByArgsN(guild: Guild, arg: String): Member? {
    val shardManager = guild.jda.shardManager ?: return null
    val user = getUserByArgsN(shardManager, guild, arg)

    return if (user == null) null
    else guild.retrieveMember(user).awaitOrNull()
}

suspend fun notEnoughPermissionsAndMessage(context: CommandContext, channel: GuildChannel, vararg perms: Permission): Boolean {
    val member = channel.guild.selfMember
    val result = notEnoughPermissions(member, channel, perms.toList())
    if (result.first.isNotEmpty()) {
        val more = if (result.first.size > 1) "s" else ""
        val msg = context.getTranslation("message.discordpermission$more.missing")
            .replace("%permissions%", result.first.joinToString(separator = "") { perm ->
                "\n    ⁎ `${perm.toUCSC()}`"
            })
            .replace("%channel%", channel.name)

        sendMsg(context, msg)
        return true
    } else if (result.second.isNotEmpty()) {
        val more = if (result.second.size > 1) "s" else ""
        val msg = context.getTranslation("message.discordcategorypermission$more.missing")
            .replace("%permissions%", result.second.joinToString(separator = "") { perm ->
                "\n    ⁎ `${perm.toUCSC()}`"
            })
            .replace("%category%", channel.parent?.name ?: "Yhea go to support and report this bug lol")

        sendMsg(context, msg)
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
        .split("\\s+".toRegex()).toMutableList()
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
        val path = if (workingPart.matches("\\d+".toRegex())) {
            "message.numbertobig"
        } else {
            "message.unknown.number"
        }
        val msg = context.getTranslation(path)
            .replace(PLACEHOLDER_ARG, workingPart)
        sendMsg(context, msg)
        return null
    }
    if (start > time || end < time) {
        val msg = context.getTranslation("command.seek.notinrange")
            .replace(PLACEHOLDER_ARG, workingPart)
        sendMsg(context, msg)
        return null
    }
    return time
}