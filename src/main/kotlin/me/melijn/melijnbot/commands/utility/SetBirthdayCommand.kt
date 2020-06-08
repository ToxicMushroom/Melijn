package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.enums.DateFormat
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBirthdayByArgsNMessage
import me.melijn.melijnbot.objects.utils.getEnumFromArgN
import me.melijn.melijnbot.objects.utils.sendMsg
import java.time.LocalDate

class SetBirthdayCommand : AbstractCommand("command.setbirthday") {

    init {
        id = 139
        name = "setBirthDay"
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            val birthday = context.daoManager.birthdayWrapper.getBirthday(context.author.idLong)

            if (birthday == null) {
                val name = context.getTranslation("$root.show.unset")
                sendMsg(context, name)
            } else {
                val localDate = LocalDate.ofYearDay(2019, birthday.first)
                val dayOfMonth = localDate.dayOfMonth
                val month = localDate.monthValue

                val name = context.getTranslation("$root.show.set")
                    .replace("%birthday%", "$dayOfMonth/$month")
                    .replace("%birthyear%", birthday.second.toString())
                sendMsg(context, name)
            }
            return
        }
        if (context.args[0] == "null") {
            context.daoManager.birthdayWrapper.unsetBirthday(context.authorId)
            val message = context.getTranslation("$root.unset")
            sendMsg(context, message)
            return
        }

        val format = getEnumFromArgN(context, 0) ?: DateFormat.DMY
        val datePair = getBirthdayByArgsNMessage(context, 0, format) ?: return
        setBirthday(context, datePair)
    }
}

suspend fun setBirthday(context: CommandContext, datePair: Pair<Int, Int?>) {
    val birthday = datePair.first
    val optionalBirthYear = datePair.second

    val birthdayWrapper = context.daoManager.birthdayWrapper
    birthdayWrapper.setBirthday(context.authorId, birthday, optionalBirthYear)
    val localDate = LocalDate.ofYearDay(2019, birthday)
    val dayOfMonth = localDate.dayOfMonth
    val month = localDate.monthValue
    val extra = if (optionalBirthYear != null) ".year" else ""
    val msg = context.getTranslation("${context.commandOrder.first().root}.set$extra")
        .replace("%birthday%", "$dayOfMonth/$month")
        .replace("%birthyear%", "$optionalBirthYear")

    sendMsg(context, msg)
}