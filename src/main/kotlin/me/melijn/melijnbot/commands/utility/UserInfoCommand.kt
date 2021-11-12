package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_USER_ID
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.retrieveUserByArgsNMessage
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.utils.TimeFormat
import net.dv8tion.jda.api.utils.Timestamp
import java.time.OffsetDateTime

class UserInfoCommand : AbstractCommand("command.userinfo") {

    init {
        id = 8
        name = "userInfo"
        aliases = arrayOf("ui", "user", "memberInfo", "member", "mi")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: ICommandContext) {
        val user: User = if (context.args.isEmpty()) context.author
        else retrieveUserByArgsNMessage(context, 0) ?: return
        val member: Member? = context.guildN?.retrieveMember(user)?.awaitOrNull()

        val isSupporter = context.daoManager.supporterWrapper.getUsers().contains(user.idLong)

        val title1 = context.getTranslation("$root.response1.field1.title")
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")

        val userProfile = user.retrieveProfile().awaitOrNull()
        val unReplacedValue1 = context.getTranslation("$root.response1.field1.value")
        val value1 = replaceUserVar(unReplacedValue1, user, isSupporter, yes, no, userProfile)

        val eb = Embedder(context)
            .setThumbnail(member?.avatarUrl ?: user.effectiveAvatarUrl)
            .setDescription(
                """
                |```INI
                |[${title1}]```$value1
            """.trimMargin()
            )

        if (context.isFromGuild && member != null) {
            val title2 = context.getTranslation("$root.response1.field2.title")
            val unReplacedValue2 = context.getTranslation("$root.response1.field2.value")
            val value2 = replaceMemberVar(unReplacedValue2, member, yes, no)
            eb.appendDescription(
                """
                |
                |
                |```INI
                |[${title2}]```$value2
            """.trimMargin()
            )
        }

        sendEmbedRsp(context, eb.build())
    }

    private fun replaceMemberVar(string: String, member: Member, yes: String, no: String): String {
        var s = string
            .withVariable("nickname", member.nickname ?: "/")
            .withVariable("roleCount", member.roles.size.toString())
            .withVariable("isOwner", if (member.isOwner) yes else no)
            .withVariable("joinTime", TimeFormat.DATE_TIME_SHORT.atDate(member.timeJoined))
            .withVariable("boostTime", member.timeBoosted?.let { TimeFormat.DATE_TIME_SHORT.atDate(it) } ?: "/")
            .withVariable("voiceStatus", getVoiceStatus(member))
            .withVariable("canMelijnInteract", if (member.guild.selfMember.canInteract(member)) yes else no)
        if (member.avatarUrl != null) {
            s += "\n**Server Avatar:** [link](${member.avatarUrl})"
        }

        return s
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

    private fun replaceUserVar(
        string: String,
        user: User,
        isSupporter: Boolean,
        yes: String,
        no: String,
        userProfile: User.Profile?
    ): String {
        var s = string
            .withVariable("name", user.name)
            .withVariable(PLACEHOLDER_USER_ID, user.id)
            .withVariable("discrim", user.discriminator)
            .withVariable("isBot", if (user.isBot) yes else no)
            .withVariable("supportsMelijn", if (isSupporter) yes else no)
            .withVariable("avatarUrl", user.effectiveAvatarUrl)
            .withVariable("creationTime", TimeFormat.DATE_TIME_SHORT.atDate(user.timeCreated))
        s += "\n**Public Flags:** ${user.flags.joinToString(" ") { getBadge(it) }}"
        if (userProfile?.bannerUrl != null) {
            s += "\n**Banner:** [link](${userProfile.bannerUrl})"
        }
        return s
    }

}

fun TimeFormat.atDate(date: OffsetDateTime): Timestamp {
    return this.atInstant(date.toInstant())
}

private fun getBadge(flag: User.UserFlag): String {
    return when (flag) {
        User.UserFlag.STAFF -> "<:furry:907322194156224542>"
        User.UserFlag.PARTNER -> "<:partnered:907322256567447552>"
        User.UserFlag.BUG_HUNTER_LEVEL_1 -> "<:bug_hunter:907322130151141416>"
        User.UserFlag.BUG_HUNTER_LEVEL_2 -> "<:gold_bughunter:907322205917052978>"
        User.UserFlag.HYPESQUAD -> "<:hypesquad_events_v1:907322220056023080>"
        User.UserFlag.HYPESQUAD_BRAVERY -> "<:bravery:907322115454300190>"
        User.UserFlag.HYPESQUAD_BRILLIANCE -> "<:brilliance:907322122580406332>"
        User.UserFlag.HYPESQUAD_BALANCE -> "<:balance:907321974211108984>"
        User.UserFlag.EARLY_SUPPORTER -> "<:early_supporter:907322161159626753>"
        User.UserFlag.TEAM_USER -> "`team user`"
        User.UserFlag.VERIFIED_BOT -> "`verified bot`"
        User.UserFlag.VERIFIED_DEVELOPER -> "<:early_verified_developer:907322174329716818>"
        User.UserFlag.CERTIFIED_MODERATOR -> "<:certified_virgin:907322144109756426>"
        User.UserFlag.BOT_HTTP_INTERACTIONS -> "`http bot`"
        User.UserFlag.UNKNOWN -> "`Unknown: ${flag.rawValue}`"
        else -> "`Unknown: ${flag.rawValue}`"
    }
}