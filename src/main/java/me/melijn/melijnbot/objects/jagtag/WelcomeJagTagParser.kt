package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import net.dv8tion.jda.api.entities.Member
import java.util.function.Supplier


val PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
            .addMethods(DiscordMethods.getMethods())
            .build()
}

object WelcomeJagTagParser {
    suspend fun parseJagTag(member: Member, input: String): String = parseJagTag(WelcomeParserArgs(member), input)

    suspend fun parseJagTag(args: WelcomeParserArgs, input: String): String {
        val parser = PARSER_SUPPLIER.get()
                .put("user", args.member.user)
                .put("member", args.member)
                .put("guild", args.member.guild)
        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class WelcomeParserArgs(
        val member: Member
)