package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.melijn.melijnbot.database.DaoManager
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.time.ZoneId
import java.util.function.Supplier

val PUNISH_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
        .addMethods(DiscordMethods.getMethods())
        .addMethods(PunishmentMethods.getMethods())
        .addMethods(SingleTargetPunishmentMethods.getMethods())
        .build()
}

object PunishmentJagTagParser {

    suspend fun parseJagTag(args: PunishJagTagParserArgs, input: String): String {
        var parser = PUNISH_PARSER_SUPPLIER.get()
            .put("punishArgs", args)
            .put("guild", args.guild)
        args.punished?.let {
            parser = parser.put("user", it)
        }

        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class PunishJagTagParserArgs(
    val punishAuthor: User?,
    val punished: User?,
    val unPunishAuthor: User?,
    val daoManager: DaoManager,

    val punishReason: String,
    val unPunishReason: String?,

    val start: Long,
    val end: Long?,
    val punishId: String,
    val extra: String,
    val zoneId: ZoneId,
    val guild: Guild
)