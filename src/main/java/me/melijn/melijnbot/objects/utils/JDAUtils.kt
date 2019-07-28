package me.melijn.melijnbot.objects.utils

import me.melijn.melijnbot.objects.command.CommandContext
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import java.util.regex.Pattern

fun getUserByArgs(context: CommandContext, index: Int): User {
    var user = getUserByArgsN(context, index)
    if (user == null) user = context.getAuthor()
    return user
}


fun getUserByArgsN(context: CommandContext, index: Int): User? {//With null
    var user: User? = null
    if (context.args.size > index) {
        val arg = context.args[index]

        user = if (context.getMessage().mentionedUsers.size > context.offset)
            context.getMessage().mentionedUsers[context.offset]
        else if (arg.matches(Regex("\\d+")) && context.jda.shardManager?.getUserById(arg) != null)
            context.jda.shardManager?.getUserById(arg)
        else if (context.isFromGuild && context.getGuild().getMembersByName(arg, true).size > 0)
            context.getGuild().getMembersByName(arg, true)[0].user
        else if (context.isFromGuild && context.getGuild().getMembersByNickname(arg, true).size > 0)
            context.getGuild().getMembersByNickname(arg, true)[0].user
        else user
    }
    return user
}

fun getRoleByArgsN(context: CommandContext, index: Int): Role? {
    var role: Role? = null
    if (context.args.size > index) {
        val arg = context.args[index]

        role = if (arg.matches(Regex("\\d+")) && context.jda.shardManager?.getRoleById(arg) != null)
            context.jda.shardManager?.getRoleById(arg)
        else if (context.isFromGuild && context.getGuild().getRolesByName(arg, true).size > 0)
            context.getGuild().getRolesByName(arg, true)[0]
        else if (arg.matches(Regex("<@&\\d+>"))) {
            var role2: Role? = null
            val pattern = Pattern.compile("<@&(\\d+)>")
            val matcher = pattern.matcher(arg)
            while (matcher.find()) {
                val id = matcher.group(1)
                val role3 = context.jda.shardManager?.getRoleById(id)
                if (role2 != null && role3 == null) continue
                role2 = role3
            }
            role2
        } else role
    }
    return role
}