package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.Settings
import me.melijn.melijnbot.commands.utility.toUniversalDateFormat
import me.melijn.melijnbot.commands.utility.toUniversalDateTimeFormat
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.remove
import me.melijn.melijnbot.internals.utils.withVariable
import moe.ganen.jikankt.JikanKt
import moe.ganen.jikankt.connection.RestClient
import moe.ganen.jikankt.exception.JikanException
import net.dv8tion.jda.api.entities.MessageEmbed

class MyAnimeListCommand(jikanSettings: Settings.Jikan) : AbstractCommand("command.myanimelist") {

    private val animeArg: AnimeArg
    private val jikanUrl: String

    init {
        id = 158
        name = "myAnimeList"
        aliases = arrayOf("mal")
        animeArg = AnimeArg(root)
        children = arrayOf(
            UserArg(root),
            animeArg,
            MangaArg(root),
            CharacterArg(root)
        )
        jikanUrl = "http${(if (jikanSettings.ssl) "s" else "")}://${jikanSettings.host}:${jikanSettings.port}/v3/"

        JikanKt.apply { restClient = RestClient(false, jikanUrl) }
        commandCategory = CommandCategory.ANIME
    }

    class CharacterArg(parent: String) : AbstractCommand("$parent.character") {

        init {
            name = "character"
            aliases = arrayOf("char")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val characterName = context.rawArg.take(256)


            try {

                val characterLite = JikanKt.searchCharacter(characterName).results?.firstOrNull()
                val character = characterLite?.malId?.let { JikanKt.getCharacter(it) }
                if (character == null) {
                    val msg = context.getTranslation("$root.noresult")
                        .withVariable(PLACEHOLDER_ARG, characterName)
                    sendRsp(context, msg)
                    return
                }


                val name = context.getTranslation("title.fullname")
                val namekanji = context.getTranslation("title.namekanji")
                val alternativenames = context.getTranslation("title.alternativenames")

                val anime = context.getTranslation("title.anime")
                val manga = context.getTranslation("title.manga")

                val eb = Embedder(context)
                    .setThumbnail(character.imageUrl)
                    .setTitle(character.name ?: "/", character.url)
                    .setDescription(character.about?.remove("\\n")?.take(MessageEmbed.TEXT_MAX_LENGTH) ?: "/")
                    .addField(name, character.name ?: "/", true)
                    .addField(namekanji, character.nameKanji ?: "/", true)

                val otherNames = character.nicknames
                if (otherNames != null && otherNames.isNotEmpty() && !(otherNames.size == 1 && otherNames[0]?.isBlank() == true)) {
                    eb.addField(alternativenames, otherNames.joinToString(), true)
                }

                val animes = mutableListOf<String>()
                val mangas = mutableListOf<String>()


                for (animepgraphi in character.animeography ?: emptyList()) {
                    if (animepgraphi == null) continue
                    val title = animepgraphi.name ?: "error"
                    val url = animepgraphi.url ?: "error"
                    val characterRole = animepgraphi.role ?: "?"

                    animes.add("[$title]($url) [$characterRole]")
                }

                for (mangagraphi in character.mangaography ?: emptyList()) {
                    if (mangagraphi == null) continue
                    val title = mangagraphi.name ?: "error"
                    val url = mangagraphi.url ?: "error"
                    val characterRole = mangagraphi.role ?: "?"

                    mangas.add("[$title]($url) [$characterRole]")
                }

                if (animes.isNotEmpty()) {
                    val split = StringUtils.splitMessage(animes.joinToString("\n"), 600, MessageEmbed.VALUE_MAX_LENGTH)
                    if (split.size == 1)
                        eb.addField(anime, split[0], true)
                    else {
                        eb.addField(anime, split[0], true)
                        eb.addField("$anime..", split[1], true)
                        if (split.size > 2) {
                            val didntfit = context.getTranslation("title.nofitgotourl")
                            eb.addField("$anime...", didntfit, true)
                        }
                    }
                }

                if (mangas.isNotEmpty()) {
                    val split = StringUtils.splitMessage(mangas.joinToString("\n"), 600, MessageEmbed.VALUE_MAX_LENGTH)
                    if (split.size == 1)
                        eb.addField(manga, split[0], true)
                    else {
                        eb.addField(manga, split[0], true)
                        eb.addField("$manga..", split[1], true)
                        if (split.size > 2) {
                            val didntfit = context.getTranslation("title.nofitgotourl")
                            eb.addField("$manga...", didntfit, true)
                        }
                    }
                }

                val favourites = context.getTranslation("footer.favourites")
                eb.setFooter(favourites.withVariable("amount", character.memberFavorites?.toString() ?: "0"))

                sendEmbedRsp(context, eb.build())
            } catch (e: JikanException) {
                val msg = context.getTranslation("$root.noresult")
                    .withVariable(PLACEHOLDER_ARG, context.args[0])
                sendRsp(context, msg)
            }
        }
    }

    class MangaArg(parent: String) : AbstractCommand("$parent.manga") {

        init {
            name = "manga"
            aliases = arrayOf("m", "ln")
            runConditions = arrayOf(RunCondition.CHANNEL_NSFW)
        }

        // TODO get the actual manga entry
        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val manga = context.rawArg.take(256)
            try {
                val result = JikanKt.searchManga(manga).results?.firstOrNull()
                if (result == null) {
                    val msg = context.getTranslation("$root.noresult")
                        .withVariable(PLACEHOLDER_ARG, manga)
                    sendRsp(context, msg)
                    return
                }

                val rating = context.getTranslation("title.rating")
                val chapters = context.getTranslation("title.chapters")
                val volumes = context.getTranslation("title.volumes")
                val type = context.getTranslation("title.manga.type")

                val startDate = context.getTranslation("title.startdate")
                val endDate = context.getTranslation("title.enddate")
                val publishing = context.getTranslation("title.publishing")
                val synopsis = context.getTranslation("title.synopsis")


                val publishingValue = result.publishing?.let {
                    context.getTranslation(if (it) {
                        "yes"
                    } else {
                        "no"
                    })
                } ?: context.getTranslation("unknown")

                val eb = Embedder(context)
                    .setTitle(result.title ?: "???", result.url)

                    .addField(type, result.type?.toString() ?: "?", true)
                    .addField("$volumes | $chapters", "${result.volumes ?: "0"} | ${result.chapters ?: "0"}", true)
                    .addField(rating, "${result.score ?: "?"}/10.0", true)

                    .addField(startDate, result.startDate?.toUniversalDateFormat() ?: "/", true)
                    .addField(endDate, result.endDate?.toUniversalDateFormat() ?: "/", true)
                    .addField(publishing, publishingValue, true)

                    .addField(synopsis, result.synopsis ?: "/", false)

                    .setThumbnail(result.imageUrl)

                result.members?.let {
                    val members = context.getTranslation("footer.members")
                    eb.setFooter(members.withVariable("amount", "$it"))
                }

                sendEmbedRsp(context, eb.build())

            } catch (e: JikanException) {
                val msg = context.getTranslation("$root.noresult")
                    .withVariable(PLACEHOLDER_ARG, context.args[0])
                sendRsp(context, msg)
            }
        }
    }

    class AnimeArg(parent: String) : AbstractCommand("$parent.anime") {

        init {
            name = "anime"
            aliases = arrayOf("a", "series", "movie", "tv", "ova", "ona")
            runConditions = arrayOf(RunCondition.CHANNEL_NSFW)
        }

        // TODO get the actual anime entry
        override suspend fun execute(context: CommandContext) {
            showSeries(context)
        }

        suspend fun showSeries(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val animeName = context.rawArg.take(256)
            try {
                val result = JikanKt.searchAnime(animeName).results?.firstOrNull()
                if (result == null) {
                    val msg = context.getTranslation("$root.noresult")
                        .withVariable(PLACEHOLDER_ARG, animeName)
                    sendRsp(context, msg)
                    return
                }

                val rating = context.getTranslation("title.rating")
                val episodes = context.getTranslation("title.episodes")
                val type = context.getTranslation("title.anime.type")

                val startDate = context.getTranslation("title.startdate")
                val endDate = context.getTranslation("title.enddate")
                val airing = context.getTranslation("title.airing")
                val synopsis = context.getTranslation("title.synopsis")

                val airingValue = result.airing?.let {
                    context.getTranslation(if (it) {
                        "yes"
                    } else {
                        "no"
                    })
                } ?: context.getTranslation("unknown")

                val eb = Embedder(context)
                    .setTitle(result.title ?: "???", result.url)

                    .addField(type, result.type?.toString() ?: "?", true)
                    .addField(episodes, "${result.episodes ?: "?"}", true)
                    .addField(rating, "${result.score ?: "?"}/10.0", true)

                    .addField(startDate, result.startDate?.toUniversalDateFormat() ?: "/", true)
                    .addField(endDate, result.endDate?.toUniversalDateFormat() ?: "/", true)
                    .addField(airing, airingValue, true)

                    .addField(synopsis, result.synopsis ?: "/", false)

                    .setThumbnail(result.imageUrl)

                result.members?.let {
                    val members = context.getTranslation("footer.members")
                    eb.setFooter(members.withVariable("amount", "$it"))
                }

                sendEmbedRsp(context, eb.build())

            } catch (e: JikanException) {
                val msg = context.getTranslation("$root.noresult")
                    .withVariable(PLACEHOLDER_ARG, context.args[0])
                sendRsp(context, msg)
            }
        }
    }

    class UserArg(parent: String) : AbstractCommand("$parent.user") {

        init {
            name = "user"
            aliases = arrayOf("profile", "u", "userProfile", "up")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val user = context.rawArg.take(256)

            try {
                val result = JikanKt.getUser(user)
                if (result.url == null) {
                    val msg = context.getTranslation("$root.noresult")
                        .withVariable(PLACEHOLDER_ARG, user)
                    sendRsp(context, msg)
                    return
                }

                val about = context.getTranslation("title.about")
                val joinDate = context.getTranslation("title.joindate")


                val eb = Embedder(context)
                    .setTitle(result.username, result.url)
                    .addField(joinDate, result.joined?.toUniversalDateFormat() ?: "/", true)

                result.location?.let {
                    eb.addField(context.getTranslation("title.location"), it, true)
                }
                result.birthday?.let {
                    eb.addField(context.getTranslation("title.birthday"), it.toUniversalDateFormat(), true)
                }
                result.gender?.let {
                    eb.addField(context.getTranslation("title.gender"), it, true)
                }

                eb.addField(about, result.about ?: "", false)

                result.favorites?.let outer@{
                    it.anime?.let { animeList ->
                        if (animeList.isEmpty()) return@let
                        val top = animeList.take(5)

                        eb.addField(
                            context.getTranslation("title.favorite.anime"),
                            top.joinToString("\n") { anime ->
                                "⁎ [${anime?.name}](${anime?.url})"
                            }.take(MessageEmbed.VALUE_MAX_LENGTH),
                            true
                        )
                    }
                    it.manga?.let { mangaList ->
                        if (mangaList.isEmpty()) return@let

                        eb.addField(
                            context.getTranslation("title.favorite.manga"),
                            mangaList
                                .joinToString("\n") { manga ->
                                    "⁎ [${manga?.name}](${manga?.url})"
                                }
                                .take(MessageEmbed.VALUE_MAX_LENGTH),
                            true
                        )
                    }
                    it.characters?.let { characters ->
                        if (characters.isEmpty()) return@let

                        eb.addField(
                            context.getTranslation("title.favorite.characters"),
                            characters.joinToString("\n") { character ->
                                "⁎ [${character?.name}](${character?.url})"
                            }.take(MessageEmbed.VALUE_MAX_LENGTH),
                            true
                        )
                    }
                }

                result.lastOnline?.let {
                    eb.addField(context.getTranslation("title.lastonline"), it.toUniversalDateTimeFormat(), false)
                }

                eb.setThumbnail(result.imageUrl)
                sendEmbedRsp(context, eb.build())
            } catch (e: JikanException) {
                val msg = context.getTranslation("$root.noresult")
                    .withVariable(PLACEHOLDER_ARG, context.args[0])
                sendRsp(context, msg)
                return
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        animeArg.showSeries(context)
    }
}
