package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.getUserByArgs
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class UserInfoCommand : AbstractCommand("command.userinfo") {

    init {
        id = 8
        name = "userInfo"
        aliases = arrayOf("user", "memberInfo", "member")
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        val user: User = getUserByArgs(context, 0)
        val member: Member? = context.getGuild().getMember(user)

        val title1 = Translateable("$root.response1.field1.title").string(context)
        val value1 = replaceUserVar(Translateable("$root.response1.field1.value").string(context), user)

        val eb = Embedder(context)
        eb.setThumbnail(user.effectiveAvatarUrl)
        eb.addField(title1, value1, false)

        if (context.isFromGuild && member != null) {
            val title2 = Translateable("$root.response1.field2.title").string(context)
            val value2 = replaceMemberVar(Translateable("$root.response1.field2.value").string(context), member)
            eb.addField(title2, value2, false)
        }
        sendEmbed(context, eb.build())
    }

    private fun replaceMemberVar(string: String, member: Member): String {
        return string
                .replace("%nickname%", member.nickname ?: "/")
                .replace("%roleCount%", member.roles.size.toString())
                .replace("%isOwner%", if (member.isOwner) "Yes" else "No")
                .replace("%joinTime%", member.timeJoined.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG).withZone(ZoneId.of("GMT"))))
                .replace("%boostTime%", member.timeBoosted?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG).withZone(ZoneId.of("GMT")))
                        ?: "/")
                .replace("%activities%", member.activities.joinToString { activity -> activity.name })
                .replace("%onlineStatus%", member.onlineStatus.name)
                .replace("%voiceStatus%", getVoiceStatus(member))

    }

    private fun getVoiceStatus(member: Member): String {
        if (member.voiceState == null) return "disconnected"
        if (member.voiceState?.channel == null) return "disconnected"
        val sb = ArrayList<String>()
        if (member.voiceState?.isSuppressed == true) sb.add("suppressed")

        if (member.voiceState?.isGuildDeafened == true)
            sb.add("forced deafened")
        else if (member.voiceState?.isSelfDeafened == true) sb.add("deafened")

        if (member.voiceState?.isGuildMuted == true)
            sb.add("forced muted")
        else if (member.voiceState?.isSelfMuted == true) sb.add("muted")

        return "${sb.joinToString()} in ${member.voiceState?.channel?.name}"

    }

    private fun replaceUserVar(string: String, user: User): String {
        return string
                .replace("%name%", user.name)
                .replace("%discrim%", user.discriminator)
                .replace("%isBot%", if (user.isBot) "Yes" else "No")
                .replace("%avatarUrl%", user.effectiveAvatarUrl)
                .replace("%creationTime%", user.timeCreated.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.LONG).withZone(ZoneId.of("GMT"))))
    }
}