package me.melijn.melijnbot.database.ban

import me.melijn.melijnbot.database.locking.EntityType

class BotBannedWrapper(private val botBannedDao: BotBannedDao) {

    companion object {
        val bannedUsers = HashSet<Long>()
        val bannedGuilds = HashSet<Long>()

        fun isBotBanned(type: EntityType, id: Long): Boolean {
            return when (type) {
                EntityType.GUILD -> bannedGuilds.contains(id)
                EntityType.USER -> bannedUsers.contains(id)
                else -> false
            }
        }
    }

    suspend fun initialize() {
        bannedUsers.addAll(botBannedDao.getAll(EntityType.USER).map { it.id })
        bannedGuilds.addAll(botBannedDao.getAll(EntityType.GUILD).map { it.id })
    }

    fun add(type: EntityType, id: Long, reason: String) {
        if (type == EntityType.GUILD) bannedGuilds.add(id)
        else if (type == EntityType.USER) bannedUsers.add(id)

        botBannedDao.add(type, id, reason)
    }

    fun remove(id: Long) {
        bannedGuilds.remove(id)
        bannedUsers.remove(id)
        botBannedDao.remove(id)
    }

    suspend fun get(id: Long): BotBanInfo? {
        return botBannedDao.get(id)
    }

    suspend fun renew(): Set<BotBanInfo> {
        val allGuild = botBannedDao.getAll(EntityType.GUILD)
        val newGuildBans = fish(allGuild, bannedUsers)

        val allUsers = botBannedDao.getAll(EntityType.USER)
        val newUserBans = fish(allUsers, bannedUsers)

        return (newUserBans + newGuildBans).toSet()
    }

    private fun fish(all: Set<BotBanInfo>, bannedEntities: HashSet<Long>): List<BotBanInfo> {
        val newBans = all.filter { maybeNew -> bannedEntities.none { guild -> guild == maybeNew.id } }
        val removedBans = bannedEntities.filter { banned -> all.none { it.id == banned } }
        bannedEntities.removeAll(removedBans)
        bannedEntities.addAll(newBans.map { it.id })
        return newBans
    }
}

data class BotBanInfo(
    val id: Long,
    val entityType: EntityType,
    val reason: String,
    val moment: Long
)
