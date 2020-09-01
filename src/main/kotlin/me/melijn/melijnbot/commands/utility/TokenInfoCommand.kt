package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.iharder.Base64
import java.math.BigInteger

class TokenInfoCommand : AbstractCommand("command.tokeninfo") {

    init {
        id = 212
        name = "tokenInfo"
        aliases = arrayOf("Ti")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val split = context.args[0].split(".")
        if (split.size != 3) {
            sendSyntax(context)
            return
        }
        val userId = String(Base64.decode(split[0]))
        val created = BigInteger(Base64.decode(split[1])).toLong()
        val hmac = (split[2])

        val eb = Embedder(context)
            .setTitle("Token Info")
            .setDescription("**UserID** ${userId}\n" +
                "**Creation Time** ${((created * 1000 + 1293840000) * 1000).asEpochMillisToDateTime(context.getTimeZoneId())}\n" +
                "**Hmac** ${hmac}")
        sendEmbedRsp(context, eb.build())
    }
}
