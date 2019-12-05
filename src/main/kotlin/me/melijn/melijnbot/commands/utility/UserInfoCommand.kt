package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_USER_ID
import me.melijn.melijnbot.objects.utils.asLongLongGMTString
import me.melijn.melijnbot.objects.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

class UserInfoCommand : AbstractCommand("command.userinfo") {

    init {
        id = 8
        name = "userInfo"
        aliases = arrayOf("user", "memberInfo", "member")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val user: User = if (context.args.isEmpty()) {
            context.author
        } else {
            retrieveUserByArgsNMessage(context, 0) ?: return
        }

        val member: Member? = context.guild.getMember(user)

        val title1 = context.getTranslation("$root.response1.field1.title")
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")

        val unReplacedValue1 = context.getTranslation("$root.response1.field1.value")
        val value1 = replaceUserVar(unReplacedValue1, user, yes, no)

        val eb = Embedder(context)
        eb.setThumbnail(user.effectiveAvatarUrl)
        eb.addField(title1, value1, false)

        if (context.isFromGuild && member != null) {
            val title2 = context.getTranslation("$root.response1.field2.title")
            val unReplacedValue2 = context.getTranslation("$root.response1.field2.value")
            val value2 = replaceMemberVar(unReplacedValue2, member, yes, no)
            eb.addField(title2, value2, false)
        }
        sendEmbed(context, eb.build())
    }

    private fun replaceMemberVar(string: String, member: Member, yes: String, no: String): String = string
        .replace("%nickname%", member.nickname ?: "/")
        .replace("%roleCount%", member.roles.size.toString())
        .replace("%isOwner%", if (member.isOwner) yes else no)
        .replace("%joinTime%", member.timeJoined.asLongLongGMTString())
        .replace("%boostTime%", member.timeBoosted?.asLongLongGMTString() ?: "/")
        .replace("%activities%", member.activities.joinToString { activity -> activity.name })
        .replace("%onlineStatus%", member.onlineStatus.name)
        .replace("%voiceStatus%", getVoiceStatus(member))


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

    private fun replaceUserVar(string: String, user: User, yes: String, no: String): String = string
        .replace("%name%", user.name)
        .replace(PLACEHOLDER_USER_ID, user.id)
        .replace("%discrim%", user.discriminator)
        .replace("%isBot%", if (user.isBot) yes else no)
        .replace("%avatarUrl%", user.effectiveAvatarUrl)
        .replace("%creationTime%", user.timeCreated.asLongLongGMTString())
}