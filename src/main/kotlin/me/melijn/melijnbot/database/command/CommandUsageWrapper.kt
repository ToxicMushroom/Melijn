package me.melijn.melijnbot.database.command

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.command.AbstractCommand
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
    suspend fun getTopUsageWithinPeriod(from: Long, until: Long, top: Int): MutableMap<AbstractCommand, Long> {
        val map =  commandUsageDao.getUsageWithinPeriod(from, until)
        val sortedAndTopped = sortAndTopOfUsageMap(map, top)
        return mapKeysToAbstractCommand(sortedAndTopped)
    }

    private fun mapKeysToAbstractCommand(map: MutableMap<Int, Long>): MutableMap<AbstractCommand, Long> {
        val newMutableMap = mutableMapOf<AbstractCommand, Long>()
        for ((id, usageCount) in map) {
            val cmd = Container.instance.commandMap[id] ?: continue
            newMutableMap[cmd] = usageCount
        }
        return newMutableMap
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