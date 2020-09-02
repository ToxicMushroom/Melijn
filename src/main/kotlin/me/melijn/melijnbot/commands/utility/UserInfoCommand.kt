package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER_ID
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

class UserInfoCommand : AbstractCommand("command.userinfo") {

    init {
        id = 8
        name = "userInfo"
        aliases = arrayOf("ui", "user", "memberInfo", "member", "mi")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val user: User = if (context.args.isEmpty()) {
            context.author
        } else {
            retrieveUserByArgsNMessage(context, 0) ?: return
        }

        val member: Member? = if (context.isFromGuild) context.guild.retrieveMember(user).awaitOrNull() else null
        val isSupporter = context.daoManager.supporterWrapper.getUsers().contains(user.idLong)

        val title1 = context.getTranslation("$root.response1.field1.title")
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")

        val unReplacedValue1 = context.getTranslation("$root.response1.field1.value")
        val value1 = replaceUserVar(unReplacedValue1, user, isSupporter, yes, no)

        val eb = Embedder(context)
            .setThumbnail(user.effectiveAvatarUrl)
            .addField(title1, value1, false)

        if (context.isFromGuild && member != null) {
            val title2 = context.getTranslation("$root.response1.field2.title")
            val unReplacedValue2 = context.getTranslation("$root.response1.field2.value")
            val value2 = replaceMemberVar(unReplacedValue2, member, yes, no)
            eb.addField(title2, value2, false)
        }

        sendEmbedRsp(context, eb.build())
    }

    private fun replaceMemberVar(string: String, member: Member, yes: String, no: String): String = string
        .withVariable("nickname", member.nickname ?: "/")
        .withVariable("roleCount", member.roles.size.toString())
        .withVariable("isOwner", if (member.isOwner) yes else no)
        .withVariable("joinTime", member.timeJoined.asLongLongGMTString())
        .withVariable("boostTime", member.timeBoosted?.asLongLongGMTString() ?: "/")
        .withVariable("onlineStatus", member.onlineStatus.toUCSC())
        .withVariable("voiceStatus", getVoiceStatus(member))
        .withVariable("canMelijnInteract", if (member.guild.selfMember.canInteract(member)) yes else no)


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

    private fun replaceUserVar(string: String, user: User, isSupporter: Boolean, yes: String, no: String): String = string
        .withVariable("name", user.name)
        .withVariable(PLACEHOLDER_USER_ID, user.id)
        .withVariable("discrim", user.discriminator)
        .withVariable("isBot", if (user.isBot) yes else no)
        .withVariable("supportsMelijn", if (isSupporter) yes else no)
        .withVariable("avatarUrl", user.effectiveAvatarUrl)
        .withVariable("creationTime", user.timeCreated.asLongLongGMTString())
}