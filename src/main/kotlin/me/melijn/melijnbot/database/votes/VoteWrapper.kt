package me.melijn.melijnbot.database.votes

class VoteWrapper(private val voteDao: VoteDao) {

    suspend fun getUserVote(userId: Long): UserVote? {
        return voteDao.getVotesObject(userId)
    }

    fun setVote(userVote: UserVote) {
        voteDao.set(userVote)
    }

    suspend fun getTop(users: Int, offset: Int): Map<Long, Long> {
        return voteDao.getTop(users, offset)
    }

    suspend fun getRowCount(): Long {
        return voteDao.getRowCount()
    }

    suspend fun getPosition(userId: Long): Pair<Long, Long> {
        return voteDao.getPosition(userId)
    }

}