package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.RunConditionUtil
import me.melijn.melijnbot.internals.utils.countWords
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.remove
import me.melijn.melijnbot.internals.utils.withVariable
import me.melijn.melijnbot.internals.web.WebUtils
import net.dv8tion.jda.api.entities.MessageEmbed

const val GENIUS: String = "https://api.genius.com"
const val GENIUS_SITE: String = "https://genius.com"

class LyricsCommand : AbstractCommand("command.lyrics") {

    init {
        id = 152
        name = "lyrics"
        aliases = arrayOf("songText")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        if (RunConditionUtil.checkPlayingTrackNotNull(
                context.container,
                context.message
            ) || context.args.isNotEmpty()
        ) {
            // If argument supplied, use argument info, otherwise use playing trackinfo
            val arg = context.rawArg.takeIf { it.isNotBlank() }
            val info = context.getGuildMusicPlayer().guildTrackManager.playingTrack?.info
            val title = arg ?: info?.title ?: throw IllegalStateException("invariant didn't hold, poop code")
            val author = if (arg == null) info?.author else null
            val lyrics = getLyricsNMessage(context, title, author)

            if (lyrics == null) {
                sendRsp(context, "found no lyrics for $title :/")
                return
            }

            formatAndSendLyrics(context, lyrics.first, lyrics.second)
        } else {
            val msg = context.getTranslation("$root.extrahelp")
                .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
            sendRsp(context, msg)
        }
    }

    private suspend fun formatAndSendLyrics(context: ICommandContext, name: String, lyrics: String) {
        val words = context.getTranslation("$root.words")
        val characters = context.getTranslation("$root.characters")

        val embed = Embedder(context)
            .setTitle(name.take(MessageEmbed.TITLE_MAX_LENGTH))
            .setDescription(lyrics.take(MessageEmbed.TEXT_MAX_LENGTH))
            .addField(words, "${lyrics.countWords()}", true)
            .addField(characters, "${lyrics.remove(" ").length}", true)

        sendEmbedRsp(context, embed.build())
    }

    // url, lyrics
    private suspend fun getLyricsNMessage(
        context: ICommandContext,
        title: String,
        author: String?
    ): Pair<String, String>? {
        val key = context.container.settings.tokens.geniusApi
        val httpClient = context.webManager.proxiedHttpClient
        val json = WebUtils.getJsonFromUrl(
            httpClient,
            "$GENIUS/search",
            mutableMapOf(
                Pair("q", title + (author?.let { " $author" } ?: "")),
            ),
            mutableMapOf(Pair("Authorization", key))
        ) ?: return null

        val hits = json.getObject("response").getArray("hits")
        if (hits.isEmpty) return null
        val result = hits.getObject(0).getObject("result")

        val apiPath = result.getString("api_path")
        val resultTitle = result.getString("title")

        val songText =
            WebUtils.getResponseFromUrl(
                httpClient, "${GENIUS_SITE}${apiPath}/embed.js",
                headers = emptyMap()
            ) ?: return null

        return if (songText.isEmpty()) null
        else resultTitle to getReadable(songText)
    }
}

// https://github.com/LowLevelSubmarine/GeniusLyricsAPI/blob/master/src/main/java/genius/LyricsParser.java
private fun getReadable(rawLyrics: String): String {
    return rawLyrics
        .replace(
            "[\\S\\s]*<div class=\\\\\\\\\\\\\"rg_embed_body\\\\\\\\\\\\\">[ (\\\\\\\\n)]*".toRegex(),
            ""
        )
        .replace("[ (\\\\\\\\n)]*<\\\\/div>[\\S\\s]*".toRegex(), "")
        .replace("<[^<>]*>".toRegex(), "")
        .replace("\\\\\\\\n".toRegex(), "\n")
        .replace("\\\\'".toRegex(), "'")
        .replace("\\\\\\\\\\\\\"".toRegex(), "\"")
}