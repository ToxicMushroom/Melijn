package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.Environment
import com.jagrosh.jagtag.Method
import com.jagrosh.jagtag.ParseException
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.getUserByArgsN
import me.melijn.melijnbot.objects.utils.retrieveMemberByArgsN
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsN
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

object DiscordMethods {
    fun getMethods(): List<Method> = listOf(
        Method("userMention", { env ->
            val user: User = env.getReifiedX("user")
            user.asMention
        }),

        Method("userTag", { env ->
            val user: User = env.getReifiedX("user")
            user.asTag
        }),

        Method("user", { env ->
            val user: User = env.getReifiedX("user")
            user.asTag
        }),

        Method("isBot", { env ->
            val user: User = env.getReifiedX("user")
            if (user.isBot) "true" else "false"
        }),


        //{isUser:userId|userTag}
        Method("isUser", { "true" }, { env: Environment, input: Array<String> ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = input[0]

            (retrieveUserByArgsN(guild, arg) != null).toString()
        }),

        //{isMember:userId|userTag}
        Method("isMember", { "true" }, { env: Environment, input: Array<String> ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = input[0]
            (retrieveMemberByArgsN(guild, arg) != null).toString()
        }),
        Method("userId", { env ->
            val user: User = env.getReifiedX("user")
            user.id
        }),
        Method("username", { env ->
            val user: User = env.getReifiedX("user")
            user.name
        }),
        Method("voiceChannelId", { env ->
            val member: Member? = env.getReified("member")
            member?.voiceState?.channel?.id ?: "null"
        }),
        Method("nickname", { env ->
            val member: Member? = env.getReified("member")
            member?.nickname ?: throw ParseException("no nickname")
        }),
        Method("hasNickname", { env ->
            val member: Member? = env.getReified("member")
            (member?.nickname != null).toString()
        }),
        Method("effectiveName", { env ->
            val member: Member? = env.getReified("member")
            member?.effectiveName ?: "null"
        }),
        Method("effectiveAvatarUrl", { env ->
            val user: User = env.getReifiedX("user")
            user.effectiveAvatarUrl
        }),
        Method("discriminator", { env ->
            val user: User = env.getReifiedX("user")
            user.discriminator
        }),
        Method("guildName", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.name
        }),
        Method("guildId", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.id
        }),
        Method("serverName", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.name
        }),
        Method("serverId", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.id
        }),
        Method("memberCount", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.memberCount.toString()
        }),
        Method("currentTimeMillis", {
            System.currentTimeMillis().toString()
        }),
        Method("currentDateTime", { env ->
            val guild: Guild = env.getReifiedX("guild")
            val user: User = env.getReifiedX("user")
            System.currentTimeMillis().asEpochMillisToDateTime(Container.instance.daoManager, guild.idLong, user.idLong)
        }, { env, args ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = args[0]
            val user: User = getUserByArgsN(guild.jda.shardManager ?: return@Method "null", guild, arg
            ) ?: return@Method "null"

            System.currentTimeMillis().asEpochMillisToDateTime(Container.instance.daoManager, guild.idLong, user.idLong)
        })
    )
}