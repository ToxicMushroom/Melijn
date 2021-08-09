package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Method

object MassTargetPunishmentMethods {
    fun getMethods(): List<Method> {
        return listOf(
            Method("punishList", { env ->
                env.getReifiedX<MassPunishJagTagParserArgs>("massPunishArgs").punished
                    .joinToString("\n") {
                        "${it.id} - ${it.asTag}"
                    }
            })
        )
    }
}
