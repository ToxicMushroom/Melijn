package me.melijn.melijnbot.database.votes

class VoteWrapper(private val voteDao: VoteDao) {
    suspend fun getUserVote(userId: Long): UserVote? {
        return voteDao.getVotesObject(userId)
    }

    suspend fun setVote(userId: Long, votes: Long, streak: Long, lastTime: Long) {
        voteDao.set(userId, votes, streak, lastTime)
    }
}