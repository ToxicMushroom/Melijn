package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class ClearCacheCommand : AbstractCommand("command.clearcache") {

    init {
        id = 216
        name = "clearCache"
        commandCategory = CommandCategory.DEVELOPER
    }

    suspend fun execute(context: ICommandContext) {
        context.daoManager.driverManager.redisConnection?.async()
            ?.flushdb()

        sendRsp(context, "Cleared cache")
    }
}