package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.Method
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import net.dv8tion.jda.api.entities.Guild

object BirthdayMethods {
    fun getMethods(): List<Method> = listOf(
        Method("startOfBirthday", { env ->
            val guild = env.getReifiedX<Guild>("guild")
            val daoManager = env.getReifiedX<DaoManager>("daoManager")

            val time = System.currentTimeMillis().asEpochMillisToDateTime(daoManager, guild.idLong)
            time
        }),
        Method("endOfBirthday", { env ->
            val guild = env.getReifiedX<Guild>("guild")
            val daoManager = env.getReifiedX<DaoManager>("daoManager")

            val time = (System.currentTimeMillis() + 86_400_000).asEpochMillisToDateTime(daoManager, guild.idLong)
            time
        }),
        Method("birthdayRole", { env ->
            val guild = env.getReifiedX<Guild>("guild")
            val daoManager = env.getReifiedX<DaoManager>("daoManager")
            val birthdayRole = guild.getAndVerifyRoleByType(daoManager, RoleType.BIRTHDAY, false)

            birthdayRole?.name ?: "null"
        })
    )
}