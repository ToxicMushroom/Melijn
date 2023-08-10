package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class CacheInfoCommand : AbstractCommand("command.cacheinfo") {

    init {
        name = "cacheInfo"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: ICommandContext) {

        sendRsp(context, """
            PrivChanCacheSize: ${context.shardManager.privateChannelCache.size()}
            TextChanCacheSize: ${context.shardManager.textChannelCache.size()}
            StorChanCacheSize: ${context.shardManager.storeChannelCache.size()}
            VoisChanCacheSize: ${context.shardManager.voiceChannelCache.size()}
            CategoryCacheSize: ${context.shardManager.categoryCache.size()}
            ShardCacheSize: ${context.shardManager.shardCache.size()}
            EmoteCacheSize: ${context.shardManager.emoteCache.size()}
            GuildCacheSize: ${context.shardManager.guildCache.size()}
            RoleCacheSize: ${context.shardManager.roleCache.size()}
            UserCacheSize: ${context.shardManager.userCache.size()}
        """.trimIndent())
    }
}