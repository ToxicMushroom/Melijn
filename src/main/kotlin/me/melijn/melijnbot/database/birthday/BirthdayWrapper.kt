package me.melijn.melijnbot.database.birthday

import me.melijn.melijnbot.objects.utils.isLeapYear
import java.util.*


class BirthdayWrapper(private val birthdayDao: BirthdayDao) {

    //userId -> birthDayInfo (Year, birthDay, time, timeZoneId)
    suspend fun getBirthdaysToday(): Map<Long, BirthdayInfo> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val isleap = calendar.isLeapYear()
        val dayOfYear = if (isleap && calendar[Calendar.DAY_OF_YEAR] >= 60) {
            calendar[Calendar.DAY_OF_YEAR] - 1
        } else calendar[Calendar.DAY_OF_YEAR]
        return birthdayDao.getBirthdays(dayOfYear)
    }

    suspend fun setBirthday(userId: Long, birthday: Int, birthyear: Int?) {
        birthdayDao.set(userId, birthday, birthyear ?: 0)
    }

    suspend fun getBirthday(userId: Long): Pair<Int, Int>? {
        return birthdayDao.get(userId)
    }

    suspend fun unsetBirthday(userId: Long) {
        birthdayDao.remove(userId)
    }
}