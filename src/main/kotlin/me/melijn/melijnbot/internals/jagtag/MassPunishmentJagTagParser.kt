package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.JagTag
import com.jagrosh.jagtag.Parser
import me.melijn.melijnbot.database.DaoManager
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.time.ZoneId
import java.util.function.Supplier

val MASS_PUNISH_PARSER_SUPPLIER: Supplier<Parser> = Supplier {
    JagTag().newDefaultBuilder()
        .addMethods(DiscordMethods.getMethods())
        .addMethods(PunishmentMethods.getMethods())
        .addMethods(MassTargetPunishmentMethods.getMethods())
        .build()
}

object MassPunishmentJagTagParser {

    suspend fun parseJagTag(args: MassPunishJagTagParserArgs, input: String): String {
        val parser = MASS_PUNISH_PARSER_SUPPLIER.get()
            .put("punishArgs", PunishJagTagParserArgs(args.punishAuthor, null, args.unPunishAuthor, args.daoManager, args.punishReason, args.unPunishReason, args.start, args.end, args.punishId, "null", args.zoneId, args.guild))
            .put("massPunishArgs", args)
            .put("guild", args.guild)
        val parsed = parser.parse(input)
        parser.clear()
        return parsed
    }
}

data class MassPunishJagTagParserArgs(
    val punishAuthor: User?,
    val punished: Set<User>,
    val unPunishAuthor: User?,

    val daoManager: DaoManager,

    val punishReason: String,
    val unPunishReason: String?,

    val start: Long,
    val end: Long?,
    val punishId: String,
    val zoneId: ZoneId,
    val guild: Guild
)