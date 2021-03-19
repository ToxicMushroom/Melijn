package me.melijn.melijnbot.internals.threading

import kotlinx.coroutines.*
import me.melijn.melijnbot.internals.command.ICommandContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.*

object TaskManager {

    private val threadFactory = { name: String ->
        var counter = 0
        { r: Runnable ->
            Thread(r, "[$name-Pool-%d]".replace("%d", "${counter++}"))
        }
    }

    val executorService: ExecutorService = ForkJoinPool()
    private val dispatcher = executorService.asCoroutineDispatcher()
    val scheduledExecutorService: ScheduledExecutorService =
        Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))
    val coroutineScope = CoroutineScope(dispatcher)

    fun async(block: suspend CoroutineScope.() -> Unit): Job {

        return coroutineScope.launch {
            Task {
                block.invoke(this)
            }.run()
        }
    }

    fun asyncIgnoreEx(block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        try {
            block.invoke(this)
        } catch (t: Throwable) {
            // ignored by design
        }
    }

    fun <T> taskValueAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = coroutineScope.async {
        DeferredTask {
            block.invoke(this)
        }.run()
    }

    fun <T> taskValueNAsync(block: suspend CoroutineScope.() -> T?): Deferred<T?> = coroutineScope.async {
        DeferredNTask {
            block.invoke(this)
        }.run()
    }
    fun <T> evalTaskValueNAsync(block: suspend CoroutineScope.() -> T?): Deferred<T?> = coroutineScope.async {
        EvalDeferredNTask {
            block.invoke(this)
        }.run()
    }

    fun async(context: ICommandContext, block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        ContextTask(context) {
            block.invoke(this)
        }.run()
    }

    fun async(member: Member, block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        MemberTask(member) {
            block.invoke(this)
        }.run()
    }

    fun async(channel: MessageChannel, block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        ChannelTask(channel) {
            block.invoke(this)
        }.run()
    }

    fun async(guild: Guild, block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        GuildTask(guild) {
            block.invoke(this)
        }.run()
    }

    fun async(
        user: User,
        messageChannel: MessageChannel,
        block: suspend CoroutineScope.() -> Unit
    ) = coroutineScope.launch {
        UserChannelTask(user, messageChannel) {
            block.invoke(this)
        }.run()
    }

    fun async(user: User, guild: Guild, block: suspend CoroutineScope.() -> Unit) = coroutineScope.launch {
        UserGuildTask(user, guild) {
            block.invoke(this)
        }.run()
    }

    inline fun asyncInline(crossinline block: CoroutineScope.() -> Unit) = coroutineScope.launch {
        TaskInline {
            block.invoke(this)
        }.run()
    }

    inline fun asyncAfter(afterMillis: Long, crossinline func: suspend () -> Unit) {
        scheduledExecutorService.schedule(RunnableTask { func() }, afterMillis, TimeUnit.MILLISECONDS)
    }
}
