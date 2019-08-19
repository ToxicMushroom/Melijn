package me.melijn.melijnbot.commands.moderation

import me.melijn.melijnbot.database.ban.Ban
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.User
import java.awt.Color

class UnbanCommand : AbstractCommand("command.unban"){

    init {
        id = 25
        name = "unban"
        aliases = arrayOf("pardon")
        commandCategory = CommandCategory.MODERATION
        discordPermissions = arrayOf(Permission.BAN_MEMBERS)
    }

    override fun execute(context: CommandContext) {

    }
}
fun getUnbanMessage(guild: Guild, bannedUser: User, banAuthor: User?, unbanAuthor: User, ban: Ban): MessageEmbed  {
    val eb = EmbedBuilder()
    eb.setAuthor("Unbanned by: " + unbanAuthor.asTag + " ".repeat(80).substring(0, 80 - unbanAuthor.name.length) + "\u200B", null, unbanAuthor.effectiveAvatarUrl)
    eb.setDescription("```LDIF" +
            "\nGuild: " + guild.name +
            "\nGuildId: " + guild.id +
            "\nBan Author: " + (banAuthor?.asTag ?: "deleted account") +
            "\nBan Author Id: " + ban.banAuthorId +
            "\nUnbanned: " + bannedUser.asTag +
            "\nUnbannedId: " + bannedUser.id +
            "\nBan Reason: " + ban.reason +
            "\nUnban Reason: " + ban.unbanReason +
            "\nStart of ban: " + (ban.startTime.asEpochMillisToDateTime()) +
            "\nEnd of ban: " + (ban.endTime?.asEpochMillisToDateTime() ?: "error") + "```")
    eb.setThumbnail(bannedUser.effectiveAvatarUrl)
    eb.setColor(Color.green)
    return eb.build()
}