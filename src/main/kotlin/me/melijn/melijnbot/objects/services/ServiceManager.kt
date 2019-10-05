package me.melijn.melijnbot.objects.services

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.services.bans.BanService
import me.melijn.melijnbot.objects.services.mutes.MuteService
import me.melijn.melijnbot.objects.threading.TaskManager
import net.dv8tion.jda.api.sharding.ShardManager

class ServiceManager(val taskManager: TaskManager, val daoManager: DaoManager) {

    var started = false
    var shardManager: ShardManager? = null
    private var banService: BanService? = null
    private var muteService: MuteService? = null


    fun init(shardManager: ShardManager) {
        this.shardManager = shardManager
        banService = BanService(shardManager, daoManager.banWrapper, daoManager.logChannelWrapper, daoManager.embedDisabledWrapper, daoManager)
        muteService = MuteService(shardManager, taskManager, daoManager.muteWrapper, daoManager.logChannelWrapper, daoManager.embedDisabledWrapper, daoManager)
    }

    fun startServices() {
        requireNotNull(shardManager) { "Init first!" }
        banService?.start()
        muteService?.start()

        started = true
    }

    fun stopServices() {
        require(started) { "Never started!" }
        banService?.stop()
        muteService?.stop()
    }
}