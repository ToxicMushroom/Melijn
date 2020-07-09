package me.melijn.melijnbot.internals.embed

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.CommandContext
import net.dv8tion.jda.api.EmbedBuilder

class Embedder(daoManager: DaoManager, guildId: Long, userId: Long, embedColor: Int) : EmbedBuilder() {

    constructor(context: CommandContext) : this(context.daoManager, if (context.isFromGuild) context.guildId else -1, context.authorId, context.embedColor)

    init {
        val embedColorWrapper = daoManager.embedColorWrapper
        val userEmbedColorWrapper = daoManager.userEmbedColorWrapper
        val guildColor: Int = embedColorWrapper.embedColorCache.get(guildId).get()
        val userColor = userEmbedColorWrapper.userEmbedColorCache.get(userId).get()

        val color = when {
            userColor != 0 -> userColor
            guildColor != 0 -> guildColor
            else -> embedColor
        }

        setColor(color)
    }
}