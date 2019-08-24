package me.melijn.melijnbot.database.mute

import me.melijn.melijnbot.objects.threading.TaskManager

class MuteWrapper(val taskManager: TaskManager, private val muteDao: MuteDao) {

    fun getUnmuteableMutes(): List<Mute> {
        return muteDao.getUnmuteableMutes()
    }

    fun setMute(newMute: Mute) {
        muteDao.setMute(newMute)
    }

    fun getActiveMute(guildId: Long, mutedId: Long): Mute? {
        return muteDao.getActiveMute(guildId, mutedId)
    }
}