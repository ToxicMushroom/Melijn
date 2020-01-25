package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.utils.sendMsg

class SetBirthdayCommand : AbstractCommand("command.setbirthday") {

    init {
        id = 139
        name = "setBirthDay"
        runConditions = arrayOf(RunCondition.SUPPORTER)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        when {
            context.args.isEmpty() -> {
                val birthday = context.daoManager.birthdayWrapper.getBirthday(context.author.idLong)
                if (birthday == null) {
                    val name = context.getTranslation("$root.show.unset")
                    sendMsg(context, name)
                } else {
                    val name = context.getTranslation("$root.show.set")
                        .replace("%birthday%", birthday.first.toString())
                        .replace("%birthyear%", birthday.second.toString())
                    sendMsg(context, name)
                }
            }
            context.args.size == 1 -> {

            }
            else -> {

            }
        }
    }

}