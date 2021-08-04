package me.melijn.melijnbot.internals.jagtag

import com.jagrosh.jagtag.Method

object SingleTargetPunishmentMethods  {
    fun getMethods(): List<Method> {
        return PunishmentMethods.getUserMethods("punishedUser") { env -> PunishmentMethods.getArgs(env).punished }
    }
}