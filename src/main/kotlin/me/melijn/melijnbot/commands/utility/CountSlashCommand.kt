package me.melijn.melijnbot.commands.utility

import dev.minn.jda.ktx.await
import me.melijn.melijnbot.internals.command.SlashCommandContext
import me.melijn.melijnbot.internals.utils.asTag

object CountCommand {

    suspend fun execute(ctx: SlashCommandContext, number: Double) {
        val member = ctx.event.member ?: return

        ctx.event
            .reply(ctx.translate("counting_msg", number, member.asTag))
            .setEphemeral(true).await()
    }
}