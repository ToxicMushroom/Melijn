package me.melijn.melijnbot.commands.anime

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import me.melijn.melijnbot.anilist.FindCharacterQuery
import me.melijn.melijnbot.anilist.FindUserQuery
import me.melijn.melijnbot.anilist.type.MediaType
import me.melijn.melijnbot.commandutil.anime.AniListCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.StringUtils
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.toUpperWordCase
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import java.awt.Color

class AniListCommand : AbstractCommand("command.anilist") {

    private val animeArg: AnimeArg

    init {
        id = 165
        name = "aniList"
        aliases = arrayOf("al")
        animeArg = AnimeArg(root)
        children = arrayOf(
            animeArg,
            SearchAnimeArg(root),
            MangaArg(root),
            SearchMangaArg(root),
            CharacterArg(root),
//            SearchCharacterArg(root),
            UserArg(root)
//            , SearchUserArg(root)
        )
        commandCategory = CommandCategory.ANIME
    }

    class MangaArg(parent: String) : AbstractCommand("$parent.manga") {

        init {
            name = "manga"
            aliases = arrayOf("m")
        }

        override suspend fun execute(context: CommandContext) {
            AniListCommandUtil.searchManga(context)
        }
    }

    class SearchMangaArg(parent: String) : AbstractCommand("$parent.searchmanga") {

        init {
            name = "searchManga"
            aliases = arrayOf("sm")
        }

        override suspend fun execute(context: CommandContext) {
            AniListCommandUtil.searchMedia(context, MediaType.MANGA)
        }
    }

    class UserArg(parent: String) : AbstractCommand("$parent.user") {

        init {
            name = "user"
            aliases = arrayOf("u", "profile", "userProfile", "up")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val userName = context.rawArg.take(256)

            context.webManager.aniListApolloClient.query(
                FindUserQuery.builder()
                    .name(userName)
                    .build()
            ).enqueue(object : ApolloCall.Callback<FindUserQuery.Data>() {
                override fun onFailure(e: ApolloException) {
                    TaskManager.async(context) {
                        val msg = context.getTranslation("$root.noresult")
                            .withVariable(PLACEHOLDER_ARG, userName)
                        sendRsp(context, msg)
                    }
                }

                override fun onResponse(response: Response<FindUserQuery.Data>) {
                    TaskManager.async(context) {
                        val user: FindUserQuery.User = response.data?.User() ?: return@async
                        foundUser(context, user)
                    }
                }
            })
        }

        private suspend fun foundUser(context: CommandContext, user: FindUserQuery.User) {
            val daoManager = context.daoManager
            val embedColorWrapper = daoManager.embedColorWrapper
            val userEmbedColorWrapper = daoManager.userEmbedColorWrapper
            val guildColor: Int = embedColorWrapper.getColor(context.guildId)
            val userColor = userEmbedColorWrapper.getColor(context.guildId)

            val defaultColor = when {
                userColor != 0 -> userColor
                guildColor != 0 -> guildColor
                else -> context.embedColor
            }

            val eb = EmbedBuilder() // We're using custom colors below
                .setThumbnail(user.avatar()?.large())
                .setTitle(user.name(), user.siteUrl())


            val aboutValue = user.about()
            if (aboutValue != null && aboutValue.isNotEmpty()) {
                val about = context.getTranslation("title.about")
                eb.setDescription(
                    "**$about**\n" + user.about()?.take(MessageEmbed.TEXT_MAX_LENGTH - 11)
                )
            }

            val chosenColor = when (user.options()?.profileColor()?.toLowerCase() ?: "null") {
                "green" -> Color(0x4CCA51)
                "blue" -> Color(0x3DB4F2)
                "purple" -> Color(0xC063FF)
                "pink" -> Color(0xFC9DD6)
                "orange" -> Color(0xEF881A)
                "red" -> Color(0xE13333)
                "gray" -> Color(0x677B94)
                else -> Color(defaultColor)
            }
            eb.setColor(chosenColor)

            val animeStats = user.statistics()?.anime()
            val mangaStats = user.statistics()?.manga()

            val favoriteAnimeGenres = context.getTranslation("title.topanimegenres")
            val favoriteMangaGenres = context.getTranslation("title.topmangagenres")

            eb.addField(favoriteAnimeGenres, animeStats?.genres()?.withIndex()?.joinToString("\n") { (index, genre) ->
                "${index + 1}. ${genre.genre() ?: "error"}"
            }?.ifEmpty { "/" } ?: "/", true)

            eb.addField(favoriteMangaGenres, mangaStats?.genres()?.withIndex()?.joinToString("\n") { (index, genre) ->
                "${index + 1}. ${genre.genre() ?: "error"}"
            }?.ifEmpty { "/" } ?: "/", true)

            val parts = mutableListOf<String>()

            val animeStatsTitle = context.getTranslation("$root.title.animestats")
            val mangaStatsTitle = context.getTranslation("$root.title.mangastats")


            val animePart = animeStatsTitle + if ((animeStats?.count() ?: 0) > 0) {
                val animeValueStats = context.getTranslation("$root.animestats")
                "\n" + animeValueStats
                    .withVariable("watched", animeStats?.count() ?: 0)
                    .withVariable("epwatched", animeStats?.episodesWatched() ?: 0)
                    .withVariable("meanscore", animeStats?.meanScore()?.toString() ?: "--")
                    .withVariable("standardDeviation", animeStats?.standardDeviation()?.toString() ?: "--")
            } else ""

            val mangaPart = mangaStatsTitle + if ((mangaStats?.count() ?: 0) > 0) {
                val mangaValueStats = context.getTranslation("$root.mangastats")
                "\n" + mangaValueStats
                    .withVariable("read", mangaStats?.count() ?: 0)
                    .withVariable("volread", mangaStats?.volumesRead() ?: 0)
                    .withVariable("chapread", mangaStats?.chaptersRead() ?: 0)
                    .withVariable("meanscore", mangaStats?.meanScore()?.toString() ?: "--")
                    .withVariable("standardDeviation", mangaStats?.standardDeviation()?.toString() ?: "--")
            } else ""

            parts.add(animePart)
            parts.add(mangaPart)

            val otherStats = context.getTranslation("title.otherstats")

            eb.addField(otherStats, parts.joinToString("\n\n"), false)


            user.favourites()?.let outer@{
                it.anime()?.nodes()?.let { animeList ->
                    if (animeList.isEmpty()) return@let
                    val top = animeList.take(5)

                    eb.addField(
                        context.getTranslation("title.favorite.anime"),
                        StringUtils.splitMessage(
                            top.joinToString("\n") { anime ->
                                "⁎ [${anime?.title()?.english() ?: anime?.title()?.romaji()}](${anime?.siteUrl()})"
                            }, 800, MessageEmbed.VALUE_MAX_LENGTH
                        ).first(),
                        true
                    )
                }
                it.manga()?.nodes()?.let { mangaList ->
                    if (mangaList.isEmpty()) return@let

                    eb.addField(
                        context.getTranslation("title.favorite.manga"),
                        StringUtils.splitMessage(
                            mangaList.joinToString("\n") { manga ->
                                "⁎ [${manga?.title()?.english() ?: manga?.title()?.romaji()}](${manga?.siteUrl()})"
                            }, 800, MessageEmbed.VALUE_MAX_LENGTH
                        ).first(),
                        true
                    )
                }
                it.characters()?.nodes()?.let { characters ->
                    if (characters.isEmpty()) return@let

                    eb.addField(
                        context.getTranslation("title.favorite.characters"),
                        StringUtils.splitMessage(
                            characters.joinToString("\n") { character ->
                                "⁎ [${character?.name()?.full()}](${character?.siteUrl()})"
                            }, 800, MessageEmbed.VALUE_MAX_LENGTH
                        ).first(),
                        true
                    )
                }
            }

            sendEmbedRsp(context, eb.build())
        }
    }

    class AnimeArg(parent: String) : AbstractCommand("$parent.anime") {

        init {
            name = "anime"
            aliases = arrayOf("a", "series", "movie", "ova", "ona", "tv")
        }

        override suspend fun execute(context: CommandContext) {
            AniListCommandUtil.searchAnime(context)
        }
    }

    class SearchAnimeArg(parent: String) : AbstractCommand("$parent.searchanime") {

        init {
            name = "searchanime"
            aliases = arrayOf("sa", "searchmovie", "searchova", "searchona")
        }

        override suspend fun execute(context: CommandContext) {
            AniListCommandUtil.searchMedia(context, MediaType.ANIME)
        }
    }

    class CharacterArg(parent: String) : AbstractCommand("$parent.character") {

        init {
            name = "character"
            aliases = arrayOf("c", "char")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val characterName = context.rawArg.take(256)


            context.webManager.aniListApolloClient.query(
                FindCharacterQuery.builder()
                    .name(characterName)
                    .build()
            ).enqueue(object : ApolloCall.Callback<FindCharacterQuery.Data>() {
                override fun onFailure(e: ApolloException) {
                    TaskManager.async(context) {
                        val msg = context.getTranslation("$root.noresult")
                            .withVariable(PLACEHOLDER_ARG, characterName)
                        sendRsp(context, msg)
                    }
                }

                override fun onResponse(response: Response<FindCharacterQuery.Data>) {
                    TaskManager.async(context) {
                        val char: FindCharacterQuery.Character = response.data?.Character() ?: return@async
                        foundCharacter(context, char)
                    }
                }
            })
        }

        suspend fun foundCharacter(context: CommandContext, character: FindCharacterQuery.Character) {
            val nameList = mutableListOf<String>()
            character.name()?.first()?.let { nameList.add(it) }
            character.name()?.last()?.let { nameList.add(it) }
            val fullName = nameList.joinToString(" ")

            val firstname = context.getTranslation("title.firstname")
            val lastname = context.getTranslation("title.lastname")
            val namekanji = context.getTranslation("title.namekanji")
            val alternativenames = context.getTranslation("title.alternativenames")

            val anime = context.getTranslation("title.anime")
            val manga = context.getTranslation("title.manga")

            val eb = Embedder(context)
                .setThumbnail(character.image()?.large())
                .setTitle(fullName, character.siteUrl())
                .setDescription(character.description()?.take(MessageEmbed.TEXT_MAX_LENGTH))
                .addField(firstname, character.name()?.first() ?: "/", true)
                .addField(lastname, character.name()?.last() ?: "/", true)
                .addField(namekanji, character.name()?.native_() ?: "/", true)

            val otherNames = character.name()?.alternative()
            if (otherNames != null && otherNames.isNotEmpty() && (otherNames.size != 1 && otherNames[0].isBlank())) {
                eb.addField(alternativenames, otherNames.joinToString(), true)
            }

            val mediaEdges = character.media()?.edges()
            val animes = mutableListOf<String>()
            val mangas = mutableListOf<String>()

            if (mediaEdges != null) {
                for (edge in mediaEdges) {
                    val node = edge.node() ?: break
                    val type = node.type()
                    val title = node.title()?.romaji() ?: "error"
                    val url = node.siteUrl() ?: "error"
                    val characterRole = edge.characterRole()?.rawValue()?.toUpperWordCase() ?: "?"

                    val list = (if (type == MediaType.ANIME) {
                        animes
                    } else if (type == MediaType.MANGA) {
                        mangas
                    } else break)

                    list.add("[$title]($url) [$characterRole]")
                }

                if (animes.isNotEmpty()) {
                    val split = StringUtils.splitMessage(animes.joinToString("\n"), 600, MessageEmbed.VALUE_MAX_LENGTH)
                    if (split.size == 1)
                        eb.addField(anime, split[0], true)
                    else {
                        eb.addField(anime, split[0], true)
                        eb.addField("$anime..", split[1], true)
                        if (split.size > 2) {
                            val didntfit = context.getTranslation("value.nofitgotourl")
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
                            val didntfit = context.getTranslation("value.nofitgotourl")
                            eb.addField("$manga...", didntfit, true)
                        }
                    }
                }
            }

            eb.setFooter("Favourites ${character.favourites() ?: 0} 💗")

            sendEmbedRsp(context, eb.build())
        }
    }

    override suspend fun execute(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        AniListCommandUtil.searchAnime(context)
    }
}