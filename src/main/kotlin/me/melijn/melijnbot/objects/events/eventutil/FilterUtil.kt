package me.melijn.melijnbot.objects.events.eventutil

import kotlinx.coroutines.future.await
import me.melijn.melijnbot.Container
import net.dv8tion.jda.api.entities.Message

object FilterUtil {
    suspend fun filterDefault(container: Container, message: Message) {
        val guild = message.guild
        val author = message.author
        val channel = message.textChannel
        val filterWrapper = container.daoManager.filterWrapper
        val globalPair = Pair(guild.idLong, -1L)
        val specificPair = Pair(guild.idLong, channel.idLong)
        val globalDeniedFilters = filterWrapper.deniedFilterCache.get(globalPair).await()
        val globalAllowedFilters = filterWrapper.allowedFilterCache.get(globalPair).await()
        val specificDeniedFilters = filterWrapper.deniedFilterCache.get(specificPair).await()
        val specificAllowedFilters = filterWrapper.deniedFilterCache.get(specificPair).await()

        val deniedList = globalDeniedFilters + specificDeniedFilters
        val allowedList = globalAllowedFilters + specificAllowedFilters


    }
}