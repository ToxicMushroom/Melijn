package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.enums.DateFormat
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.utils.getBirthdayByArgsNMessage
import me.melijn.melijnbot.objects.utils.getEnumFromArgN
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.withVariable
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
                sendRsp(context, name)
            } else {
                val localDate = LocalDate.ofYearDay(2019, birthday.first)
                val dayOfMonth = localDate.dayOfMonth
                val month = localDate.monthValue

                val name = context.getTranslation("$root.show.set")
                    .withVariable("birthday", "$dayOfMonth/$month")
                    .withVariable("birthyear", birthday.second.toString())
                sendRsp(context, name)
            }
            return
        }
        if (context.args[0] == "null") {
            context.daoManager.birthdayWrapper.unsetBirthday(context.authorId)
            val message = context.getTranslation("$root.unset")
            sendRsp(context, message)
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
        .withVariable("birthday", "$dayOfMonth/$month")
        .withVariable("birthyear", "$optionalBirthYear")

    sendRsp(context, msg)
}