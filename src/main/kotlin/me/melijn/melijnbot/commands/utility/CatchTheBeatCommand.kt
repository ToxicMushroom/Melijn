package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.web.apis.OsuMode

class CatchTheBeatCommand : AbstractCommand("command.catchthebeat") {

    init {
        name = "catchTheBeat"
        aliases = arrayOf("ctb","osu!catchTheBeat", "o!ctb")
        val mode = OsuMode.CATCH_THE_BEAT
        children = arrayOf(
            OsuCommand.UserArg(root, mode),
            OsuCommand.TopArg(root, mode),
            OsuCommand.RecentArg(root, mode),
            OsuCommand.SetUserArg(root)
        )
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}