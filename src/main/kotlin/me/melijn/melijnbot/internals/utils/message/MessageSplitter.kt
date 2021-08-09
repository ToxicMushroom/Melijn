package me.melijn.melijnbot.internals.utils.message

import me.melijn.melijnbot.internals.utils.StringUtils

class MessageSplitter(val minSplit: Int, val maxSplit: Int) {

    var codeBlockLanguage: String? = null

    fun split(text: String): List<String> {
        codeBlockLanguage?.let { lang ->
            return StringUtils.splitMessageWithCodeBlocks(text, minSplit, maxSplit, lang)
        }
        return StringUtils.splitMessage(text, minSplit, maxSplit)
    }

    companion object {
        val Default = MessageSplitter(1800, 2000)
        val EmbedLdif = MessageSplitter(3800, 4000)
    }
}