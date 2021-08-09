package me.melijn.melijnbot.internals.utils.message

import me.melijn.melijnbot.internals.models.ModularMessage
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.Button
import kotlin.math.max
import kotlin.math.min
import net.dv8tion.jda.api.MessageBuilder as JdaMessageBuilder

class FetchingPagination(
    override val pageCount: Int,
    override var currentPage: Int,
    val messageFetcher: suspend (Int) -> ModularMessage
) : Pagination {

    override val type: PaginationType = PaginationType.FETCHING

    override suspend fun getPage(page: Int): Message {
        val modularMessage = messageFetcher(page)
        return paginate(modularMessage, page)
    }
}

class StoringPagination(
    val messages: List<ModularMessage>,
    override var currentPage: Int
) : Pagination {

    override val pageCount: Int = messages.size
    override val type: PaginationType = PaginationType.STORING

    override suspend fun getPage(page: Int): Message {
        val modularMessage = messages[page]
        return paginate(modularMessage, page)
    }
}

interface Pagination {

    val type: PaginationType
    val pageCount: Int
    var currentPage: Int

    /**
     * @throws IllegalArgumentException if page > pageCount || page < 0
     */
    suspend fun getPage(page: Int): Message

    fun paginate(modularMessage: ModularMessage, page: Int): Message {
        val message = modularMessage.toMessage()
            ?: throw IllegalStateException("Failed to convert fetched modularMessage into a message")
        if (message.actionRows.size >= 5) throw IllegalStateException("Can't add pagination buttons to message with >5 button rows")

        val currentActionRows = message.actionRows.toMutableList()
        val paginationRow = getPaginationActionRow(page)
        currentActionRows.add(ActionRow.of(paginationRow))
        return JdaMessageBuilder(message)
            .setActionRows(currentActionRows)
            .build()
    }

    fun getPaginationActionRow(page: Int): List<Button> {
        val buttons = mutableListOf<Button>()
        buttons.add(Button.secondary("full_left", "<<").withDisabled(page < 2))
        buttons.add(Button.secondary("left", "<").withDisabled(page < 1))
        buttons.add(Button.secondary("right", ">").withDisabled(page > pageCount - 2))
        buttons.add(Button.secondary("full_right", ">>").withDisabled(page > pageCount - 3))
        return buttons
    }

    suspend fun navigate(button: Button): Message? {
        val lastPage = pageCount - 1
        val page = when (button.id) {
            "full_left" -> 0
            "left" -> currentPage - 1
            "right" -> currentPage + 1
            "full_right" -> lastPage
            else -> return null
        }
        val next = min(max(0, page), lastPage)
        if (next == currentPage) return null
        currentPage = next
        return getPage(next)
    }
}

enum class PaginationType {
    STORING, FETCHING
}
