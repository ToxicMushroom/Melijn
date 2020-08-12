package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandContext

class RedisCommand : AbstractCommand("command.redis") {

    init {
        id = 201
        name = "redis"
    }

    override suspend fun execute(context: CommandContext) {
        when (context.args[0]) {
            "flush" -> {
                context.daoManager.driverManager.redisConnection.async()
                    .flushall()
            }
            "read" -> {
                context.daoManager.driverManager.redisConnection.async()
                    .get(context.getRawArgPart(1))
                    .await()
            }
            "write" -> {
                context.daoManager.driverManager.redisConnection.async()
                    .set(context.args[1], context.getRawArgPart(2))
                    .await()
            }
        }
    }

}