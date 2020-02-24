package me.melijn.melijnbot.objects.jagtag

import com.jagrosh.jagtag.Method
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleByType
import me.melijn.melijnbot.objects.utils.isLeapYear
import net.dv8tion.jda.api.entities.Guild
import java.util.*
import kotlin.math.roundToInt

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
        }),
        Method("age", { env ->
            val born = env.getReifiedX<Int>("birthYear")
            if (born == 0)
                "unknown"
            else {
                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val isleap = calendar.isLeapYear()
                val year = calendar[Calendar.YEAR]
                val dayOfYear = if (isleap && calendar[Calendar.DAY_OF_YEAR] >= 60) {
                    calendar[Calendar.DAY_OF_YEAR] - 1
                } else calendar[Calendar.DAY_OF_YEAR]

                val bornMillis = ((year - 1970) * 365.25 + dayOfYear) * 24 * 60 * 60 * 1000
                val millisDifference = System.currentTimeMillis() - bornMillis
                val age = (millisDifference / 1000 / 60 / 60 / 24 / 365.25).roundToInt()

                age.toString()
            }
        })
    )
}