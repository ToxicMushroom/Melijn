package me.melijn.melijnbot.internals.jda

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.database.ratelimit.RatelimitWrapper
import net.dv8tion.jda.api.utils.SessionControllerAdapter

class MelijnSessionController(private val ratelimitWrapper: RatelimitWrapper) : SessionControllerAdapter() {

    var internalRatelimit = 0L

    init {
        setConcurrency(16)
    }

    override fun getGlobalRatelimit(): Long {
        return runBlocking {
            try {
                ratelimitWrapper.get() ?: internalRatelimit
            } catch (t: Throwable) {
                internalRatelimit
            }
        }
    }

    override fun setGlobalRatelimit(ratelimit: Long) {
        return runBlocking {
            internalRatelimit = ratelimit
            try {
                ratelimitWrapper.set(ratelimit)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}