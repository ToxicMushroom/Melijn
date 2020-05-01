package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.*

class SetRoleColorCommand : AbstractCommand("command.setrolecolor") {

    init {
        id = 167
        name = "setRoleColor"
        aliases = arrayOf("src")
        commandCategory = CommandCategory.ADMINISTRATION
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        if (context.args.size == 1) {
            val role = getRoleByArgsNMessage(context, 0) ?: return
            val color = role.color
            if (color == null) {
                val msg = context.getTranslation("$root.show.unset")
                    .replace("%role%", role.name)
                sendMsg(context, msg)
            } else {
                val plane = ImageUtils.createPlane(100, color.rgb)
                val msg = context.getTranslation("$root.show")
                    .replace("%role%", role.name)
                    .replace("%color%", color.toHex())
                sendMsg(context, msg, plane, "jpg")
            }

            return
        }

        val role = getRoleByArgsNMessage(context, 0, canInteract = true) ?: return
        val oldColor = role.color

        if (context.args[1] == "null") {
            role.manager.setColor(null).reason("setRoleColor command").queue()
            val msg = context.getTranslation("$root.unset")
                .replace("%role%", role.name)
                .replace("%oldColor%", oldColor?.toHex() ?: "/")
            sendMsg(context, msg)
        } else {
            val color = getColorFromArgNMessage(context, 0) ?: return
            role.manager.setColor(color).reason("setRoleColor command").queue()
            val plane = ImageUtils.createPlane(100, color.rgb)

            val msg = context.getTranslation("$root.set")
                .replace("%role%", role.name)
                .replace("%oldColor%", oldColor?.toHex() ?: "/")
                .replace("%color%", color.toHex())
            sendMsg(context, msg, plane, "jpg")
        }
    }
}