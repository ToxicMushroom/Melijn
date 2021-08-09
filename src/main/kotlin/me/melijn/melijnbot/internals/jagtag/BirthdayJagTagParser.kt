package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.melijn.melijnbot.database.DaoManager
import net.dv8tion.jda.api.entities.Member
import java.util.function.Supplier

val BIRTHDAY_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag()
        .newDefaultBuilder()
        .addMethods(DiscordMethods.getMethods())
        .addMethods(BirthdayMethods.getMethods())
        .build()
}

object BirthdayJagTagParser {
    suspend fun parseJagTag(args: BirthdayParserArgs, input: String): String {
        val parser = BIRTHDAY_PARSER_SUPPLIER.get()
            .put("user", args.member.user)
            .put("member", args.member)
            .put("daoManager", args.daoManager)
            .put("guild", args.member.guild)
            .put("birthYear", args.birthYear ?: 0)
        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class BirthdayParserArgs(
    val daoManager: DaoManager,
    val member: Member,
    val birthYear: Int?
)