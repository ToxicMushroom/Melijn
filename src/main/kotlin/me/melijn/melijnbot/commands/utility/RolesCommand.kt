package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.arguments.annotations.CommandArg
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.escapeCodeblockMarkdown
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withSafeVariable
import net.dv8tion.jda.api.entities.Guild

class RolesCommand : AbstractCommand("command.roles") {

    init {
        id = 10
        name = "roles"
        aliases = arrayOf("roleList", "listRoles")
        commandCategory = CommandCategory.UTILITY
    }

    suspend fun execute(
        context: ICommandContext,
        @CommandArg(index = 0, optional = true) guildN: Guild?
    ) {
        if (context.args.isEmpty() && !context.isFromGuild) {
            sendSyntax(context)
            return
        }

        val guild: Guild = guildN ?: context.guild

        val title = context.getTranslation("$root.response1.title")
            .withSafeVariable("serverName", guild.name)

        var content = "```INI"
        for ((index, role) in guild.roleCache.withIndex()) {
            content += "\n${index + 1} - [${role.name.escapeCodeblockMarkdown(true)}] - ${role.id}"
        }
        content += "```"

        val msg = title + content
        sendRspCodeBlock(context, msg, "INI", true)
    }
}