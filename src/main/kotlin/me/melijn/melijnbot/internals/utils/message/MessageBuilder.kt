package me.melijn.melijnbot.internals.utils.message

import net.dv8tion.jda.api.entities.Message

class MessageBuilder {

    var content: Message? = null
    var splitter: MessageSplitter = MessageSplitter.Default
    var pagination: Pagination? = null



}