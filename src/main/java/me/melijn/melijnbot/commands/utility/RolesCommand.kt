package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendMsgCodeBlock
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.Guild

class RolesCommand : AbstractCommand("command.roles") {

    init {
        id = 10
        name = "roles"
        aliases = arrayOf("roleList", "listRoles")
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty() && !context.isFromGuild) {
            sendSyntax(context, syntax)
            return
        }

        val guild: Guild = if (context.args.isNotEmpty())
            (context.jda.shardManager?.getGuildById(context.args[0]) ?: context.getGuild())
        else context.getGuild()

        val title = Translateable("$root.response1.title").string(context)
                .replace("%guildName%", guild.name)
        var msg = "$title```INI"
        var count = 1
        for (role in guild.roleCache) {
            msg += "\n${count++} - [${role.name}] - ${role.id}"
        }
        msg += "```"

        sendMsgCodeBlock(context, msg, "INI")

    }
}