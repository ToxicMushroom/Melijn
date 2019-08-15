package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.enums.LogChannelType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.sendMsg

class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    override fun execute(context: CommandContext) {
        val timeStamp1 = System.currentTimeMillis()
        val future = context.daoManager.logChannelWrapper.logChannelCache.get(Pair(context.getGuildId(), LogChannelType.PERMANENT_BAN))
        val timeStamp2 = System.currentTimeMillis()
        val channel = future.get()
        val timeStamp3 = System.currentTimeMillis()
        val channel2 = future.get()
        val timeStamp4 = System.currentTimeMillis()
        Integer.parseInt("sas")
        sendMsg(context, "${timeStamp2 - timeStamp1} , ${timeStamp3 - timeStamp2} , ${timeStamp4 - timeStamp3} , $channel, $channel2")
    }
}