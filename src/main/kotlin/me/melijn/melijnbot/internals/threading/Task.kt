package me.melijn.melijnbot.internals.threading

import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendInGuild
import net.dv8tion.jda.api.entities.*

class Task(private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild()
        }
    }
}

class DeferredNTask<T>(private val func: suspend () -> T?) : DeferredNKTRunnable<T> {

    override suspend fun run(): T? {
        return try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild()
            null
        }
    }
}

class EvalDeferredNTask<T>(private val func: suspend () -> T?) : DeferredNKTRunnable<T> {

    override suspend fun run(): T? {
        return func()
    }
}

class DeferredTask<T>(private val func: suspend () -> T) : DeferredKTRunnable<T> {

    override suspend fun run(): T {
        return func()
    }
}

class MemberTask(private val member: Member, private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild(member.guild, null, member.user)
        }
    }
}

class ChannelTask(private val messageChannel: MessageChannel, private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            if (messageChannel is TextChannel) {
                e.sendInGuild(messageChannel.guild, messageChannel)
            } else {
                e.sendInGuild(null, messageChannel)
            }
        }
    }
}

class GuildTask(private val guild: Guild, private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild(guild)
        }
    }
}

class ContextTask(private val context: ICommandContext, private val func: suspend () -> Unit) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild(context)
        }
    }
}

class UserChannelTask(
    private val user: User,
    private val messageChannel: MessageChannel,
    private val func: suspend () -> Unit
) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            if (messageChannel is TextChannel) {
                e.sendInGuild(messageChannel.guild, messageChannel, user)
            } else {
                e.sendInGuild(null, messageChannel, user)
            }
        }
    }
}

class UserGuildTask(
    private val user: User,
    private val guild: Guild,
    private val func: suspend () -> Unit
) : KTRunnable {

    override suspend fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild(guild, null, user)
        }
    }
}

class RunnableTask(private val func: suspend () -> Unit) : Runnable {

    override fun run() {
        TaskManager.async {
            func()
        }
    }
}

class TaskInline(private inline val func: () -> Unit) : Runnable {

    override fun run() {
        try {
            func()
        } catch (e: Throwable) {
            e.printStackTrace()
            e.sendInGuild()
        }
    }
}

@FunctionalInterface
interface KTRunnable {
    suspend fun run()
}

@FunctionalInterface
interface DeferredNKTRunnable<T> {
    suspend fun run(): T?
}

@FunctionalInterface
interface DeferredKTRunnable<T> {
    suspend fun run(): T
}