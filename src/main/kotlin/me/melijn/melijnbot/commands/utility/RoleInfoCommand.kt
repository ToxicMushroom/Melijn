package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.asLongLongGMTString
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.Role
import java.awt.Color

class RoleInfoCommand : AbstractCommand("command.roleinfo") {

    init {
        id = 9
        name = "roleInfo"
        aliases = arrayOf("role")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val language = context.getLanguage()
        val role = getRoleByArgsNMessage(context, 0, false) ?: return
        val tile1 = i18n.getTranslation(language, "$root.response1.field1.title")
        val yes = i18n.getTranslation(language, "yes")
        val no = i18n.getTranslation(language, "no")
        val unReplacedValue1 = i18n.getTranslation(language, "$root.response1.field1.value")
        val value1 = replaceRoleVars(unReplacedValue1, role, yes, no)

        val eb = Embedder(context)
        eb.addField(tile1, value1, false)
        sendEmbed(context, eb.build())
    }

    private fun replaceRoleVars(string: String, role: Role, yes: String, no: String): String = string
            .replace("%roleName%", role.name)
            .replace("%roleId%", role.id)
            .replace("%creationTime%", role.timeCreated.asLongLongGMTString())
            .replace("%position%", role.position.toString() + "/" + role.guild.roleCache.size())
            .replace("%members%", role.guild.memberCache.stream().filter { member -> member.roles.contains(role) }.count().toString())
            .replace("%isMentionable%", if (role.isMentionable) yes else no)
            .replace("%isHoisted%", if (role.isHoisted) yes else no)
            .replace("%isManaged%", if (role.isManaged) yes else no)
            .replace("%color%", getColorString(role))

    private fun getColorString(role: Role): String {
        if (role.color == null) return "none"
        return "${role.colorRaw} | #${getHex(role.color)} | RGB(${role.color?.red}, ${role.color?.green}, ${role.color?.blue})"
    }

    private fun getHex(color: Color?): String {
        return String.format("%02X%02X%02X", color?.red, color?.green, color?.blue)
    }
}