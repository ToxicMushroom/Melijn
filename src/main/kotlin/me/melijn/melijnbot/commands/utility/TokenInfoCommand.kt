package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.iharder.Base64
import java.math.BigInteger

class TokenInfoCommand : AbstractCommand("command.tokeninfo") {

    init {
        id = 212
        name = "tokenInfo"
        aliases = arrayOf("ti", "token")
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
        val userId = String(Base64.decode(split[0])).toLongOrNull() ?: return
        val bot = context.shardManager.retrieveUserById(userId).awaitOrNull()
        val botCreated = (userId shr 22) + 1420070400000
        val tokenCreated = BigInteger(Base64.decode(split[1])).toLong() * 1000 + 1_592_707_552_616
        val hmac = (split[2])

        val eb = Embedder(context)
            .setDescription("**BotID:** ${userId}\n" +
                "**Bot Creation Time:** ${(botCreated).asEpochMillisToDateTime(context.getTimeZoneId())} | $botCreated\n" +
                "**Token Creation Time:** ${(tokenCreated).asEpochMillisToDateTime(context.getTimeZoneId())} | $tokenCreated\n" +
                "**Hmac:** $hmac")
        if (bot != null) {
            eb.setAuthor(bot.asTag, null, bot.effectiveAvatarUrl)
        } else {
            eb.setTitle("Token Info of unknown bot")
        }

        sendEmbedRsp(context, eb.build())
    }
}
