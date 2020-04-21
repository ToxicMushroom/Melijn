package me.melijn.melijnbot.database.role

import com.google.common.cache.CacheBuilder
import me.melijn.melijnbot.database.NOT_IMPORTANT_CACHE
import me.melijn.melijnbot.objects.threading.TaskManager
import me.melijn.melijnbot.objects.utils.loadingCacheFrom
import net.dv8tion.jda.api.utils.data.DataArray
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class JoinRoleWrapper(val taskManager: TaskManager, private val joinRoleDao: JoinRoleDao) {

    // guildId -> <selfRoleGroupName -> emotejiInfo (see SelfRoleDao for example)
    val selfRoleCache = CacheBuilder.newBuilder()
        .expireAfterAccess(NOT_IMPORTANT_CACHE, TimeUnit.MINUTES)
        .build(loadingCacheFrom<Long, JoinRoleInfo> { key ->
            getMap(key)
        })

    fun getMap(guildId: Long): CompletableFuture<JoinRoleInfo> {
        val future = CompletableFuture<JoinRoleInfo>()
        taskManager.async {
            val map = joinRoleDao.get(guildId)
            val info = convertToJoinRoleInfo(map)
            future.complete(info)
        }
        return future
    }

    private fun convertToJoinRoleInfo(fromString: String): JoinRoleInfo {
        val internalMap = mutableMapOf<String, List<JoinRoleInfo.JoinRoleEntry>>()
        val dataArray = DataArray.fromJson(fromString)
        for (i in 0 until dataArray.length()) {
            val dataEntry = dataArray.getArray(i)
            

        }
        

        return JoinRoleInfo(internalMap)
    }
}

data class JoinRoleInfo(
    var list: Map<String, List<JoinRoleEntry>>
) {

    data class JoinRoleEntry(
        var roleId: Long,
        var chance: Int
    )
}