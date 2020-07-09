package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.melijn.melijnbot.database.command.CustomCommand
import me.melijn.melijnbot.internals.utils.SPACE_PATTERN
import net.dv8tion.jda.api.entities.Member
import java.util.function.Supplier

val CC_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
        .addMethods(DiscordMethods.getMethods())
        .addMethods(CCMethods.getMethods())
        .build()
}

object CCJagTagParser {
    suspend fun parseCCJagTag(args: CCJagTagParserArgs, input: String): String {
        val parser = CC_PARSER_SUPPLIER.get()
            .put("user", args.member.user)
            .put("member", args.member)
            .put("guild", args.member.guild)
            .put("args", args.rawArg.split(SPACE_PATTERN))
            .put("rawArg", args.rawArg)
            .put("cc", args.cc)
        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class CCJagTagParserArgs(
    val member: Member,
    val rawArg: String,
    val cc: CustomCommand
)