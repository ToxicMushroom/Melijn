package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendRsp

class RedisCommand : AbstractCommand("command.redis") {

    init {
        id = 201
        name = "redis"
    }

    override suspend fun execute(context: ICommandContext) {
        val commands = context.daoManager.driverManager.redisConnection?.async()
        if (commands == null) {
            sendRsp(context, "Redis not initizialized")
            return
        }
        when (context.args[0]) {
            "flush" -> {
                commands
                    .flushall()
            }
            "read" -> {
                commands
                    .get(context.getRawArgPart(1))
                    .await()
            }
            "write" -> {
                commands
                    .set(context.args[1], context.getRawArgPart(2))
                    .await()
            }
        }
    }

}