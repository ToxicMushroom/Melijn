package me.melijn.melijnbot.objects.services

import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.objects.services.bans.BanService
import me.melijn.melijnbot.objects.services.mutes.MuteService
import net.dv8tion.jda.api.sharding.ShardManager

class ServiceManager(val daoManager: DaoManager) {

    var started = false
    var shardManager: ShardManager? = null
    private var banService: BanService? = null
    private var muteService: MuteService? = null


    fun init(shardManager: ShardManager) {
        this.shardManager = shardManager
        banService = BanService(shardManager, daoManager.banWrapper, daoManager.logChannelWrapper, daoManager.embedDisabledWrapper)
        muteService = MuteService(shardManager, daoManager.muteWrapper, daoManager.logChannelWrapper, daoManager.embedDisabledWrapper)
    }

    fun startServices() {
        if (shardManager == null) throw IllegalArgumentException("Init first!")
        banService?.start()
        muteService?.start()

        started = true
    }

    fun stopServices() {
        if (!started) throw IllegalArgumentException("Never started!")
        banService?.stop()
        muteService?.stop()
    }
}