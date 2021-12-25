package me.melijn.melijnbot.commands.utility

import dev.minn.jda.ktx.await
import me.melijn.melijnbot.internals.command.SlashCommandContext

class CountCommand {

    suspend fun execute(ctx: SlashCommandContext) {
        val member = ctx.event.member ?: return
        val memberId = member.idLong

        ctx.event.reply(ctx.translate("command.alpaca.syntax", 12)).setEphemeral(true).await()
    }
}