package me.melijn.melijnbot.commands.music

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.PLACEHOLDER_PREFIX
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.KSOFT_SI
import me.melijn.melijnbot.objects.utils.RunConditionUtil
import me.melijn.melijnbot.objects.utils.countWords
import me.melijn.melijnbot.objects.utils.message.sendEmbedRsp
import me.melijn.melijnbot.objects.utils.message.sendRsp
import me.melijn.melijnbot.objects.utils.remove
import me.melijn.melijnbot.objects.utils.withVariable
import me.melijn.melijnbot.objects.web.WebUtils
import net.dv8tion.jda.api.entities.MessageEmbed

class LyricsCommand : AbstractCommand("command.lyrics") {

    init {
        id = 152
        name = "lyrics"
        aliases = arrayOf("songText")
        commandCategory = CommandCategory.MUSIC
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            if (RunConditionUtil.checkPlayingTrackNotNull(context.container, context.event, context.getLanguage())) {
                val info = context.guildMusicPlayer.guildTrackManager.playingTrack?.info
                    ?: throw IllegalArgumentException("angry pepe")

                val lyrics = getLyricsNMessage(context, info.title, info.author) ?: return

                formatAndSendLyrics(context, info.title + " " + info.author, lyrics.first, lyrics.second)
            } else {
                val msg = context.getTranslation("$root.extrahelp")
                    .withVariable(PLACEHOLDER_PREFIX, context.usedPrefix)
                sendRsp(context, msg)
            }
        } else {
            val title = context.rawArg
            val lyrics = getLyricsNMessage(context, title, null) ?: return

            formatAndSendLyrics(context, title, lyrics.first, lyrics.second)
        }
    }

    private suspend fun formatAndSendLyrics(context: CommandContext, title: String, name: String, lyrics: String) {
        val embed = Embedder(context)
        embed.setTitle(name.take(MessageEmbed.TITLE_MAX_LENGTH))
        embed.setDescription(lyrics.take(MessageEmbed.TEXT_MAX_LENGTH))

        val words = context.getTranslation("$root.words")
        val characters = context.getTranslation("$root.characters")

        embed.addField(words, "${lyrics.countWords()}", true)
        embed.addField(characters, "${lyrics.remove(" ").length}", true)

        sendEmbedRsp(context, embed.build())
    }

    // url, lyrics
    private suspend fun getLyricsNMessage(context: CommandContext, title: String, author: String?): Pair<String, String>? {
        val json = WebUtils.getJsonFromUrl(context.webManager.httpClient,
            "$KSOFT_SI/lyrics/search",
            mutableMapOf(
                Pair("q", title + author?.let { " $author" }),
                Pair("limit", "1")
            ),
            mutableMapOf(Pair("Authorization", context.container.settings.tokens.kSoftApi))
        ) ?: return null

        val res = json.getArray("data")
        if (res.isEmpty) return null

        val result = res.getObject(0)
        val name = result.getString("name") + " (" + result.getString("artist") + ")"
        val lyrics = result.getString("lyrics")

        return Pair(name, lyrics)
    }
}