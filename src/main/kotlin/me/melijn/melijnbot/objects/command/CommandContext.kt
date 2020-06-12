package me.melijn.melijnbot.objects.command

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.objects.music.GuildMusicPlayer
import me.melijn.melijnbot.objects.translation.i18n
import me.melijn.melijnbot.objects.utils.USER_MENTION
import me.melijn.melijnbot.objects.utils.removeFirst
import me.melijn.melijnbot.objects.utils.removePrefix
import me.melijn.melijnbot.objects.web.WebManager
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.util.regex.Pattern

class CommandContext(
    private val messageReceivedEvent: MessageReceivedEvent,
    val commandParts: List<String>, // rawArg split on spaces and prefix, index 0 = prefix, next index will be (part of) command invoke...
    val container: Container,
    val commandList: Set<AbstractCommand>, // Just the total list of commands
    val partSpaceMap: MutableMap<String, Int>, // Only tracks spaces in the command's invoke (string is the path 45.perm.user.set or cc.101)
    val aliasMap: MutableMap<String, List<String>>, // cmd.subcommand... -> list of aliases
    var searchedAliases: Boolean,
    private val contentRaw: String = messageReceivedEvent.message.contentRaw
) : ICommandContext {

    lateinit var logger: Logger

    override val event: MessageReceivedEvent
        get() = messageReceivedEvent

    override val guild: Guild
        get() = event.guild

    val guildN: Guild?
        get() = if (isFromGuild) guild else null


    val webManager: WebManager = container.webManager

    val usedPrefix: String = getNicerUsedPrefix()
    val mentionOffset: Int = retrieveOffset()

    private fun getNicerUsedPrefix(): String {
        val prefix = commandParts[0]
        return if (prefix.contains(container.settings.id.toString()) && USER_MENTION.matcher(prefix).matches()) {
            "@${container.settings.name} "
        } else {
            prefix
        }
    }

    val embedColor: Int = container.settings.embedColor
    val prefix: String = container.settings.prefix
    var commandOrder: List<AbstractCommand> = emptyList()
    var args: List<String> = emptyList()
    var oldArgs: List<String> = emptyList()
    val botDevIds: LongArray = container.settings.developerIds
    val daoManager = container.daoManager
    val taskManager = container.taskManager
    var rawArg: String = ""
    val contextTime = System.currentTimeMillis()
    val lavaManager = container.lavaManager
    val musicPlayerManager = container.lavaManager.musicPlayerManager
    val audioLoader = container.lavaManager.musicPlayerManager.audioLoader

    var calculatedRoot = ""
    var calculatedCommandPartsOffset = 1

    fun initArgs() {
        args = commandParts.drop(calculatedCommandPartsOffset)
        rawArg = contentRaw
            .removePrefix(commandParts[0], true)
            .trim()

        for (i in 1 until calculatedCommandPartsOffset) {
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
        oldArgs = if (rawArg.isNotBlank()) {
            rawArg.split("\\s+".toRegex())
        } else {
            emptyList()
        }
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
        if (beginIndex > oldArgs.size) return ""
        var newString = rawArg
        for (i in 0 until beginIndex) {
            newString = newString.removeFirst(oldArgs[i]).trim()
        }


        if (endIndex != -1 && endIndex < oldArgs.size) {
            for (i in endIndex until oldArgs.size) {
                newString = newString.removeSuffix(oldArgs[i]).trim()
            }
        }

        return newString
    }

    suspend fun getTranslation(path: String): String = i18n.getTranslation(this, path)
    suspend fun getTimeZoneId(): ZoneId {
        val guildTimezone = guildId.let {
            val zoneId = daoManager.timeZoneWrapper.timeZoneCache.get(it).await()
            if (zoneId?.isBlank() == true) null
            else ZoneId.of(zoneId)
        }

        val userTimezone = authorId.let {
            val zoneId = daoManager.timeZoneWrapper.timeZoneCache.get(it).await()
            if (zoneId?.isBlank() == true) null
            else ZoneId.of(zoneId)
        }

        return userTimezone ?: guildTimezone ?: ZoneId.of("GMT")
    }

    val guildMusicPlayer: GuildMusicPlayer
        get() = musicPlayerManager.getGuildMusicPlayer(guild)
}