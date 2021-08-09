package me.melijn.melijnbot.internals.utils.message

import me.melijn.melijnbot.Container
import me.melijn.melijnbot.database.DaoManager
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.models.ModularMessage
import me.melijn.melijnbot.internals.utils.FetchingPaginationInfo
import me.melijn.melijnbot.internals.utils.ModularStoragePaginationInfo
import me.melijn.melijnbot.internals.utils.StoragePaginationInfo
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.PrivateChannel
import net.dv8tion.jda.api.entities.TextChannel

suspend fun sendPaginationModularRsp(context: ICommandContext, modularMessages: List<ModularMessage>, index: Int) {
    val premiumGuild = context.isFromGuild && context.daoManager.supporterWrapper.getGuilds().contains(context.guildId)
    if (premiumGuild) {
        sendPaginationModularRsp(
            context.textChannel,
            context.authorId,
            context.daoManager,
            modularMessages,
            index
        )
    } else {
        sendPaginationModularMsg(context, modularMessages, index)
    }
}

suspend fun sendPaginationModularRsp(
    textChannel: TextChannel,
    authorId: Long,
    daoManager: DaoManager,
    modularMessages: List<ModularMessage>,
    index: Int
) {
    val storage = StoringPagination(modularMessages, index)
    val content = storage.getPage(index)
    val message = sendRspAwaitN(textChannel, daoManager, content)
        ?: throw IllegalArgumentException("Couldn't send the message")
    if (modularMessages.size > 1)
        registerPaginationModularMessage(textChannel, authorId, message, modularMessages, index)
}

suspend fun sendPaginationMsgFetching(
    context: ICommandContext,
    msg: String,
    pages: Int,
    fetcher: suspend (Int) -> String
) {
    if (msg.length > 2000) throw IllegalArgumentException("No splitting here :angry:")
    val channel = context.channel
    require(context.isFromGuild || context.textChannel.canTalk()) { "Cannot talk in this channel " + channel.name }
    val fetcherPagination = FetchingPagination(pages, 0) { ModularMessage(fetcher(it)) }
    val content = fetcherPagination.getPage(0)

    val message = sendRspAwaitN(context, content) ?: return
    registerPaginationMessage(context.textChannel, context.authorId, message, fetcher, pages)
}

fun registerPaginationMessage(
    textChannel: TextChannel,
    authorId: Long,
    message: Message,
    fetcher: suspend (Int) -> String,
    pages: Int
) {
    Container.instance.fetcherPaginationMap[message.idLong] = FetchingPaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        authorId,
        message.idLong,
        fetcher,
        pages,
        0
    )
}

suspend fun sendPaginationModularMsg(context: ICommandContext, msgList: List<ModularMessage>, index: Int) {
    val storage = StoringPagination(msgList, index)
    val content = storage.getPage(index)
    if (context.isFromGuild) {
        val message = sendMsgAwaitN(context.textChannel, content)
            ?: throw IllegalArgumentException("Couldn't send the message")
        if (msgList.size > 1)
            registerPaginationModularMessage(context.textChannel, context.authorId, message, msgList, index)
    } else {
        val message = sendMsgAwaitN(context.privateChannel, content)
            ?: throw IllegalArgumentException("Couldn't send the message")
        if (msgList.size > 1)
            registerPaginationModularMessage(context.privateChannel, context.authorId, message, msgList, index)
    }
}

fun registerPaginationModularMessage(
    textChannel: TextChannel,
    authorId: Long,
    message: Message,
    msgList: List<ModularMessage>,
    index: Int
) {
    Container.instance.modularPaginationMap[message.idLong] = ModularStoragePaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )
}

fun registerPaginationModularMessage(
    privateChannel: PrivateChannel,
    authorId: Long,
    message: Message,
    msgList: List<ModularMessage>,
    index: Int
) {
    Container.instance.modularPaginationMap[message.idLong] = ModularStoragePaginationInfo(
        -1,
        privateChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )

}

fun registerPaginationMessage(
    textChannel: TextChannel,
    authorId: Long,
    message: Message,
    msgList: List<String>,
    index: Int
) {
    Container.instance.paginationMap[System.nanoTime()] = StoragePaginationInfo(
        textChannel.guild.idLong,
        textChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )
}

fun registerPaginationMessage(
    privateChannel: PrivateChannel,
    authorId: Long,
    message: Message,
    msgList: List<String>,
    index: Int
) {
    Container.instance.paginationMap[System.nanoTime()] = StoragePaginationInfo(
        -1,
        privateChannel.idLong,
        authorId,
        message.idLong,
        msgList,
        index
    )
}