package me.melijn.melijnbot.commands.image

import me.melijn.melijnbot.commandutil.image.ImageCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import net.dv8tion.jda.api.Permission

class RerenderGifCommand : AbstractCommand("command.rerendergif") {

    init {
        id = 151
        name = "rerenderGif"
        aliases = arrayOf("rerender")
        discordChannelPermissions = arrayOf(Permission.MESSAGE_ATTACH_FILES)
        commandCategory = CommandCategory.DEVELOPER
    }

    suspend fun execute(context: ICommandContext) {
        executeGif(context)
    }

    private suspend fun executeGif(context: ICommandContext) {
        ImageCommandUtil.executeGifRecolorSingleOffset(context, { ints ->
            if (ints[0] == 255 && ints[1] == 255 && ints[2] == 255) {
                intArrayOf(254, 254, 254, 255)
            } else if (ints[3] < 128) {
                intArrayOf(255, 255, 255, 255)
            } else ints
        })
    }
}