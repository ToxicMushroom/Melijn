package me.melijn.melijnbot.objects.embed

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.command.CommandContext
import net.dv8tion.jda.api.EmbedBuilder

class Embedder(daoManager: DaoManager, guildId: Long, userId: Long, embedColor: Int) : EmbedBuilder() {

    constructor(context: CommandContext) : this(context.daoManager, context.guildId, context.authorId, context.embedColor)

    init {
        val embedColorWrapper = daoManager.embedColorWrapper
        val userEmbedColorWrapper = daoManager.userEmbedColorWrapper
        var color: Int = embedColorWrapper.embedColorCache.get(guildId).get() //Error
        if (daoManager.supporterWrapper.userSupporterIds.contains(userId)) {
            color = userEmbedColorWrapper.userEmbedColorCache.get(userId).get()
        }

        if (color == 0) color = embedColor
        setColor(color)
    }
}