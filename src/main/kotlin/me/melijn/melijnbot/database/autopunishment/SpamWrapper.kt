package me.melijn.melijnbot.database.autopunishment

class SpamWrapper(private val spamDao: SpamDao) {

    //guildId, spamGroupName, spamInfo
//    val fastMessageCache = CacheBuilder.newBuilder()
//        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
//        .build(loadingCacheFrom<Pair<Long, String>, FastMessageInfo> { (first, second) ->
//            getSpamsFastMessage(first, second)
//        })
//
//    val manyCapsCache = CacheBuilder.newBuilder()
//        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
//        .build(loadingCacheFrom<Pair<Long, String>, ManyCapsInfo> { (first, second) ->
//            getSpamsManyCaps(first, second)
//        })
//
//    val manyMentionsCache = CacheBuilder.newBuilder()
//        .expireAfterAccess(IMPORTANT_CACHE, TimeUnit.MINUTES)
//        .build(loadingCacheFrom<Pair<Long, String>, ManyMentionsInfo> { (first, second) ->
//            getSpamsManyMentions(first, second)
//        })
//
//    private fun getSpamsManyMentions(guildId: Long, spamGroupName: String): CompletableFuture<ManyMentionsInfo> {
//        return getSpams(guildId, spamGroupName, SpamType.MANY_MENTIONS) {
//            try {
//                objectMapper.readValue(it, ManyMentionsInfo::class.java)
//            } catch (t: Throwable) {
//                ManyMentionsInfo(0, true)
//            }
//        }
//    }
//
//    private fun getSpamsManyCaps(guildId: Long, spamGroupName: String): CompletableFuture<ManyCapsInfo> {
//        return getSpams(guildId, spamGroupName, SpamType.MANY_CAPS) {
//            try {
//                objectMapper.readValue(it, ManyCapsInfo::class.java)
//            } catch (t: Throwable) {
//                ManyCapsInfo(0, 0, 0)
//            }
//        }
//    }
//
//    private fun getSpamsFastMessage(guildId: Long, spamGroupName: String): CompletableFuture<FastMessageInfo> {
//        return getSpams(guildId, spamGroupName, SpamType.FAST_MESSAGE) {
//            try {
//                objectMapper.readValue(it, FastMessageInfo::class.java)
//            } catch (t: Throwable) {
//                FastMessageInfo(0, 0, 0)
//            }
//        }
//    }
//
//    private fun <T> getSpams(guildId: Long, spamGroupName: String, type: SpamType, func: (String) -> T): CompletableFuture<T> {
//        val future = CompletableFuture<T>()
//        TaskManager.async {
//            val filters = spamDao.get(guildId, spamGroupName, type)
//            future.complete(func(filters))
//        }
//        return future
//    }
//
//    suspend fun setManyCapsInfo(guildId: Long, spamGroupName: String, manyCapsInfo: ManyCapsInfo) {
//        spamDao.add(guildId, spamGroupName, SpamType.MANY_CAPS, objectMapper.writeValueAsString(manyCapsInfo))
//
//        val pair = Pair(guildId, spamGroupName)
//        manyCapsCache.put(pair, CompletableFuture.completedFuture(manyCapsInfo))
//    }
//
//    suspend fun removeManyCapsInfo(guildId: Long, spamGroupName: String) {
//        spamDao.remove(guildId, spamGroupName, SpamType.MANY_CAPS)
//
//        val pair = Pair(guildId, spamGroupName)
//        manyCapsCache.invalidate(pair)
//    }
//
//    suspend fun setManyMentionsInfo(guildId: Long, spamGroupName: String, manyMentionsInfo: ManyMentionsInfo) {
//        spamDao.add(guildId, spamGroupName, SpamType.FAST_MESSAGE, objectMapper.writeValueAsString(manyMentionsInfo))
//
//        val pair = Pair(guildId, spamGroupName)
//        manyMentionsCache.put(pair, CompletableFuture.completedFuture(manyMentionsInfo))
//    }
//
//    suspend fun removeManyMentionsInfo(guildId: Long, spamGroupName: String) {
//        spamDao.remove(guildId, spamGroupName, SpamType.FAST_MESSAGE)
//
//        val pair = Pair(guildId, spamGroupName)
//        fastMessageCache.invalidate(pair)
//    }
//
//    suspend fun setFastMessageInfo(guildId: Long, spamGroupName: String, fastMessageInfo: FastMessageInfo) {
//        spamDao.add(guildId, spamGroupName, SpamType.FAST_MESSAGE, objectMapper.writeValueAsString(fastMessageInfo))
//
//        val pair = Pair(guildId, spamGroupName)
//        fastMessageCache.put(pair, CompletableFuture.completedFuture(fastMessageInfo))
//    }
//
//    suspend fun removeFastMessageInfo(guildId: Long, spamGroupName: String) {
//        spamDao.remove(guildId, spamGroupName, SpamType.FAST_MESSAGE)
//
//        val pair = Pair(guildId, spamGroupName)
//        fastMessageCache.invalidate(pair)
//    }
}

data class FastMessageInfo(
    val messageAmount: Int,
    val startTime: Int,
    val endTime: Int
)

data class ManyCapsInfo(
    val capsLimit: Int, // ex: 50 (over 50 caps gets removed)
    val percentLimit: Int, // ex: 40% (over 40% gets relived)
    val minPercentActivation: Int // ex: 4 (so 3 caps are always allowed)
)

data class ManyMentionsInfo(
    val mentionLimit: Int, // ex: 50 (over 50 mentions gets removed)
    val onlyCountUnique: Boolean // ex: true only counts unique pings
)
