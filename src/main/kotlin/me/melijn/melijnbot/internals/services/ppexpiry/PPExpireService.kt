package me.melijn.melijnbot.internals.services.ppexpiry

import me.melijn.melijnbot.database.autopunishment.AutoPunishmentWrapper
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask

class PPExpireService(private val autoPunishmentWrapper: AutoPunishmentWrapper) : Service("ppexpiry", 30, 30) {

    override val service: RunnableTask = RunnableTask {
        autoPunishmentWrapper.removeExpiredEntries()
    }
}