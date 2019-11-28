package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.Environment
import com.jagrosh.jagtag.Method
import com.jagrosh.jagtag.ParseException
import me.melijn.melijnbot.objects.utils.getMemberByArgsN
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
            (getMemberByArgsN(guild, arg) != null).toString()
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
            val member: Member = env.getReifiedX("member")
            member.voiceState?.channel?.id ?: "null"
        }),
        Method("nickname", { env ->
            val member: Member = env.getReifiedX("member")
            member.nickname ?: throw ParseException("no nickname")
        }),
        Method("hasNickname", { env ->
            val member: Member = env.getReifiedX("member")
            (member.nickname != null).toString()
        }),
        Method("effectiveName", { env ->
            val member: Member = env.getReifiedX("member")
            member.effectiveName
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
        Method("memberCount", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.memberCache.size().toString()
        })
    )
}