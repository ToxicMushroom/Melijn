package me.melijn.melijnbot.objects.jagtag.parsers

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.melijn.melijnbot.database.giveaway.Giveaway
import me.melijn.melijnbot.internals.jagtag.DiscordMethods
import me.melijn.melijnbot.objects.jagtag.methods.GiveawayMethods
import net.dv8tion.jda.api.entities.Member
import java.util.function.Supplier


val GIVEAWAY_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
        .addMethods(DiscordMethods.getMethods())
        .addMethods(GiveawayMethods.getMethods())
        .build()
}

object GiveawayJagTagParser {
    suspend fun parseCCJagTag(args: GiveawayJagTagParserArgs, input: String): String {
        val parser = GIVEAWAY_PARSER_SUPPLIER.get()
            .put("user", args.member.user)
            .put("member", args.member)
            .put("guild", args.member.guild)
            .put("args", args.rawArg.split("\\s+".toRegex()))
            .put("rawArg", args.rawArg)
            .put("giveaway", args.giveaway)

        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class GiveawayJagTagParserArgs(
    val member: Member,
    val rawArg: String,
    val giveaway: Giveaway
)