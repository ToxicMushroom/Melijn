package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.KSOFT_SI
import me.melijn.melijnbot.internals.utils.RunConditionUtil
import me.melijn.melijnbot.internals.utils.countWords
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.remove
import me.melijn.melijnbot.internals.utils.withVariable
import me.melijn.melijnbot.internals.web.WebUtils
import net.dv8tion.jda.api.entities.MessageEmbed

class LyricsCommand : AbstractCommand("command.lyrics") {

    init {
        id = 152
        name = "lyrics"
        aliases = arrayOf("songText")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            if (RunConditionUtil.checkPlayingTrackNotNull(context.container, context.message)) {
                val info = context.getGuildMusicPlayer().guildTrackManager.playingTrack?.info
                    ?: throw IllegalArgumentException("angry pepe")

                val lyrics = getLyricsNMessage(context, info.title, info.author) ?: return

                formatAndSendLyrics(context, lyrics.first, lyrics.second)
            } else {
                val msg = context.getTranslation("$root.extrahelp")
                    .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
            }
        } else {
            val title = context.rawArg
            val lyrics = getLyricsNMessage(context, title, null) ?: return

            formatAndSendLyrics(context, lyrics.first, lyrics.second)
        }
    }

    private suspend fun formatAndSendLyrics(context: ICommandContext, name: String, lyrics: String) {
        val words = context.getTranslation("$root.words")
        val characters = context.getTranslation("$root.characters")
        val powered = context.getTranslation("$root.powered")

        val embed = Embedder(context)
            .setTitle(name.take(MessageEmbed.TITLE_MAX_LENGTH))
            .setDescription(lyrics.take(MessageEmbed.TEXT_MAX_LENGTH))
            .addField(words, "${lyrics.countWords()}", true)
            .addField(characters, "${lyrics.remove(" ").length}", true)
            .setFooter(powered.withVariable("url", "api.ksoft.si"))

        sendEmbedRsp(context, embed.build())
    }

    // url, lyrics
    private suspend fun getLyricsNMessage(
        context: ICommandContext,
        title: String,
        author: String?
    ): Pair<String, String>? {
        val json = WebUtils.getJsonFromUrl(context.webManager.proxiedHttpClient,
            "$KSOFT_SI/lyrics/search",
            mutableMapOf(
                Pair("q", title + (author?.let { " $author" } ?: "")),
                Pair("limit", "1")
            ),
            mutableMapOf(Pair("Authorization", context.container.settings.tokens.kSoftApi))
        ) ?: return null

        val res = try {
            json.getArray("data")
        } catch (t: Throwable) {
            return null
        }
        if (res.isEmpty) return null

        val result = res.getObject(0)
        val name = result.getString("name") + " (" + result.getString("artist") + ")"
        val lyrics = result.getString("lyrics")

        return Pair(name, lyrics)
    }
}