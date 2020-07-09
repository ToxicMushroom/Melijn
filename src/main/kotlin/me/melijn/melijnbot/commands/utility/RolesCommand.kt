package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.message.sendRspCodeBlock
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Guild

class RolesCommand : AbstractCommand("command.roles") {

    init {
        id = 10
        name = "roles"
        aliases = arrayOf("roleList", "listRoles")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty() && !context.isFromGuild) {
            sendSyntax(context)
            return
        }

        val guild: Guild = if (context.args.isNotEmpty() && context.args[0].matches(DISCORD_ID.toRegex())) {
            context.jda.shardManager?.getGuildById(context.args[0]) ?: context.guild
        } else {
            context.guild
        }

        val unReplacedTitle = context.getTranslation("$root.response1.title")
        val title = unReplacedTitle
            .withVariable("serverName", guild.name)


        var content = "```INI"
        for ((index, role) in guild.roleCache.withIndex()) {
            content += "\n${index + 1} - [${role.name}] - ${role.id}"
        }
        content += "```"

        val msg = title + content
        sendRspCodeBlock(context, msg, "INI", true)
    }
}