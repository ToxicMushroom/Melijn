package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class ClearCacheCommand : AbstractCommand("command.clearcache") {

    init {
        id = 216
        name = "clearCache"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
        context.daoManager.driverManager.redisConnection?.async()
            ?.flushdb()

        sendRsp(context, "Cleared cache")
    }
}