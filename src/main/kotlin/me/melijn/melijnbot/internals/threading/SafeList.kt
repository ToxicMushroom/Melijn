package me.melijn.melijnbot.internals.threading

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SafeList<E> {

    val lock = Mutex()
    private val list = ArrayList<E>()

    val size: Int
        get() = list.size

    fun isEmpty(): Boolean = list.isEmpty()

    suspend fun get(index: Int): E = lock.withLock {
        return list[index] ?: throw IndexOutOfBoundsException()
    }

    suspend fun getOrNull(index: Int): E? = lock.withLock {
        return list[index]
    }

    suspend fun add(element: E): Boolean = lock.withLock {
        return list.add(element)
    }

    suspend fun add(index: Int, element: E) = lock.withLock {
        list.add(index, element)
    }

    suspend fun removeAt(index: Int): E = lock.withLock {
        return list.removeAt(index)
    }

    suspend fun removeAtOrNull(index: Int): E? = lock.withLock {
        return if (list.size > index) {
            list.removeAt(index)
        } else {
            null
        }
    }

    suspend fun remove(element: E) = lock.withLock {
        list.remove(element)
    }

    suspend fun shuffle() = lock.withLock {
        list.shuffle()
    }

    suspend fun clear() = lock.withLock {
        list.clear()
    }

    suspend fun removeFirstAndGetNextOrNull(amount: Int): E? = lock.withLock {
        for (i in 0 until (amount - 1)) {
            if (list.size < 1) break
            else list.removeAt(0)
        }
        return list.removeFirstOrNull()
    }

    suspend fun forEach(function: suspend (E) -> Unit) = lock.withLock {
        val size = list.size
        for (i in 0 until size) {
            function.invoke(list[i])
        }
    }

    suspend fun indexedForEach(function: (Int, E) -> Unit) = lock.withLock {
        val size = list.size
        for (i in 0 until size) {
            function.invoke(i, list[i])
        }
    }

    suspend fun indexOf(audioTrack: E): Int = lock.withLock {
        return list.indexOf(audioTrack)
    }

    suspend fun any(function: (E) -> Boolean): Boolean = lock.withLock {
        return list.any(function)
    }

    suspend fun firstOrNull(function: (E) -> Boolean): E? = lock.withLock {
        return list.firstOrNull(function)
    }

    suspend fun removeAll(toRemove: List<E>) = lock.withLock {
        list.removeAll(toRemove)
    }
}