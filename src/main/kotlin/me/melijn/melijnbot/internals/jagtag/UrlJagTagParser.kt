package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Method
import com.jagrosh.jagtag.Parser
import com.jagrosh.jagtag.ParserBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.util.function.Supplier

val URL_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    val methods = (PunishmentMethods.getMethods() +
        SingleTargetPunishmentMethods.getMethods()).filter {
        it.name.contains("AvatarUrl")
    }.map {
        Method(it.name, { env ->
            env.getReifiedX<User>("user").effectiveAvatarUrl
        })
    }
    ParserBuilder()
        .addMethods(DiscordMethods.imgUrlMethods)
        .addMethods(methods)
        .build()
}

object UrlJagTagParser {
    suspend fun parseJagTag(guild: Guild, user: User, input: String): String =
        parseJagTag(UrlParserArgs(guild, user), input)

    suspend fun parseJagTag(args: UrlParserArgs, input: String): String {
        val parser = URL_PARSER_SUPPLIER.get()
            .put("user", args.user)
            .put("guild", args.guild)

        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class UrlParserArgs(
    val guild: Guild,
    val user: User
)