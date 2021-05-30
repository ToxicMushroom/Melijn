package me.melijn.melijnbot.database.statesync

import me.melijn.melijnbot.database.CacheDao
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.database.DriverManager
import me.melijn.melijnbot.internals.command.ICommandContext
import net.dv8tion.jda.api.entities.Emote
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.data.DataArray

class EmoteCacheDao(driverManager: DriverManager) : CacheDao(driverManager) {

    override val cacheName: String = "EMOTE"

    fun save(emote: Emote) {
        val darr = DataArray.empty()
        darr.add(emote.name)
        darr.add(if (emote.isAnimated) 1 else 0)
        darr.add(if (emote.isManaged) 1 else 0)
        darr.add(if (emote.isAvailable) 1 else 0)
        setCacheEntry(emote.id, darr.toString(), 10)
    }

    fun remove(emoteId: Long) {
        removeCacheEntry(emoteId)
    }

    suspend fun getEmote(emoteId: Long): LiteEmote? {
        return getCacheEntry(emoteId)?.let {
            val darr = DataArray.fromJson(it)
            LiteEmote(
                emoteId,
                darr.getString(0),
                darr.getInt(1) == 1,
                darr.getInt(2) == 1,
                darr.getInt(3) == 1,
            )
        }
    }

}

class LiteEmote(
    val id: Long,
    val name: String,
    val isAnimated: Boolean,
    val isManaged: Boolean,
    val isAvailable: Boolean
) {
    val asMention: String
        get() = (if (isAnimated) "<a:" else "<:") + name + ":" + id + ">"

}

fun Emote.toLite(): LiteEmote = LiteEmote(idLong, name, isAnimated, isManaged, isAvailable)

suspend fun getEmote(context: ICommandContext, emoteId: Long): LiteEmote? {
    return getEmote(context.shardManager, context.daoManager, emoteId)
}

suspend fun getEmote(shardManager: ShardManager, daoManager: DaoManager, emoteId: Long): LiteEmote? {
    return shardManager.getEmoteById(emoteId)?.toLite() ?: daoManager.emoteCache.getEmote(
        emoteId
    )
}

