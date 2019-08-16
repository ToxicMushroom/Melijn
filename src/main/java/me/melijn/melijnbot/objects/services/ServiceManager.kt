package me.melijn.melijnbot.objects.services

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.services.bans.BanService

class ServiceManager(container: Container) {

    val banService = BanService(container.daoManager.banWrapper)

    fun startServices() {
        banService.start()
    }
}