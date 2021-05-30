package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.melijn.melijnbot.internals.utils.awaitOrNull
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.util.function.Supplier

val WELCOME_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
        .addMethods(DiscordMethods.getMethods())
        .build()
}

object WelcomeJagTagParser {
    suspend fun parseJagTag(guild: Guild, user: User, input: String): String =
        parseJagTag(WelcomeParserArgs(guild, user), input)

    suspend fun parseJagTag(args: WelcomeParserArgs, input: String): String {
        val parser = WELCOME_PARSER_SUPPLIER.get()
            .put("user", args.user)
            .put("guild", args.guild)

        args.guild.retrieveMember(args.user).awaitOrNull()?.let {
            parser.put("member", it)
        }

        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class WelcomeParserArgs(
    val guild: Guild,
    val user: User
)