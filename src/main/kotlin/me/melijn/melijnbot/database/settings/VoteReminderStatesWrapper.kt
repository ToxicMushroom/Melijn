package me.melijn.melijnbot.database.settings

class VoteReminderStatesWrapper(private val denyVoteReminderStatesDao: VoteReminderStatesDao) {

    suspend fun contains(userId: Long): Map<VoteReminderOption, Boolean> { // Map of voteReminderOptions and their actual state for the user
        val flags = denyVoteReminderStatesDao.getFlags(userId)

        val stateMap = mutableMapOf<VoteReminderOption, Boolean>()
        VoteReminderOption.values().forEach { opt ->
            stateMap[opt] = if (flags.contains(opt.number)) {
                !opt.default
            } else {
                opt.default
            }
        }
        return stateMap
    }

    suspend fun getRaw(userId: Long): Map<Int, Boolean> { // Map of voteReminderOptions and their actual state for the user
        val flags = denyVoteReminderStatesDao.getFlags(userId)

        val stateMap = mutableMapOf<Int, Boolean>()
        VoteReminderOption.values().forEach { opt ->
            stateMap[opt.number] = if (flags.contains(opt.number)) {
                !opt.default
            } else {
                opt.default
            }
        }
        return stateMap
    }

    fun enable(userId: Long, option: VoteReminderOption) {
        denyVoteReminderStatesDao.add(userId, option.number)
    }

    fun remove(userId: Long, option: VoteReminderOption) {
        denyVoteReminderStatesDao.delete(userId, option.number)
    }
}

enum class VoteReminderOption(val number: Int, val default: Boolean) {
    // true = default reminders, false = no default reminders

    TOPGG(1, true),
    DBLCOM(2, false),
    BFDCOM(3, false),
    DBOATS(4, false),
    GLOBAL(5, false)
}