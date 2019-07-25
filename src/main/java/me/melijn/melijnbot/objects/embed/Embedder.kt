package me.melijn.melijnbot.objects.embed

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import net.dv8tion.jda.api.EmbedBuilder

class Embedder(daoManager: DaoManager, guildId: Long, userId: Long) : EmbedBuilder() {

    init {
        val embedColorWrapper = daoManager.embedColorWrapper
        val userEmbedColorWrapper = daoManager.userEmbedColorWrapper
    }

    constructor(context: CommandContext) : this(context.daoManager, context.guildId, context.authorId) {

    }
}