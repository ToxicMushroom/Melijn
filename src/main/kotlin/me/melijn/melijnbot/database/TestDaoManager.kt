package me.melijn.melijnbot.database

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.database.ban.BanDao
import me.melijn.melijnbot.database.ban.BanWrapper
import me.melijn.melijnbot.database.ban.SoftBanDao
import me.melijn.melijnbot.database.ban.SoftBanWrapper
import me.melijn.melijnbot.database.kick.KickDao
import me.melijn.melijnbot.database.kick.KickWrapper
import me.melijn.melijnbot.database.mute.MuteDao
import me.melijn.melijnbot.database.mute.MuteWrapper
import me.melijn.melijnbot.database.warn.WarnDao
import me.melijn.melijnbot.database.warn.WarnWrapper
import me.melijn.melijnbot.objects.threading.TaskManager


class TestDaoManager(taskManager: TaskManager, dbSettings: Settings.Database) {

    companion object {
        val afterTableFunctions = mutableListOf<() -> Unit>()
    }

    lateinit var dbVersion: String
    lateinit var connectorVersion: String

    val banWrapper: BanWrapper
    val muteWrapper: MuteWrapper
    val kickWrapper: KickWrapper
    val warnWrapper: WarnWrapper
    val softBanWrapper: SoftBanWrapper

    var driverManager: DriverManager

    init {
        driverManager = DriverManager(dbSettings
            //, dbSettings.mySQL
        )

        runBlocking {
            dbVersion = driverManager.getDBVersion()
            connectorVersion = driverManager.getConnectorVersion()
        }

        banWrapper = BanWrapper(taskManager, BanDao(driverManager))
        muteWrapper = MuteWrapper(taskManager, MuteDao(driverManager))
        kickWrapper = KickWrapper(taskManager, KickDao(driverManager))
        warnWrapper = WarnWrapper(taskManager, WarnDao(driverManager))
        softBanWrapper = SoftBanWrapper(taskManager, SoftBanDao(driverManager))

        //After registering wrappers
        driverManager.executeTableRegistration()
        for (func in afterTableFunctions) {
            func()
        }
    }
}