package me.melijn.melijnbot.internals.music.lavaimpl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.util.concurrent.*

/**
 * Wrapper for executor services which ensures that tasks with the same key are processed in order.
 */
class MelijnOrderedExecutor(delegateService: ExecutorService) {
    private val states: ConcurrentMap<Any, BlockingQueue<suspend () -> Unit>?>
    val coroutineScope = CoroutineScope(delegateService.asCoroutineDispatcher())

    /**
     * @param orderingKey Key for the ordering channel
     * @param func function to submit to the executor service
     * @return Future for the task
     */
    fun submit(orderingKey: Any, func: suspend () -> Unit) {
        queueOrSubmit(ChannelRunnable(orderingKey), func)
    }

    private fun queueOrSubmit(runnable: ChannelRunnable, func: suspend () -> Unit) {
        val newQueue: BlockingQueue<suspend () -> Unit> = LinkedBlockingQueue()
        newQueue.add(func)
        val existing = states.putIfAbsent(runnable.key, newQueue)
        if (existing != null) {
            existing.add(func)
            if (states.putIfAbsent(runnable.key, existing) == null) {
                coroutineScope.launch { ChannelRunnable(runnable.key) }
            }
        } else {
            coroutineScope.launch { runnable.run() }
        }
    }

    private fun <T> newTaskFor(runnable: Runnable, value: T): RunnableFuture<T> {
        return FutureTask(runnable, value)
    }

    private fun <T> newTaskFor(callable: Callable<T>): RunnableFuture<T> {
        return FutureTask(callable)
    }

    private inner class ChannelRunnable(val key: Any) {
        suspend fun run() {
            val queue = states[key]
            queue?.let { executeQueue(it) }
        }

        private suspend fun executeQueue(queue: BlockingQueue<suspend () -> Unit>) {
            var next: (suspend () -> Unit)?

            do {
                next = queue.poll()
                if (next == null) break

                var finished = false
                finished = try {
                    next.invoke()
                    true
                } finally {
                    if (!finished) {
                        coroutineScope.launch { ChannelRunnable(key).run() }
                    }
                }
            } while (true)

            states.remove(key, queue)
        }

    }

    init {
        states = ConcurrentHashMap()
    }
}
