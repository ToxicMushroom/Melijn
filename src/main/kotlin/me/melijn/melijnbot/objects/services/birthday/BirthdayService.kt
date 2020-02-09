package me.melijn.melijnbot.objects.services.birthday

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.enums.ChannelType
import me.melijn.melijnbot.enums.RoleType
import me.melijn.melijnbot.objects.services.Service
import me.melijn.melijnbot.objects.utils.LogUtils
import me.melijn.melijnbot.objects.utils.awaitBool
import me.melijn.melijnbot.objects.utils.awaitEX
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyChannelById
import me.melijn.melijnbot.objects.utils.checks.getAndVerifyRoleById
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class BirthdayService(
    val shardManager: ShardManager,
    val daoManager: DaoManager
) : Service("birthday") {

    private var scheduledFuture: ScheduledFuture<*>? = null

    private val birthdayService = Runnable {
        runBlocking {
            try {
                val birthdayHistory = daoManager.birthdayHistoryWrapper
                val birthdays = daoManager.birthdayWrapper.getBirthdaysToday()
                val birthDaysToRemove = daoManager.birthdayHistoryWrapper.getBirthdaysToDeactivate()

                val roles = daoManager.roleWrapper.getRoles(RoleType.BIRTHDAY)
                val channels = daoManager.channelWrapper.getChannels(ChannelType.BIRTHDAY).toMutableMap()

                val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentDay = calendar.get(Calendar.HOUR_OF_DAY)

                for (guildId in roles.keys) {
                    val guild = shardManager.getGuildById(guildId)
                    if (guild == null) {
                        daoManager.roleWrapper.removeRole(guildId, RoleType.BIRTHDAY)
                        continue
                    }

                    val role = roles[guildId]?.let {
                        guild.getAndVerifyRoleById(daoManager, RoleType.BIRTHDAY, it, true)
                    } ?: continue

                    //Add birthday role (maybe channel message)
                    for ((userId, triple) in birthdays) {
                        val member = guild.getMemberById(userId) ?: return@runBlocking
                        if ((currentDay == 1 && (triple.third < currentHour || triple.second < currentDay)) || triple.second < currentDay || triple.third < currentHour) {
                            if (birthdayHistory.contains(calendar.get(Calendar.YEAR), guildId, userId)) continue
                            val ex = guild.addRoleToMember(member, role)
                                .reason("Birthday begins")
                                .awaitEX()

                            if (ex == null)
                                birthdayHistory.add(calendar.get(Calendar.YEAR), guildId, userId)


                            val channelId = channels.getOrElse(guildId) { null } ?: continue
                            val textChannel = guild.getAndVerifyChannelById(daoManager, ChannelType.BIRTHDAY, channelId, setOf(Permission.MESSAGE_WRITE))
                            if (textChannel == null) {
                                channels.remove(guildId)
                                continue
                            }

                            LogUtils.sendBirthdayMessage(daoManager, textChannel, member)
                            channels.remove(guildId)
                        }
                    }

                    //Birthdays to remove
                    for ((userId, pair) in birthDaysToRemove) {
                        val member = guild.getMemberById(userId) ?: return@runBlocking
                        guild.removeRoleFromMember(member, role)
                            .reason("Birthday is over")
                            .awaitBool()

                        birthdayHistory.deactivate(pair.first, guildId, userId)
                    }
                }

                //Send birthday channel message
                for (guildId in channels.keys) {
                    val guild = shardManager.getGuildById(guildId)
                    if (guild == null) {
                        daoManager.roleWrapper.removeRole(guildId, RoleType.BIRTHDAY)
                        continue
                    }


                    val textChannel = channels[guildId]?.let {
                        guild.getAndVerifyChannelById(daoManager, ChannelType.BIRTHDAY, it, setOf(Permission.MESSAGE_WRITE))
                    } ?: continue

                    for ((userId, triple) in birthdays) {
                        val member = guild.getMemberById(userId) ?: return@runBlocking
                        if ((currentDay == 1 && (triple.third < currentHour || triple.second < currentDay)) || triple.second < currentDay || triple.third < currentHour) {
                            if (birthdayHistory.contains(calendar.get(Calendar.YEAR), guildId, userId)) continue

                            LogUtils.sendBirthdayMessage(daoManager, textChannel, member)
                            birthdayHistory.add(calendar.get(Calendar.YEAR), guildId, userId)
                        }
                    }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun start() {
        logger.info("Started BirthdayService")
        scheduledFuture = scheduledExecutor.scheduleWithFixedDelay(birthdayService, 1_100, 1_000, TimeUnit.MILLISECONDS)
    }

    override fun stop() {
        logger.info("Stopping BirthdayService")
        scheduledFuture?.cancel(false)
    }
}