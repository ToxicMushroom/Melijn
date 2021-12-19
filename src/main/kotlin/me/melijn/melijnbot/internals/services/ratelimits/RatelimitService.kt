package me.melijn.melijnbot.internals.services.ratelimits

import kotlinx.coroutines.sync.withLock
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.models.PodInfo
import me.melijn.melijnbot.internals.services.Service
import me.melijn.melijnbot.internals.threading.RunnableTask
import me.melijn.melijnbot.internals.web.rest.KillerInterceptor
import net.dv8tion.jda.api.sharding.ShardManager

class RatelimitService(val shardManager: ShardManager) : Service("ratelimit-handler", 60, 60) {

    var errorLimit = 2000 / PodInfo.podCount / 60

    companion object {
        var lastMinuteErrorCounts = mutableMapOf<Int, Int>()
        var lastMinuteRouteErrorCounts = mutableMapOf<String, Map<Int, Int>>()
    }

    override val service: RunnableTask = RunnableTask {
        KillerInterceptor.mutex.withLock {
//            val errorCount = KillerInterceptor.errorCounters.entries.filter { it.key >= 400 }.sumOf { it.value }
//            val successCount = KillerInterceptor.errorCounters.entries.filter { it.key < 400 }.sumOf { it.value }
//            if (errorCount > errorLimit) {
//                ratelimitOn(errorCount, successCount)
//                TaskManager.asyncAfter(15_000) { ratelimitOff(errorCount, successCount) }
//            } else if (Container.instance.ratelimiting) {
//                ratelimitOff(errorCount, successCount)
//            }

            lastMinuteErrorCounts = HashMap(KillerInterceptor.errorCounters)
            lastMinuteRouteErrorCounts = HashMap(KillerInterceptor.map)
            KillerInterceptor.errorCounters.clear()
            KillerInterceptor.map.clear()
        }
    }

    private fun ratelimitOn(errorCount: Int, successCount: Int) {
        Container.instance.ratelimiting = true
        logger.error("lots of errors! errors: ${errorCount}, success: $successCount, limit: ${errorLimit}/minute")
    }

    private fun ratelimitOff(errorCount: Int, successCount: Int) {
        Container.instance.ratelimiting = false
        logger.warn("switching rate-limiting off. errors: ${errorCount}, success: $successCount, limit: ${errorLimit}/minute")
    }
}