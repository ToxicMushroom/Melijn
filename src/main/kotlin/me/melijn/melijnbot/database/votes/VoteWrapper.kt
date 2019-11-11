package me.melijn.melijnbot.database.votes

class VoteWrapper(private val voteDao: VoteDao) {
    suspend fun getUserVote(userId: Long): UserVote? {
        return voteDao.getVotesObject(userId)
    }
}