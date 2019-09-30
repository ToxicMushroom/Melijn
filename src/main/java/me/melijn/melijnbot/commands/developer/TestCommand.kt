package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.events.eventlisteners.MessageReceivedListener.Companion.coolString

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override suspend fun execute(context: CommandContext) {
//        val msgParts = StringUtils.splitMessage(SupportedDiscordEmoji.helpMe.joinToString(" "), 400, 1500)
//        for (part in msgParts) {
//            sendMsg(context, part)
//        }
        println(coolString)
    }
}