package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Environment
import com.jagrosh.jagtag.Method
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.getDurationString
import net.dv8tion.jda.api.entities.User

object PunishmentMethods {

    fun getUserMethods(baseName: String, userFetch: (env: Environment) -> User?): List<Method> {
        return listOf(
            Method("${baseName}Tag", { env -> userFetch(env)?.asTag.toString() }),
            Method("${baseName}Id", { env -> userFetch(env)?.id.toString() }),
            Method("${baseName}Mention", { env -> userFetch(env)?.asMention.toString() }),
            Method("${baseName}AvatarUrl", { env -> userFetch(env)?.effectiveAvatarUrl.toString() }),
            Method("${baseName}CreationDate", { env -> userFetch(env)?.timeCreated.toString() }),
        )
    }

    fun getMethods(): List<Method> =
        DiscordMethods.guildMethods +
        getUserMethods("punishAuthor") { env -> getArgs(env).punishAuthor } +
            getUserMethods("unPunishAuthor") { env -> getArgs(env).unPunishAuthor } +
            listOf(
                Method("punishId", { env -> getArgs(env).punishId }),
                Method("reason", { env -> getArgs(env).punishReason }),
                Method("unPunishReason", { env -> getArgs(env).unPunishReason.toString() }),
                Method("moment", { env -> getArgs(env).start.toString() }),
                Method("start", { env -> getArgs(env).start.toString() }),
                Method("end", { env -> getArgs(env).end.toString() }),
                Method("duration", { env -> getArgs(env).end?.let { end -> end - getArgs(env).start }?.toString() ?: "-1" }),
                Method("startTime", complex = { env, input ->
                    val args = getArgs(env)
                    args.start.asEpochMillisToDateTime(args.daoManager, args.guild.idLong, input[0].toLongOrNull())
                }),
                Method("endTime", complex = { env, input ->
                    val args = getArgs(env)
                    args.end?.asEpochMillisToDateTime(args.daoManager, args.guild.idLong, input[0].toLongOrNull()) ?: "never"
                }),
                Method("timeDuration", { env ->
                    val args = getArgs(env)
                    args.end?.let { end -> getDurationString(end - args.start) } ?: "forever"
                }),
                Method("titleSpaces", complex = { _, input ->
                    val amount = 0.coerceAtLeast((45L - input[0].codePoints().count()).toInt())
                    " ".repeat(amount)
                }),
                Method("extraLcInfo", { env -> getArgs(env).extra })
            )

    fun getArgs(env: Environment) = env.getReifiedX<PunishJagTagParserArgs>("punishArgs")

}