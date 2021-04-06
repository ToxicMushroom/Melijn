package me.melijn.melijnbot.internals.arguments.parser

import me.melijn.melijnbot.internals.arguments.CommandArgParser
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.DISCORD_ID
import me.melijn.melijnbot.internals.utils.ROLE_MENTION
import net.dv8tion.jda.api.entities.Role

class RoleArgParser : CommandArgParser<Role>() {

    override suspend fun parse(context: ICommandContext, arg: String): Role? {
        var role: Role? = null

        if (!context.isFromGuild) return role

        role = if (DISCORD_ID.matches(arg) && context.jda.shardManager?.getRoleById(arg) != null) {
            context.shardManager.getRoleById(arg)

        } else if (context.isFromGuild && context.guild.getRolesByName(arg, true).size > 0) {
            context.guild.getRolesByName(arg, true)[0]

        } else if (ROLE_MENTION.matches(arg)) {
            val id = (ROLE_MENTION.find(arg) ?: return null).groupValues[1]
            context.message.mentionedRoles.firstOrNull { it.id == id } ?: context.shardManager.getRoleById(id)

        } else {
            if (arg == "everyone") context.guild.publicRole
            else role
        }

        if (!context.guild.roles.contains(role)) return null
        return role
    }
}