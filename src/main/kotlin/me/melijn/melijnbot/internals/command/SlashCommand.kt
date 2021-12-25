package me.melijn.melijnbot.internals.command

import dev.minn.jda.ktx.EmbedBuilder
import me.melijn.melijnbot.internals.utils.BUNDLE
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import org.jetbrains.annotations.PropertyKey
import java.util.*
import kotlin.reflect.KClass

@FunctionalInterface
interface CommandExecutor<T, F> {

    suspend fun execute(context: T): Result<F>

}

enum class CommandFailure {
    DOES_NOT_EXIST
}

interface CommandOptions {

    val name: String
    val subName: String?
    val subGroupName: String?

    fun getOption(name: String, desiredType: KClass<out Any>): Any?


}

interface L10n {
    val locale: Locale

    infix fun translate(@PropertyKey(resourceBundle = BUNDLE) key: String) =
        me.melijn.melijnbot.internals.utils.translate(this.locale, key)

    fun translate(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?) =
        me.melijn.melijnbot.internals.utils.translate(this.locale, key, params)

}

class SlashCommandContext(val event: SlashCommandEvent) : CommandOptions, L10n {
    override val name: String = event.name
    override val subGroupName: String? = event.subcommandGroup
    override val subName: String? = event.subcommandName
    override val locale: Locale
        get() = Locale.getDefault()

    override fun getOption(name: String, desiredType: KClass<out Any>): Any? {
        val option = event.getOption(name) ?: return null
        return when (option.type) {
            OptionType.UNKNOWN -> TODO()
            OptionType.SUB_COMMAND -> TODO()
            OptionType.SUB_COMMAND_GROUP -> TODO()
            OptionType.STRING ->  option.asString
            OptionType.INTEGER -> option.asLong
            OptionType.BOOLEAN -> option.asBoolean
            OptionType.USER -> option.asUser
            OptionType.CHANNEL -> option.asGuildChannel
            OptionType.ROLE -> option.asRole
            OptionType.MENTIONABLE -> TODO()
            OptionType.NUMBER -> TODO()
        }
    }

    inline val String.err: String
        get() = ":warning: $this"

    inline val String.embedWaitInput: MessageEmbed
        get() = EmbedBuilder {
            title = translate("embed_awaiting_input")
            description = this@embedWaitInput
        }.build()

    inline val String.embedWorking: MessageEmbed
        get() = EmbedBuilder {
            title = translate("embed_working")
            description = this@embedWorking
        }.build()

    inline val String.embedFailure: MessageEmbed
        get() = EmbedBuilder {
            title = translate("embed_failure")
            description = this@embedFailure
        }.build()

    inline val String.embedBorked: MessageEmbed
        get() = EmbedBuilder {
            title = translate("embed_borked")
            description = this@embedBorked
        }.build()

    inline val String.embedSuccess: MessageEmbed
        get() = EmbedBuilder {
            title = translate("embed_success")
            description = this@embedSuccess
        }.build()

    inner class IntConstraint(
        val min: Int? = null,
        val max: Int? = null
    ) {
        inline operator fun invoke(value: Int, onError: (Int) -> Int): Int {
            min?.let { min -> if (value < min) return onError(value) }
            max?.let { min -> if (value > min) return onError(value) }
            return value
        }

        inline operator fun invoke(value: Int?, onError: (Int) -> Int): Int? =
            value?.let { invoke(it, onError) }
    }

    inner class TypeConstraint {
        inline operator fun <reified Has, reified Want> invoke(value: Has, onError: (Has) -> Want): Want =
            (value as? Want) ?: onError(value)

        @JvmName("invokeNullable")
        inline operator fun <reified Has, reified Want> invoke(value: Has?, onError: (Has) -> Want): Want? =
            value?.let { invoke(it, onError) }

        // This exists because the nullable `invoke` overload may not be called where expected as the firsts generic type can be nullable.
        inline fun <reified Has, reified Want> optional(value: Has?, onError: (Has) -> Want) =
            this.invoke(value, onError)
    }
}

class Result<out F> private constructor(val failure: F?) {
    companion object {
        fun <F> failure(handler: F) = Result(handler)
        fun <F> success(): Result<F> = Result(null)
    }
}