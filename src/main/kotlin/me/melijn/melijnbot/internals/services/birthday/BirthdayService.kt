package me.melijn.melijnbot.internals.services.birthday

import io.ktor.client.*
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.utils.LogUtils
import me.melijn.melijnbot.internals.utils.awaitBool
import me.melijn.melijnbot.internals.utils.awaitEX
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyChannelById
import me.melijn.melijnbot.internals.utils.checks.getAndVerifyRoleById
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.*
import java.util.concurrent.TimeUnit

class BirthdayService(
    val shardManager: ShardManager,
    val httpClient: HttpClient,
    val daoManager: DaoManager
) : Service("Birthday", 2, 5, TimeUnit.MINUTES) {

    override val service = RunnableTask {
        val birthdayHistory = daoManager.birthdayHistoryWrapper
        val birthdays = daoManager.birthdayWrapper.getBirthdaysToday()
        val birthDaysToRemove = daoManager.birthdayHistoryWrapper.getBirthdaysToDeactivate()

        val roles = daoManager.roleWrapper.getRoles(RoleType.BIRTHDAY)
        val channels = daoManager.channelWrapper.getChannels(ChannelType.BIRTHDAY).toMutableMap()

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val currentMinuteOfDay = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val currentDay = calendar.get(Calendar.DAY_OF_YEAR)

        //If role is set
        for (guildId in roles.keys) {
            val guild = shardManager.getGuildById(guildId) ?: continue

            //Get birthday role
            val role = roles[guildId]?.let {
                guild.getAndVerifyRoleById(daoManager, RoleType.BIRTHDAY, it, true)
            } ?: continue

            val guildZone = daoManager.timeZoneWrapper.getTimeZone(guild.idLong)
            val guildTZ = TimeZone.getTimeZone(if (guildZone.isBlank()) "GMT" else guildZone)

            //Add birthday role (maybe channel message)
            for ((userId, info) in HashMap(birthdays)) {
                val member = guild.retrieveMemberById(userId).awaitOrNull() ?: continue
                val userTZ = info.zoneId?.let { TimeZone.getTimeZone(it) } ?: guildTZ

                var actualBirthday = info.birthday
                info.startMinute = userTZ.rawOffset / 60_000.0
                if (info.startMinute < 0) { //Transform birthday to utc
                    actualBirthday -= 1
                    info.startMinute = 24.0 * 60.0 + info.startMinute
                }


                //Cool checks to see if birthday should happen
                if (
                    (currentDay == 1 && (info.startMinute <= currentMinuteOfDay || actualBirthday <= currentDay)) ||
                    actualBirthday < currentDay ||
                    (info.startMinute <= currentMinuteOfDay && actualBirthday == currentDay)
                ) {
                    //Check if birthday already happened
                    if (birthdayHistory.contains(calendar.get(Calendar.YEAR), guildId, userId)) continue
                    val ex = guild.addRoleToMember(member, role)
                        .reason("Birthday begins")
                        .awaitEX()

                    //If role was added add birthday to history
                    if (ex == null)
                        birthdayHistory.add(calendar.get(Calendar.YEAR), guildId, userId)

                    //Get birthday log channel
                    val channelId = channels.getOrElse(guildId) { null } ?: continue
                    val textChannel = guild.getAndVerifyChannelById(daoManager, ChannelType.BIRTHDAY, channelId, setOf(Permission.MESSAGE_WRITE))
                    if (textChannel == null) {
                        channels.remove(guildId)
                        continue
                    }

                    //send birthday message
                    LogUtils.sendBirthdayMessage(daoManager,httpClient, textChannel, member, info.birthYear)
                }
            }

            //Birthdays to remove
            for ((userId, pair) in birthDaysToRemove) {
                val member = guild.retrieveMemberById(userId).awaitOrNull() ?: continue
                guild.removeRoleFromMember(member, role)
                    .reason("Birthday is over")
                    .awaitBool()

                birthdayHistory.deactivate(pair.first, guildId, userId)
            }
        }

        //Send birthday channel message
        for (guildId in channels.keys) {
            val guild = shardManager.getGuildById(guildId) ?: continue

            //Get guild timezone (GMT if none set)
            val guildZone = daoManager.timeZoneWrapper.getTimeZone(guild.idLong)
            val guildTZ = TimeZone.getTimeZone(if (guildZone.isBlank()) "GMT" else guildZone)

            //Get birthday channel
            val textChannel = channels[guildId]?.let {
                guild.getAndVerifyChannelById(daoManager, ChannelType.BIRTHDAY, it, setOf(Permission.MESSAGE_WRITE))
            } ?: continue

            for ((userId, info) in birthdays) {
                val member = guild.retrieveMemberById(userId).awaitOrNull() ?: continue
                val userTZ = info.zoneId?.let { TimeZone.getTimeZone(it) } ?: guildTZ

                var actualBirthday = info.birthday

                info.startMinute = userTZ.rawOffset / 60_000.0
                if (info.startMinute < 0) { //Transform birthday to utc
                    actualBirthday -= 1
                    info.startMinute = 24.0 * 60.0 + info.startMinute
                }

                //Cool checks to see if birthday should happen
                if (
                    (currentDay == 1 && (info.startMinute <= currentMinuteOfDay || actualBirthday <= currentDay)) ||
                    actualBirthday < currentDay ||
                    (info.startMinute <= currentMinuteOfDay && actualBirthday == currentDay)
                ) {
                    if (birthdayHistory.contains(calendar.get(Calendar.YEAR), guildId, userId)) continue

                    LogUtils.sendBirthdayMessage(daoManager, httpClient, textChannel, member, info.birthYear)
                    birthdayHistory.add(calendar.get(Calendar.YEAR), guildId, userId)
                }
            }
        }
    }
}