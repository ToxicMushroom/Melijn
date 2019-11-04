package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.music.GuildMusicPlayer
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Pattern

class CommandContext(
    private val messageReceivedEvent: MessageReceivedEvent,
    val commandParts: List<String>,
    val container: Container,
    val commandList: Set<AbstractCommand>
) : ICommandContext {

    override val event: MessageReceivedEvent
        get() = messageReceivedEvent

    override val guild: Guild
        get() = event.guild

    val guildN: Guild?
        get() = if (isFromGuild) guild else null


    val webManager: WebManager = container.webManager
    val usedPrefix: String = commandParts[0]
    val mentionOffset: Int = retrieveOffset()
    val embedColor: Int = container.settings.embedColor
    val prefix: String = container.settings.prefix
    var commandOrder: List<AbstractCommand> = emptyList()
    var args: List<String> = emptyList()
    val botDevIds: LongArray = container.settings.developerIds
    val daoManager = container.daoManager
    val taskManager = container.taskManager
    var rawArg: String = ""
    val contextTime = System.currentTimeMillis()
    val lavaManager = container.lavaManager
    val musicPlayerManager = container.lavaManager.musicPlayerManager
    val audioLoader = container.lavaManager.musicPlayerManager.audioLoader

    fun initArgs() {
        args = commandParts.drop(1 + commandOrder.size)
        var commandPath = ""
        for (i in 1..commandOrder.size) {
            commandPath += Pattern.quote(commandParts[i])
            commandPath += if (i == commandOrder.size) {
                if (args.isEmpty()) {
                    "\\s*"
                } else {
                    "\\s+"
                }
            } else {
                "\\s+"
            }
        }

        val regex: Regex = ("${Pattern.quote(usedPrefix)}\\s*$commandPath").toRegex()
        rawArg = messageReceivedEvent.message.contentRaw.replaceFirst(regex, "")
    }

    private fun retrieveOffset(): Int {
        var count = 0
        val pattern = Pattern.compile("<@!?(\\d+)>")
        val matcher = pattern.matcher(commandParts[0])

        while (matcher.find()) {
            val str = matcher.group(1)
            if (jda.shardManager?.getUserById(str) != null) count++
        }

        return count
    }

    fun reply(something: Any) {
        require(!(isFromGuild && !selfMember.hasPermission(textChannel, Permission.MESSAGE_WRITE))) {
            "No MESSAGE_WRITE permission"
        }
        messageChannel.sendMessage(something.toString()).queue()
    }

    suspend fun getLanguage(): String = me.melijn.melijnbot.objects.translation.getLanguage(this)


    //Gets part of the rawarg by using regex and args
    fun getRawArgPart(beginIndex: Int, endIndex: Int = -1): String {
        var stringPatternForRemoval = ""
        for (i in 0 until beginIndex) {
            stringPatternForRemoval += "${args[i]}\\s*"
        }
        var patternForRemoval = stringPatternForRemoval.toRegex()
        rawArg.replace(patternForRemoval, "")

        stringPatternForRemoval = ""
        if (endIndex != -1) {
            for (i in endIndex until args.size) {
                stringPatternForRemoval += "${args[i]}\\s*"
            }
        }

        patternForRemoval = stringPatternForRemoval.toRegex()
        rawArg.replace(patternForRemoval, "")
        return rawArg
    }

    val guildMusicPlayer: GuildMusicPlayer
        get() = musicPlayerManager.getGuildMusicPlayer(guild)
}