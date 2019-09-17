package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.MESSAGE_UNKNOWN_USER
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.translation.i18n
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.data.DataObject
import net.dv8tion.jda.internal.JDAImpl
import java.awt.Color
import java.util.regex.Pattern
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


val DISCORD_ID: Pattern = Pattern.compile("\\d{17,20}") // ID
val FULL_USER_REF: Pattern = Pattern.compile("(\\S.{0,30}\\S)\\s*#(\\d{4})") // $1 -> username, $2 -> discriminator
val USER_MENTION: Pattern = Pattern.compile("<@!?(\\d{17,20})>") // $1 -> ID
val CHANNEL_MENTION: Pattern = Pattern.compile("<#(\\d{17,20})>") // $1 -> ID
val ROLE_MENTION: Pattern = Pattern.compile("<@&(\\d{17,20})>") // $1 -> ID
val EMOTE_MENTION: Pattern = Pattern.compile("<:(.{2,32}):(\\d{17,20})>")

val Member.asTag: String
    get() = this.user.asTag

val TextChannel.asTag: String
    get() = "#${this.name}"

suspend fun <T> RestAction<T>.await() = suspendCoroutine<T> {
    queue(
        { success -> it.resume(success) },
        { failure -> it.resumeWithException(failure) }
    )
}

//fun getUserByArgs(context: CommandContext, index: Int): User {
//    var user = getUserByArgsN(context, index)
//    if (user == null) user = context.getAuthor()
//    return user
//}


fun getUserByArgsN(context: CommandContext, index: Int): User? {//With null
    val shardManager = context.getShardManager() ?: return null
    return if (context.args.size > index) {
        getUserByArgsN(shardManager, context.getGuildN(), context.args[index])
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
    if (user1 != null) {
        it.resume(user1)
    } else if (context.args.size > index) {
        val arg = context.args[index]
        val idMatcher = DISCORD_ID.matcher(arg)
        val mentionMatcher = USER_MENTION.matcher(arg)

        when {
            idMatcher.matches() -> context.jda.shardManager?.retrieveUserById(arg)
            mentionMatcher.matches() -> {
                val id = arg.substring(2, arg.lastIndex - 1).toLong()
                context.jda.shardManager?.retrieveUserById(id)
            }
            else -> null
        }?.queue({ user ->
            it.resume(user)
        }, { _ ->
            it.resume(null)
        })
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
        sendMsg(context, msg, null)
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
        else if (context.isFromGuild && context.getGuild().getRolesByName(arg, true).size > 0)
            context.getGuild().getRolesByName(arg, true)[0]
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
    if (sameGuildAsContext && !context.getGuild().roles.contains(role)) return null
    return role
}

suspend fun getRoleByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): Role? {
    val role = getRoleByArgsN(context, index, sameGuildAsContext)
    if (role == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.role")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg, null)
    }
    return role
}

suspend fun getColorFromArgNMessage(context: CommandContext, index: Int): Color? {
    val arg = context.args[index]
    when {
        arg.matches("(?i)#([a-f]|\\d){6}".toRegex()) -> {

            var red: Int = Integer.valueOf(arg.substring(1, 3), 16)
            var green: Int = Integer.valueOf(arg.substring(3, 5), 16)
            var blue: Int = Integer.valueOf(arg.substring(5, 7), 16)
            red = red shl 16 and 0xFF0000
            green = green shl 8 and 0x00FF00
            blue = blue and 0x0000FF
            return Color.getColor((red or green or blue).toString())
        }
        else -> {
            val color: Color? = Color.getColor(arg)
            if (color == null) {
                val language = context.getLanguage()
                val msg = i18n.getTranslation(language, "message.unknown.color")
                    .replace(PLACEHOLDER_ARG, arg)
                sendMsg(context, msg, null)
            }
            return color
        }
    }
}

suspend fun JDA.messageByJSONNMessage(context: CommandContext, json: String): MessageEmbed? {
    val jdaImpl = (this as JDAImpl)

    return try {
        jdaImpl.entityBuilder.createMessageEmbed(DataObject.fromJson(json))
    } catch (e: Exception) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.invalidJSONStruct")
            .replace("%cause%", e.message ?: "unknown")
        sendMsg(context, msg, null)
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
        } else if (context.isFromGuild && context.getGuild().getTextChannelsByName(arg, true).size > 0) {
            context.getGuild().getTextChannelsByName(arg, true)[0]
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
    if (sameGuildAsContext && !context.getGuild().textChannels.contains(channel)) return null
    return channel
}

suspend fun getTextChannelByArgsNMessage(context: CommandContext, index: Int, sameGuildAsContext: Boolean = true): TextChannel? {
    val textChannel = getTextChannelByArgsN(context, index, sameGuildAsContext)
    if (textChannel == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.textchannel")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg, null)
    }
    return textChannel
}

suspend fun getMemberByArgsNMessage(context: CommandContext, index: Int): Member? {
    val user = getUserByArgsN(context, index)
    val member =
        if (user == null) null
        else context.getGuild().getMember(user)

    if (member == null) {
        val language = context.getLanguage()
        val msg = i18n.getTranslation(language, "message.unknown.member")
            .replace(PLACEHOLDER_ARG, context.args[index])
        sendMsg(context, msg, null)
    }

    return member
}

fun getMemberByArgsN(guild: Guild, arg: String): Member? {
    val shardManager = guild.jda.shardManager ?: return null
    val user = getUserByArgsN(shardManager, guild, arg)

    return if (user == null) null
    else guild.getMember(user)
}