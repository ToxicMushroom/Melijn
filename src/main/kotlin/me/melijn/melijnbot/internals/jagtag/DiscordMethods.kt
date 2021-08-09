package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Environment
import com.jagrosh.jagtag.Method
import com.jagrosh.jagtag.ParseException
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.translation.MISSING_IMAGE_URL
import me.melijn.melijnbot.internals.utils.*
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

object DiscordMethods {

    val serverUrlMethods = listOf(
        Method("serverIconUrl", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.iconUrl ?: MISSING_IMAGE_URL
        }),
        Method("serverBannerUrl", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.bannerUrl ?: MISSING_IMAGE_URL
        }),
        Method("serverSplashUrl", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.splashUrl ?: MISSING_IMAGE_URL
        })
    )

    val userUrlMethods = listOf(
        Method("effectiveAvatarUrl", { env ->
            val user: User = env.getReifiedX("user")
            user.effectiveAvatarUrl
        })
    )
    val imgUrlMethods = serverUrlMethods + userUrlMethods

    private val userMethods: List<Method> = userUrlMethods + listOf(
        Method("userMention", { env ->
            val user: User = env.getReifiedX("user")
            user.asMention
        }, { env, input ->
            val arg = input[0]
            if (arg.isEmpty()) "null"
            else retrieveUserByArgsN(env.getReifiedX("guild"), arg)?.asMention ?: "null"
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

        Method("effectiveAvatarUrlPart", { env ->
            val user: User = env.getReifiedX("user")
            user.effectiveAvatarUrl
                .remove("https://cdn.discordapp.com/")
                .remove("https://cdn.discord.com/")
        }),
        Method("discriminator", { env ->
            val user: User = env.getReifiedX("user")
            user.discriminator
        }),
        Method("accountCreated", { env ->
            val guild: Guild = env.getReifiedX("guild")
            val user: User = env.getReifiedX("user")
            user.timeCreated.asEpochMillisToDateTime(Container.instance.daoManager, guild.idLong, user.idLong)
        }, { env, args ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = args[0]
            val user: User = getUserByArgsN(
                guild.jda.shardManager ?: return@Method "null", guild, arg
            ) ?: return@Method "null"

            user.timeCreated.asEpochMillisToDateTime(Container.instance.daoManager, guild.idLong, user.idLong)
        }),
        Method("currentDateTime", { env ->
            val guild: Guild = env.getReifiedX("guild")
            val user: User = env.getReifiedX("user")
            System.currentTimeMillis().asEpochMillisToDateTime(Container.instance.daoManager, guild.idLong, user.idLong)
        }, { env, args ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = args[0]
            val user: User = getUserByArgsN(
                guild.jda.shardManager ?: return@Method "null", guild, arg
            ) ?: return@Method "null"

            System.currentTimeMillis().asEpochMillisToDateTime(Container.instance.daoManager, guild.idLong, user.idLong)
        })
    )

    val guildMethods = serverUrlMethods + listOf(
        // {isUser:userId|userTag}
        Method("isUser", { "true" }, { env: Environment, input: Array<String> ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = input[0]

            (retrieveUserByArgsN(guild, arg) != null).toString()
        }),
        // {isMember:userId|userTag}
        Method("isMember", { "true" }, { env: Environment, input: Array<String> ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = input[0]
            (retrieveMemberByArgsN(guild, arg) != null).toString()
        }),
        // {isBot:userId|userTag}
        Method("isBot", { "true" }, { env: Environment, input: Array<String> ->
            val guild: Guild = env.getReifiedX("guild")
            val arg = input[0]
            (retrieveUserByArgsN(guild, arg)?.isBot == true).toString()
        }),
        Method("serverIconUrlPart", { env ->
            val guild: Guild = env.getReifiedX("guild")
            (guild.iconUrl ?: MISSING_IMAGE_URL)
                .remove("https://cdn.discordapp.com/")
                .remove("https://cdn.discord.com/")
        }),
        Method("serverVanityUrl", { env ->
            val guild: Guild = env.getReifiedX("guild")
            guild.vanityUrl ?: "no vanity url"
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
    )

    private val combinedList = guildMethods + userMethods

    fun getMethods(): List<Method> {
        return combinedList
    }
}