package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import net.dv8tion.jda.api.Permission

class AppendReverseGifCommand : AbstractCommand("command.appendreversegif") {

    init {
        id = 153
        name = "appendReverseGif"
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        runConditions = arrayOf(RunCondition.VOTED)
        commandCategory = CommandCategory.IMAGE
    }

    override suspend fun execute(context: CommandContext) {
        ImageCommandUtil.executeGifFrameAppend(context)
    }
}