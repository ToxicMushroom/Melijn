package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.asFullLongGMTString
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

    override fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context, syntax)
            return
        }

        val role = getRoleByArgsNMessage(context, 0, false) ?: return
        val tile1 = Translateable("$root.response1.field1.title").string(context)
        val value1 = replaceRoleVars(Translateable("$root.response1.field1.value").string(context), role)

        val eb = Embedder(context)
        eb.addField(tile1, value1, false)
        sendEmbed(context, eb.build())
    }

    private fun replaceRoleVars(string: String, role: Role): String {
        return string
                .replace("%roleName%", role.name)
                .replace("%roleId%", role.id)
                .replace("%creationTime%", role.timeCreated.asFullLongGMTString())
                .replace("%position%", role.position.toString() + "/" +  role.guild.roleCache.size())
                .replace("%members%", role.guild.memberCache.stream().filter { member -> member.roles.contains(role) }.count().toString())
                .replace("%isMentionable%", if (role.isMentionable) "Yes" else "No")
                .replace("%isHoisted%", if (role.isHoisted) "Yes" else "No")
                .replace("%isManaged%", if (role.isManaged) "Yes" else "No")
                .replace("%color%", getColorString(role))
    }

    fun getColorString(role: Role): String {
        if (role.color == null) return "none"
        return "${role.colorRaw} | #${getHex(role.color)} | RGB(${role.color?.red}, ${role.color?.green}, ${role.color?.blue})"
    }

    fun getHex(color: Color?): String {
        return String.format("%02X%02X%02X", color?.red, color?.green, color?.blue)
    }
}