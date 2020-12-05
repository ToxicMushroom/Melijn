package me.melijn.melijnbot.internals.utils.message

import kotlinx.coroutines.runBlocking
import me.melijn.melijnbot.Container
import me.melijn.melijnbot.MelijnBot
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.translation.getLanguage
import me.melijn.melijnbot.internals.translation.i18n
import me.melijn.melijnbot.internals.utils.toUCC
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.ByteBuffer
import java.util.*


fun Throwable.sendInGuild(
    context: CommandContext,
    thread: Thread = Thread.currentThread(),
    extra: String? = null,
    shouldSend: Boolean = true
) = runBlocking {
    val sanitizedMessage = "Message: ${MarkdownSanitizer.escape(context.message.contentRaw)}\n" + (extra ?: "")
    sendInGuildSuspend(context.guildN, context.messageChannel, context.author, thread, sanitizedMessage, shouldSend)
}

fun Throwable.sendInGuild(
    guild: Guild? = null,
    channel: MessageChannel? = null,
    author: User? = null,
    thread: Thread = Thread.currentThread(),
    extra: String? = null,
    shouldSend: Boolean = false
) = runBlocking {
    sendInGuildSuspend(guild, channel, author, thread, extra, shouldSend)
}

suspend fun Throwable.sendInGuildSuspend(
    guild: Guild? = null,
    channel: MessageChannel? = null,
    author: User? = null,
    thread: Thread = Thread.currentThread(),
    extra: String? = null,
    shouldSend: Boolean = false
) {
    if (Container.instance.settings.unLoggedThreads.contains(thread.name)) return

    val channelId = Container.instance.settings.botInfo.exceptionChannel
    val textChannel = MelijnBot.shardManager.getTextChannelById(channelId) ?: return

    val caseId = Base58.encode(
        ByteBuffer
            .allocate(Long.SIZE_BYTES)
            .putLong(System.currentTimeMillis())
            .array()
    )

    val sb = StringBuilder()

    sb.append("**CaseID**: `").append(caseId).appendLine("`")
    if (guild != null) {
        sb.append("**Guild**: ").append(guild.name).append(" | ").appendLine(guild.id)
    }
    if (channel != null) {
        sb.append("**")
            .append(channel.type.toUCC())
            .append("Channel**: #").append(channel.name).append(" | ").appendLine(channel.id)
    }
    if (author != null) {
        sb.append("**User**: ").append(author.asTag).append(" | ").appendLine(author.id)
    }
    sb.append("**Thread**: ").appendLine(thread.name)

    val writer = StringWriter()
    val printWriter = PrintWriter(writer)
    this.printStackTrace(printWriter)
    val stacktrace = MarkdownSanitizer.escape(writer.toString())
        .replace("at me.melijn.melijnbot", "**at me.melijn.melijnbot**")
    sb.append(stacktrace)
    extra?.let {
        sb.appendLine("**Extra**")
        sb.appendLine(it)
    }

    if (Container.instance.logToDiscord) {
        sendMsg(textChannel, sb.toString())
    }

    if (shouldSend && channel != null && (channel !is TextChannel || channel.canTalk()) && (channel is TextChannel || channel is PrivateChannel)) {
        val lang = getLanguage(Container.instance.daoManager, author?.idLong ?: -1, guild?.idLong ?: -1)

        val permError = this is InsufficientPermissionException
        val msg = if (permError) {
            "${this.message}, caseId: $caseId (consider using `>support` to join our support server)"

        } else i18n.getTranslation(lang, "message.exception")
            .withVariable("caseId", caseId)

        if (channel is TextChannel)
            sendMsg(channel, msg)
        else if (channel is PrivateChannel)
            sendMsg(channel, msg)
    }
}

object Base58 {

    private val ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray()
    private val ENCODED_ZERO = ALPHABET[0]
    private val INDEXES = IntArray(128)

    init {
        Arrays.fill(INDEXES, -1)
        for (i in ALPHABET.indices) {
            INDEXES[ALPHABET[i].toInt()] = i
        }
    }

    /**
     * Encodes the given bytes as a base58 string (no checksum is appended).
     *
     * @param input the bytes to encode
     * @return the base58-encoded string
     */
    fun encode(input: ByteArray): String {
        var barr = input
        if (barr.isEmpty()) {
            return ""
        }
        // Count leading zeros.
        var zeros = 0
        while (zeros < barr.size && barr[zeros] == 0.toByte()) {
            ++zeros
        }
        // Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
        barr = barr.copyOf(barr.size) // since we modify it in-place
        val encoded = CharArray(barr.size * 2) // upper bound
        var outputStart = encoded.size
        var inputStart = zeros
        while (inputStart < barr.size) {
            encoded[--outputStart] = ALPHABET[divmod(barr, inputStart, 256, 58).toInt()]
            if (barr[inputStart] == 0.toByte()) {
                ++inputStart // optimization - skip leading zeros
            }
        }
        // Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
        while (outputStart < encoded.size && encoded[outputStart] == ENCODED_ZERO) {
            ++outputStart
        }
        while (--zeros >= 0) {
            encoded[--outputStart] = ENCODED_ZERO
        }
        // Return encoded string (including encoded leading zeros).
        return String(encoded, outputStart, encoded.size - outputStart)
    }

    fun decode(input: String): ByteArray? {
        if (input.isEmpty()) {
            return ByteArray(0)
        }
        // Convert the base58-encoded ASCII chars to a base58 byte sequence (base58 digits).
        val input58 = ByteArray(input.length)
        for (i in input.indices) {
            val c = input[i]
            val digit = if (c.toInt() < 128) INDEXES.get(c.toInt()) else -1
            if (digit < 0) {
                return null
            }
            input58[i] = digit.toByte()
        }
        // Count leading zeros.
        var zeros = 0
        while (zeros < input58.size && input58[zeros] == 0.toByte()) {
            ++zeros
        }
        // Convert base-58 digits to base-256 digits.
        val decoded = ByteArray(input.length)
        var outputStart = decoded.size
        var inputStart = zeros
        while (inputStart < input58.size) {
            decoded[--outputStart] = divmod(input58, inputStart, 58, 256)
            if (input58[inputStart] == 0.toByte()) {
                ++inputStart // optimization - skip leading zeros
            }
        }
        // Ignore extra leading zeroes that were added during the calculation.
        while (outputStart < decoded.size && decoded[outputStart] == 0.toByte()) {
            ++outputStart
        }
        // Return decoded data (including original number of leading zeros).
        return decoded.copyOfRange(outputStart - zeros, decoded.size)
    }

    /**
     * Divides a number, represented as an array of bytes each containing a single digit
     * in the specified base, by the given divisor. The given number is modified in-place
     * to contain the quotient, and the return value is the remainder.
     *
     * @param number the number to divide
     * @param firstDigit the index within the array of the first non-zero digit
     * (this is used for optimization by skipping the leading zeros)
     * @param base the base in which the number's digits are represented (up to 256)
     * @param divisor the number to divide by (up to 256)
     * @return the remainder of the division operation
     */
    private fun divmod(number: ByteArray, firstDigit: Int, base: Int, divisor: Int): Byte {
        // this is just long division which accounts for the base of the input digits
        var remainder = 0
        for (i in firstDigit until number.size) {
            val digit = number[i].toInt() and 0xFF
            val temp = remainder * base + digit
            number[i] = (temp / divisor).toByte()
            remainder = temp % divisor
        }
        return remainder.toByte()
    }
}