package me.melijn.melijnbot.internals

import com.sun.management.OperatingSystemMXBean
import me.melijn.melijnbot.internals.utils.OSValidator
import me.melijn.melijnbot.internals.utils.getTotalMBUnixRam
import me.melijn.melijnbot.internals.utils.getUsedMBUnixRam
import java.lang.management.ManagementFactory

data class JvmUsage(val totalMem: Long, val usedMem: Long, val totalJVMMem: Long, val usedJVMMem: Long) {
    companion object {
        fun current(bean: OperatingSystemMXBean): JvmUsage {
            val totalMem: Long
            val usedMem: Long
            if (OSValidator.isUnix) {
                totalMem = getTotalMBUnixRam()
                usedMem = getUsedMBUnixRam()
            } else {
                totalMem = bean.totalMemorySize shr 20
                usedMem = totalMem - (bean.freeSwapSpaceSize shr 20)
            }

            val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
            val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20

            return JvmUsage(totalMem, usedMem, totalJVMMem, usedJVMMem)
        }
    }
}
