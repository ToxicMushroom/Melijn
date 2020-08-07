package me.melijn.melijnbot.database.role

import me.melijn.melijnbot.database.HIGHER_CACHE
import me.melijn.melijnbot.database.NORMAL_CACHE
import net.dv8tion.jda.api.utils.data.DataArray

class JoinRoleWrapper(private val joinRoleDao: JoinRoleDao) {

    // guildId -> <selfRoleGroupName -> emotejiInfo (see SelfRoleDao for example)

    suspend fun getJRI(guildId: Long): JoinRoleInfo {
        val result = joinRoleDao.getCacheEntry(guildId, HIGHER_CACHE)?.let {
            convertToJoinRoleInfo(it)
        }

        if (result != null) return result

        val joinroleInfo = convertToJoinRoleInfo(joinRoleDao.get(guildId))
        joinRoleDao.setCacheEntry(guildId, convertToJson(joinroleInfo), NORMAL_CACHE)
        return joinroleInfo
    }

    fun put(guildId: Long, joinRoleInfo: JoinRoleInfo) {
        joinRoleDao.put(guildId, convertToJson(joinRoleInfo))
        joinRoleDao.setCacheEntry(guildId, convertToJson(joinRoleInfo), NORMAL_CACHE)
    }

    private fun convertToJoinRoleInfo(fromString: String): JoinRoleInfo {
        val internalMap = mutableMapOf<String, List<JoinRoleInfo.JoinRoleEntry>>()
        val dataArray = if (fromString.isBlank()) {
            DataArray.empty()
        } else {
            DataArray.fromJson(fromString)
        }

        for (i in 0 until dataArray.length()) {
            val dataEntry = dataArray.getArray(i)
            val jrEntryList = mutableListOf<JoinRoleInfo.JoinRoleEntry>()

            val group = dataEntry.getString(0)
            val jrEntries = dataEntry.getArray(1)

            for (j in 0 until jrEntries.length()) {
                val jrEntry = jrEntries.getArray(j)

                var roleId: Long? = jrEntry.getLong(0)
                if (-1L == roleId) {
                    roleId = null
                }

                val chance = jrEntry.getInt(1)

                jrEntryList.add(JoinRoleInfo.JoinRoleEntry(roleId, chance))
            }

            internalMap[group] = jrEntryList
        }

        return JoinRoleInfo(internalMap)
    }

    private fun convertToJson(joinRoleInfo: JoinRoleInfo): String {
        val internalMap = joinRoleInfo.dataMap
        val dataArray = DataArray.empty()
        for ((group, jrEntryList) in internalMap) {
            val dataEntry = DataArray.empty()

            val jrEntries = DataArray.empty()

            for ((roleId, chance) in jrEntryList) {
                val dataJrEntry = DataArray.empty()
                dataJrEntry.add(roleId ?: -1)
                dataJrEntry.add(chance)

                jrEntries.add(dataJrEntry)
            }

            dataEntry.add(group)
            dataEntry.add(jrEntries)
            dataArray.add(dataEntry)
        }

        return dataArray.toString()
    }

    // inserts or updates an entry
    suspend fun set(guildId: Long, groupName: String, roleId: Long?, chance: Int) {
        val currentInfo = getJRI(guildId)
        val map = currentInfo.dataMap.toMutableMap()
        val list = currentInfo.dataMap.getOrDefault(groupName, emptyList()).toMutableList()
        val entryToUpdate = list.firstOrNull { (roleId1) -> roleId1 == roleId }
        if (entryToUpdate != null) {
            entryToUpdate.chance = chance
            list.removeIf { (roleId1) -> roleId1 == roleId }
            list.add(entryToUpdate)
        } else {
            list.add(JoinRoleInfo.JoinRoleEntry(roleId, chance))
        }
        map[groupName] = list
        currentInfo.dataMap = map

        put(guildId, currentInfo)
    }

    // [returns] true if the item was removed, false if it was never there
    suspend fun remove(guildId: Long, groupName: String, roleId: Long?): Boolean {
        val currentInfo = getJRI(guildId)
        val map = currentInfo.dataMap.toMutableMap()
        val list = currentInfo.dataMap.getOrDefault(groupName, emptyList()).toMutableList()
        val exists = list.any { (roleId1) -> roleId1 == roleId }

        if (exists) {
            list.removeIf { (roleId1) -> roleId1 == roleId }
        } else {
            return false
        }

        map[groupName] = list
        currentInfo.dataMap = map

        put(guildId, currentInfo)
        return true
    }
}

data class JoinRoleInfo(
    var dataMap: Map<String, List<JoinRoleEntry>>
) {

    data class JoinRoleEntry(
        var roleId: Long?,
        var chance: Int
    )
}