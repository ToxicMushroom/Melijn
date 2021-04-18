package me.melijn.melijnbot.internals.models

data class PodInfo(
    val podId: Int,
    val podCount: Int,
    val shardCount: Int
) {
    val shardsPerPod: Int = shardCount / podCount
    val minShardId: Int = podId * shardsPerPod
    val maxShardId: Int = podId * shardsPerPod + shardsPerPod - 1
    val shardList: List<Int> = List(shardsPerPod) { index -> index + minShardId }
}