package me.melijn.melijnbot.commands.developer

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.events.eventlisteners.MessageDeletedListener
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.asTag
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.message.sendRsp
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    override suspend fun execute(context: CommandContext) {
        val timeZone = context.getTimeZoneId()
        val msg = MessageDeletedListener.recentDeletions[Pair(context.guildId, context.channelId)]?.entries?.joinToString {
            "[" + it.value.asEpochMillisToDateTime(timeZone) + "] " +
                    runBlocking { context.guild.retrieveMemberById(it.key.authorId).awaitOrNull()?.asTag ?: it.key.authorId } + ": " +
                    it.key.content
        } ?: "No messages to snipe!"
        sendRsp(context, msg)
        throw IllegalStateException()
    }
}