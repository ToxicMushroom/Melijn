package me.melijn.melijnbot.internals.command

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.SPACE_PATTERN
import me.melijn.melijnbot.internals.utils.USER_MENTION
import me.melijn.melijnbot.internals.utils.removeFirst
import me.melijn.melijnbot.internals.utils.removePrefix
import me.melijn.melijnbot.internals.web.WebManager
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.util.*

class CommandContext(
    override val message: Message,
    override val commandParts: List<String>, // rawArg split on spaces and prefix, index 0 = prefix, next index will be (part of) command invoke...
    override val container: Container,
    override val commandList: Set<AbstractCommand>, // Just the total list of commands
    override val partSpaceMap: MutableMap<String, Int>, // Only tracks spaces in the command's invoke (string is the path 45.perm.user.set or cc.101)
    override val aliasMap: MutableMap<String, List<String>>, // cmd.subcommand... -> list of aliases
    override var searchedAliases: Boolean,
    private val contentRaw: String = message.contentRaw
) : ICommandContext {

    lateinit var logger: Logger

    override val guild: Guild
        get() = message.guild

    override val guildN: Guild? = if (message.isFromGuild) guild else null
    override val isFromGuild: Boolean = message.isFromGuild

    override val author: User = message.author
    override val member: Member
        get() = message.member ?: throw IllegalStateException("Command not execute in guild")

    override val jda: JDA = message.jda

    override val textChannel: TextChannel
        get() = message.textChannel
    override val privateChannel: PrivateChannel
        get() = message.privateChannel
    override val channel: MessageChannel = message.channel
    override val channelId: Long = channel.idLong

    override val webManager: WebManager = container.webManager
    override val usedPrefix: String = getNicerUsedPrefix()

    private fun getNicerUsedPrefix(): String {
        val prefix = commandParts[0]
        return if (prefix.contains(jda.selfUser.id) && USER_MENTION.matches(prefix)) {
            "@${jda.selfUser.name} "
        } else {
            prefix
        }
    }

    override val prefix: String = container.settings.botInfo.prefix
    override var commandOrder: List<AbstractCommand> = emptyList()
    override var args: List<String> = emptyList()
    override var oldArgs: List<String> = emptyList()
    override val daoManager = container.daoManager
    override var rawArg: String = ""
    override val contextTime = System.currentTimeMillis()
    override var fullArg: String = ""
    override val botDevIds: LongArray = container.settings.botInfo.developerIds

    override var calculatedRoot = ""
    override var calculatedCommandPartsOffset = 1

    override fun initArgs() {
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
        val rearg = rawArg.replace(
            SPACE_PATTERN,
            " "
        ) + " " // this last space is needed so I don't need to hack around in the splitter for the last arg
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

        // Init fullArgs (rawarg without ")
        val carr = rawArg.toCharArray()
        for ((i, c) in carr.withIndex()) {
            if (quotationIndexes.contains(i)) continue
            fullArg += c
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
            rawArg.split(SPACE_PATTERN)
        } else {
            emptyList()
        }

        logger = LoggerFactory.getLogger(commandOrder.first().javaClass.name)
    }

    override  fun reply(something: Any) {
        require(!(isFromGuild && !selfMember.hasPermission(textChannel, Permission.MESSAGE_WRITE))) {
            "No MESSAGE_WRITE permission"
        }
        channel.sendMessage(something.toString()).queue()
    }

    override fun reply(embed: MessageEmbed) {
        require(!(isFromGuild && !selfMember.hasPermission(textChannel, Permission.MESSAGE_WRITE))) {
            "No MESSAGE_WRITE permission"
        }
        channel.sendMessageEmbeds(embed).queue()
    }

    override suspend fun getLanguage(): String = me.melijn.melijnbot.internals.translation.getLanguage(this)

    //Gets part of the rawarg by using regex and args
    override  fun getRawArgPart(beginIndex: Int, endIndex: Int): String {
        if (beginIndex > args.size) return ""
        var newString = fullArg
        for (i in 0 until beginIndex) {
            newString = newString.removeFirst(args[i]).trim()
        }

        if (endIndex != -1 && endIndex < args.size) {
            for (i in endIndex until args.size) {
                newString = newString.removeSuffix(args[i]).trim()
            }
        }

        return newString
    }

    override suspend fun getTranslation(path: String): String = i18n.getTranslation(this, path)
    override suspend fun getTimeZoneId(): ZoneId {
        val guildTimezone = guildN?.idLong?.let {
            val zoneId = daoManager.timeZoneWrapper.getTimeZone(it)
            if (zoneId.isBlank()) null
            else TimeZone.getTimeZone(zoneId).toZoneId()
        }

        val userTimezone = authorId.let {
            val zoneId = daoManager.timeZoneWrapper.getTimeZone(it)
            if (zoneId.isBlank()) null
            else TimeZone.getTimeZone(zoneId).toZoneId()
        }

        return userTimezone ?: guildTimezone ?: ZoneId.of("GMT")
    }

    override fun initCooldown(less: Long) {
        val idPath = commandOrder.first().id.toString() + commandOrder.drop(1).joinToString(".") { it.name }
        daoManager.globalCooldownWrapper.setLastExecuted(authorId, idPath, System.currentTimeMillis() - less)
    }
}