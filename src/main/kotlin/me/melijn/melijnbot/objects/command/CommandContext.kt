package me.melijn.melijnbot.objects.command

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.music.GuildMusicPlayer
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class CommandContext(
    private val messageReceivedEvent: MessageReceivedEvent,
    val commandParts: List<String>,
    val container: Container,
    val commandList: Set<AbstractCommand>
) : ICommandContext {

    lateinit var logger: Logger

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
        rawArg = messageReceivedEvent.message.contentRaw
            .removePrefix(usedPrefix)
            .trim()

        for (i in 1..commandOrder.size) {
            rawArg = rawArg
                .removePrefix(commandParts[i])
                .trim()
        }


        //Quot arg support
        val rearg = rawArg.replace("\\s+".toRegex(), " ") + " " // this last space is needed so I don't need to hack around in the splitter for the last arg
        val quotationIndexes = mutableListOf<Int>()
        val slashIndexes = mutableListOf<Int>()
        val spaceIndexes = mutableListOf<Int>()

        for ((index, c) in rearg.toCharArray().withIndex().sortedBy { (i, _) -> i }) {
            when (c) {
                '"' -> quotationIndexes.add(index)
                '\\' -> slashIndexes.add(index)
                ' ' -> spaceIndexes.add(index)
            }
        }

        for (slashIndex in slashIndexes) {
            quotationIndexes.remove(slashIndex + 1)
        }

        //Loop through copy of quoteIndexes to prevent concurrentmodification
        for (quotIndex in ArrayList(quotationIndexes)) {
            //Check if the quote is valid as argument beginning or ending
            if (quotIndex != 0 && !spaceIndexes.contains(quotIndex - 1) && !spaceIndexes.contains(quotIndex + 1)) {
                //Remove if not
                quotationIndexes.remove(quotIndex)
            }
        }

        if (quotationIndexes.size % 2 == 1) {
            quotationIndexes.removeAt(quotationIndexes.size - 1)
        }

        val newCoolArgs = mutableListOf<String>()

        var lastIndex = 0
        for (spaceIndex in spaceIndexes) {
            var ignoreSpace = false
            for ((index, quotIndex) in quotationIndexes.withIndex()) { // Check if space is within quotes
                if (index % 2 == 0) {
                    val nextQuotIndex = quotationIndexes[index + 1]
                    if (spaceIndex in (quotIndex + 1) until nextQuotIndex) ignoreSpace = true
                }
            }
            if (!ignoreSpace) { // if space is not withing quotes
                val betterBegin = lastIndex + if (quotationIndexes.contains(lastIndex)) 1 else 0
                val betterEnd = spaceIndex - if (quotationIndexes.contains(spaceIndex - 1)) 1 else 0
                val extraArg = rearg
                    .substring(betterBegin, betterEnd) // don't include the " and the space in the arg
                    .replace("\\\"", "\"")

                if (extraArg.isNotBlank()) {
                    newCoolArgs.add(extraArg)
                }
                lastIndex = spaceIndex + 1
            }
        }

        args = newCoolArgs.toList()

        logger = LoggerFactory.getLogger(commandOrder.first().javaClass.name)
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

    fun reply(embed: MessageEmbed) {
        require(!(isFromGuild && !selfMember.hasPermission(textChannel, Permission.MESSAGE_WRITE))) {
            "No MESSAGE_WRITE permission"
        }
        messageChannel.sendMessage(embed).queue()
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

    suspend fun getTranslation(path: String): String = i18n.getTranslation(this, path)

    val guildMusicPlayer: GuildMusicPlayer
        get() = musicPlayerManager.getGuildMusicPlayer(guild)
}