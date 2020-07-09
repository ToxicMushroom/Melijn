//package me.melijn.melijnbot.objects.services.memspam
//
//import me.melijn.melijnbot.objects.services.Service
//import me.melijn.melijnbot.objects.threading.Task
//import java.lang.management.ManagementFactory
//import java.util.concurrent.TimeUnit
//
//class MemSpammerService : Service("mem", 50, 1, TimeUnit.MILLISECONDS) {
//    override val service: Task = Task {
//        val totalJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.max shr 20
//        val usedJVMMem = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used shr 20
//        println("$usedJVMMem/$totalJVMMem")
//    }
//}