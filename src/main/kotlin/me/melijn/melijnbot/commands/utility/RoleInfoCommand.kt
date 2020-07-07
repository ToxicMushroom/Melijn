package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ROLE_ID
import me.melijn.melijnbot.objects.utils.asLongLongGMTString
import me.melijn.melijnbot.objects.utils.getRoleByArgsNMessage
import me.melijn.melijnbot.objects.utils.message.sendEmbedRsp
import me.melijn.melijnbot.objects.utils.message.sendSyntax
import me.melijn.melijnbot.objects.utils.withVariable
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
            sendSyntax(context)
            return
        }

        val role = getRoleByArgsNMessage(context, 0, false) ?: return
        val tile1 = context.getTranslation("$root.response1.field1.title")
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")
        val unReplacedValue1 = context.getTranslation("$root.response1.field1.value")
        val value1 = replaceRoleVars(unReplacedValue1, role, yes, no)

        val eb = Embedder(context)
            .addField(tile1, value1, false)
        sendEmbedRsp(context, eb.build())
    }

    private fun replaceRoleVars(string: String, role: Role, yes: String, no: String): String = string
        .withVariable("roleName", role.name)
        .withVariable(PLACEHOLDER_ROLE_ID, role.id)
        .withVariable("creationTime", role.timeCreated.asLongLongGMTString())
        .withVariable("position", role.position.toString() + "/" + role.guild.roleCache.size())
        .withVariable("members", role.guild.memberCache.stream().filter { member -> member.roles.contains(role) }.count().toString())
        .withVariable("isMentionable", if (role.isMentionable) yes else no)
        .withVariable("isHoisted", if (role.isHoisted) yes else no)
        .withVariable("isManaged", if (role.isManaged) yes else no)
        .withVariable("color", getColorString(role))
        .withVariable("canMelijnInteract", if (role.guild.selfMember.canInteract(role)) yes else no)

    private fun getColorString(role: Role): String {
        if (role.color == null) return "none"
        return "${role.colorRaw} | #${getHex(role.color)} | RGB(${role.color?.red}, ${role.color?.green}, ${role.color?.blue})"
    }

    private fun getHex(color: Color?): String {
        return String.format("%02X%02X%02X", color?.red, color?.green, color?.blue)
    }
}