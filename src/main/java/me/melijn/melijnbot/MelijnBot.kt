package me.melijn.melijnbot

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandClientBuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.reflections.Reflections
import java.util.*


class MelijnBot {

    private var instance: MelijnBot = this

    companion object {
        var shardManager: ShardManager? = null
    }

    init {
        Locale.setDefault(Locale.ENGLISH)
        val container = Container()

        val commandClientBuilder = CommandClientBuilder(container)
        loadCommands(commandClientBuilder)

        val commandClient = commandClientBuilder.build()

        shardManager = DefaultShardManagerBuilder()
                .setShardsTotal(container.settings.shardCount)
                .setToken(container.settings.tokens.melijn)
                .setActivity(Activity.listening("commands | >help"))
                .setAutoReconnect(true)
                .addEventListeners(commandClient)
                .build()


    }

    fun getInstance(): MelijnBot? {
        return instance
    }

    private fun loadCommands(client: CommandClientBuilder) {
        val reflections = Reflections("me.melijn.melijnbot.commands")

        val commands = reflections.getSubTypesOf(AbstractCommand::class.java)
        commands.forEach { command ->
            try {
                if (!command.isMemberClass) {
                    val cmd = command.getDeclaredConstructor().newInstance()
                    client.addCommand(cmd)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}

fun main(args: Array<String>) {
    MelijnBot()
}