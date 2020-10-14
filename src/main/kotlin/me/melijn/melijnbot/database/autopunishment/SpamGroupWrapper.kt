package me.melijn.melijnbot.database.autopunishment

class SpamGroupWrapper(private val spamGroupDao: SpamGroupDao) {
//
//    val spamGroupCache = CacheBuilder.newBuilder()
//        .expireAfterAccess(FREQUENTLY_USED_CACHE, TimeUnit.MINUTES)
//        .build(loadingCacheFrom<Long, List<SpamGroup>> { guildId ->
//            getGroups(guildId)
//        })
//
//    private fun getGroups(guildId: Long): CompletableFuture<List<SpamGroup>> {
//        val future = CompletableFuture<List<SpamGroup>>()
//        TaskManager.async {
//            val mode = spamGroupDao.get(guildId)
//            future.complete(mode)
//        }
//        return future
//    }
//
//    suspend fun putGroup(guildId: Long, group: SpamGroup) {
//        val list = spamGroupCache.get(guildId).await().toMutableList()
//        list.removeIf { (groupId) -> groupId == group.spamGroupName }
//        list.add(group)
//        spamGroupCache.put(guildId, CompletableFuture.completedFuture(list))
//        spamGroupDao.add(guildId, group)
//    }
//
//    suspend fun deleteGroup(guildId: Long, group: SpamGroup) {
//        val list = spamGroupCache.get(guildId).await().toMutableList()
//        list.removeIf { (groupId) -> groupId == group.spamGroupName }
//        spamGroupCache.put(guildId, CompletableFuture.completedFuture(list))
//        spamGroupDao.remove(guildId, group)
//    }
}