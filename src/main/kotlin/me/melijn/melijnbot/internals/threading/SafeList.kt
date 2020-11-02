package me.melijn.melijnbot.internals.threading

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SafeList<E> : MutableIterable<E> {

    val lock = Mutex()
    private val list = ArrayList<E>()

    val size: Int
        get() = list.size

    fun isEmpty(): Boolean {
        return list.isEmpty()
    }

    suspend fun get(index: Int): E {
        lock.withLock {
            return list[index] ?: throw IndexOutOfBoundsException()
        }
    }

    suspend fun getOrNull(index: Int): E? {
        lock.withLock {
            return list[index]
        }
    }

    suspend fun add(element: E): Boolean {
        lock.withLock {
            return list.add(element)
        }
    }

    suspend fun add(index: Int, element: E) {
        lock.withLock {
            list.add(index, element)
        }
    }

    suspend fun removeAt(index: Int): E {
        lock.withLock {
            return list.removeAt(index)
        }
    }

    suspend fun removeAtOrNull(index: Int): E? {
        lock.withLock {
            return if (list.size > index) {
                list.removeAt(index)
            } else {
                null
            }
        }
    }


    suspend fun remove(element: E) {
        lock.withLock {
            list.remove(element)
        }
    }

    // WARNING: iterating doesnt lock the list but you can access the lock and lock it yourself if required
    override fun iterator(): MutableIterator<E> {
        return list.iterator()
    }

    suspend fun shuffle() {
        lock.withLock {
            list.shuffle()
        }
    }

    suspend fun clear() {
        lock.withLock {
            list.clear()
        }
    }

    suspend fun removeFirstAndGetNextOrNull(amount: Int): E? {
        lock.withLock {
            for (i in 0 until (amount - 1)) {
                if (list.size < 1) break
                else list.removeAt(0)
            }
            return list.removeFirstOrNull()
        }
    }


}