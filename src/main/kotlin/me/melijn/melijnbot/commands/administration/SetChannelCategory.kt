package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.Permission

class SetChannelCategory : AbstractCommand("command.setchannelcategory") {

    init {
        name = "setChannelCategory"
        aliases = arrayOf("scc")
        discordPermissions = arrayOf(Permission.MANAGE_CHANNEL)
        commandCategory = CommandCategory.ADMINISTRATION
    }

    suspend fun execute(context: ICommandContext) {
        if (argSizeCheckFailed(context, 1)) return
        val channel = getChannelByArgsNMessage(context, 0, true) ?: return
        val category = getCategoryByArgsNMessage(context, 1, true) ?: return
        if (notEnoughPermissionsAndMessage(context, channel, Permission.VIEW_CHANNEL)) return

        channel.manager
            .setParent(category)
            .await()

        sendRsp(
            context, "Moved **%channel%** to the **%category%** category"
                .withSafeVariable("channel", channel.asTag)
                .withSafeVariable("category", category.name)
        )
    }
}