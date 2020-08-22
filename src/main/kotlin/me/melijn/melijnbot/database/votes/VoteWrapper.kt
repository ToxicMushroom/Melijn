package me.melijn.melijnbot.database.votes

class VoteWrapper(private val voteDao: VoteDao) {

    suspend fun getUserVote(userId: Long): UserVote? {
        return voteDao.getVotesObject(userId)
    }

    fun setVote(userId: Long, votes: Long, streak: Long, lastTime: Long) {
        voteDao.set(userId, votes, streak, lastTime)
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> {
        return voteDao.getTop(users, offset)
    }

    suspend fun getRowCount(): Long {
        return voteDao.getRowCount()
    }

}