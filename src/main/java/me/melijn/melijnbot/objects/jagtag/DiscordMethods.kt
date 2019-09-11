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
            Method("user", { env ->
                val user: User = env.getReifiedX("user")
                user.asTag
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
            Method("discrim", { env ->
                val user: User = env.getReifiedX("user")
                user.discriminator
            }),
            Method("guildname", { env ->
                val guild: Guild = env.getReifiedX("guild")
                guild.name
            }),
            Method("membercount", { env ->
                val guild: Guild = env.getReifiedX("guild")
                guild.memberCache.size().toString()
            })
    )
}