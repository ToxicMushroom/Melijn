package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.objects.threading.TaskManager
import java.util.*
import kotlin.Comparator
import kotlin.math.max

class CommandUsageWrapper(private val taskManager: TaskManager, private val commandUsageDao: CommandUsageDao) {

    suspend fun addUse(commandId: Int) {
        commandUsageDao.addUse(commandId)
    }

    suspend fun getUsageWithinPeriod(from: Long, until: Long): LinkedHashMap<Int, Long> {
        return commandUsageDao.getUsageWithinPeriod(from, until)
    }
    suspend fun getTopUsageWithinPeriod(from: Long, until: Long, top: Int): MutableMap<Int, Long> {
        val map =  commandUsageDao.getUsageWithinPeriod(from, until)
        return sortAndTopOfUsageMap(map, top)
    }

    private fun sortAndTopOfUsageMap(map: LinkedHashMap<Int, Long>, top: Int): MutableMap<Int, Long> {
        val sorted = map.toSortedMap(Comparator { o1, o2 ->
            val one = map.getOrDefault(o1, -1)
            val two = map.getOrDefault(o2, -1)
            two.compareTo(one)
        })
        val max = max(top, 1)
        val limitedMap = mutableMapOf<Int, Long>()
        for ((i, v) in sorted) {
            if (limitedMap.size >= max) break
            limitedMap[i] = v
        }

        return limitedMap
    }
}