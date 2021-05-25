package me.melijn.melijnbot.internals.models

object PodInfo {

    fun init(podCount: Int, shardCount: Int, podId: Int) {
        this.podId = podId
        this.podCount = podCount
        this.shardCount = shardCount
        this.shardsPerPod = shardCount / podCount
        this.minShardId = podId * shardsPerPod
        this.maxShardId = podId * shardsPerPod + shardsPerPod - 1
        this.shardList =  List(shardsPerPod) { index -> index + minShardId }
    }

    var podId: Int = 0
    var podCount: Int = 1
    var shardCount: Int = 0

    var shardsPerPod: Int = 1
    var minShardId: Int = 0
    var maxShardId: Int = 0
    var shardList: List<Int> = List(shardsPerPod) { index -> index + minShardId }

}