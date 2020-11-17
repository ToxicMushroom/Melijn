package me.melijn.melijnbot.internals.threading

import kotlinx.coroutines.*
import me.melijn.melijnbot.internals.command.CommandContext
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

object TaskManager {

    private val threadFactory = { name: String ->
        var counter = 0
        { r: Runnable ->
            Thread(r, "[$name-Pool-%d]".replace("%d", "${counter++}"))
        }
    }

    val executorService: ExecutorService = Executors.newCachedThreadPool(threadFactory.invoke("Task"))
    val dispatcher = executorService.asCoroutineDispatcher()
    val scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(15, threadFactory.invoke("Repeater"))

    fun async(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        Task {
            block.invoke(this)
        }.run()
    }

    fun asyncIgnoreEx(block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        try {
            block.invoke(this)
        } catch (t: Throwable) {
            // ignored by design
        }
    }

    fun <T> taskValueAsync(block: suspend CoroutineScope.() -> T): Deferred<T> = CoroutineScope(dispatcher).async {
        DeferredTask {
            block.invoke(this)
        }.run()
    }

    fun <T> taskValueNAsync(block: suspend CoroutineScope.() -> T?): Deferred<T?> = CoroutineScope(dispatcher).async {
        DeferredNTask {
            block.invoke(this)
        }.run()
    }

    fun async(context: CommandContext, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        ContextTask(context) {
            block.invoke(this)
        }.run()
    }

    fun async(member: Member, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        MemberTask(member) {
            block.invoke(this)
        }.run()
    }

    fun async(channel: MessageChannel, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        ChannelTask(channel) {
            block.invoke(this)
        }.run()
    }

    fun async(guild: Guild, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        GuildTask(guild) {
            block.invoke(this)
        }.run()
    }

    fun async(user: User, messageChannel: MessageChannel, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        UserChannelTask(user, messageChannel) {
            block.invoke(this)
        }.run()
    }

    fun async(user: User, guild: Guild, block: suspend CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        UserGuildTask(user, guild) {
            block.invoke(this)
        }.run()
    }

    inline fun asyncInline(crossinline block: CoroutineScope.() -> Unit) = CoroutineScope(dispatcher).launch {
        TaskInline {
            block.invoke(this)
        }.run()
    }

    inline fun asyncAfter(afterMillis: Long, crossinline func: suspend () -> Unit) {
        scheduledExecutorService.schedule(RunnableTask { func() }, afterMillis, TimeUnit.MILLISECONDS)
    }
}
