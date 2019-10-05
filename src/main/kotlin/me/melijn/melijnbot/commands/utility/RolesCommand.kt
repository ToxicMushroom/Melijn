package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.translation.i18n
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

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty() && !context.isFromGuild) {
            sendSyntax(context, syntax)
            return
        }

        val guild: Guild = if (context.args.isNotEmpty() && context.args[0].matches("\\d+".toRegex())) {
            context.jda.shardManager?.getGuildById(context.args[0]) ?: context.getGuild()
        } else {
            context.getGuild()
        }

        val language = context.getLanguage()
        val unReplacedTitle = i18n.getTranslation(language, "$root.response1.title")
        val title = unReplacedTitle
            .replace("%guildName%", guild.name)


        var content = "```INI"
        for ((index, role) in guild.roleCache.withIndex()) {
            content += "\n${index+1} - [${role.name}] - ${role.id}"
        }
        content += "```"

        val msg = title + content

        sendMsgCodeBlock(context, msg, "INI")
    }
}