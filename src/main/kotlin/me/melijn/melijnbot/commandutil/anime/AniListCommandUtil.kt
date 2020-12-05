package me.melijn.melijnbot.commandutil.anime

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import me.melijn.melijnbot.anilist.*
import me.melijn.melijnbot.anilist.fragment.*
import me.melijn.melijnbot.anilist.type.MediaType
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.*
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.util.regex.Pattern

object AniListCommandUtil {


    // User methods
    suspend fun searchUser(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val mediaName = context.rawArg.take(256)

        context.webManager.aniListApolloClient.query(
            SearchUserQuery(mediaName)
        ).enqueue(object : ApolloCall.Callback<SearchUserQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.first().root}.media.noresult")
                        .withVariable(PLACEHOLDER_ARG, mediaName)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<SearchUserQuery.Data>) {
                TaskManager.async(context) {
                    val users = response.data?.page?.users?.filterNotNull() ?: return@async
                    if (users.isEmpty()) {
                        sendRsp(context, "weird")
                        return@async
                    }

                    val eb = Embedder(context)
                    val sb = StringBuilder()
                    for ((index, user) in users.withIndex()) {
                        sb.append("[")
                            .append(index + 1)
                            .append("](")
                            .append(user.siteUrl ?: "")
                            .append(") - ")
                            .appendLine(user.name)
                    }

                    eb
                        .setTitle("Results for: $mediaName")
                        .setDescription(sb.toString())
                        .setFooter("Send the number to select that series or write anything else to cancel")

                    sendEmbedRsp(context, eb.build())

                    context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, {
                        it.channel.idLong == context.channelId && it.author.idLong == context.authorId
                    }, received@{
                        val index = it.message.contentRaw.toIntOrNull()
                        if (index == null || index < 1 || index > users.size) {
                            sendRsp(context, "cancelled search")
                            return@received
                        }

                        getUserById(context, users[index - 1].id)

                    })
                }
            }
        })
    }

    fun getUserByName(context: CommandContext, name: String) {
        context.webManager.aniListApolloClient.query(
            FindUserQuery(name)
        ).enqueue(object : ApolloCall.Callback<FindUserQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<FindUserQuery.Data>) {
                TaskManager.async(context) {
                    val user: UserFragment = response.data?.user?.fragments?.userFragment ?: return@async
                    foundUser(context, user)
                }
            }
        })
    }

    fun getUserById(context: CommandContext, id: Int) {
        context.webManager.aniListApolloClient.query(
            GetUserQuery(id)
        ).enqueue(object : ApolloCall.Callback<GetUserQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, "$id")
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<GetUserQuery.Data>) {
                TaskManager.async(context) {
                    val user: UserFragment = response.data?.user?.fragments?.userFragment ?: return@async
                    foundUser(context, user)
                }
            }
        })
    }

    suspend fun foundUser(context: CommandContext, user: UserFragment) {
        val daoManager = context.daoManager
        val root = context.commandOrder.first().root + ".user"
        val embedColorWrapper = daoManager.embedColorWrapper
        val userEmbedColorWrapper = daoManager.userEmbedColorWrapper
        val guildColor: Int = embedColorWrapper.getColor(context.guildId)
        val userColor = userEmbedColorWrapper.getColor(context.guildId)

        val defaultColor = when {
            userColor != 0 -> userColor
            guildColor != 0 -> guildColor
            else -> context.embedColor
        }

        val eb = Embedder(context) // We're using custom colors below
            .setThumbnail(user.avatar?.large)
            .setTitle(user.name, user.siteUrl)


        val aboutValue = user.about
        if (aboutValue != null && aboutValue.isNotEmpty()) {
            val about = context.getTranslation("title.about")
            eb.setDescription(
                "**$about**\n" + user.about.take(MessageEmbed.TEXT_MAX_LENGTH - 11)
            )
        }

        val chosenColor = when (user.options?.profileColor?.toLowerCase() ?: "null") {
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

        val animeStats = user.statistics?.anime
        val mangaStats = user.statistics?.manga

        val favoriteAnimeGenres = context.getTranslation("title.topanimegenres")
        val favoriteMangaGenres = context.getTranslation("title.topmangagenres")

        eb.addField(favoriteAnimeGenres, animeStats?.genres?.withIndex()?.joinToString("\n") { (index, genre) ->
            "${index + 1}. ${genre?.genre ?: "error"}"
        }?.ifEmpty { "/" } ?: "/", true)

        eb.addField(favoriteMangaGenres, mangaStats?.genres?.withIndex()?.joinToString("\n") { (index, genre) ->
            "${index + 1}. ${genre?.genre ?: "error"}"
        }?.ifEmpty { "/" } ?: "/", true)

        val parts = mutableListOf<String>()

        val animeStatsTitle = context.getTranslation("$root.title.animestats")
        val mangaStatsTitle = context.getTranslation("$root.title.mangastats")


        val animePart = animeStatsTitle + if ((animeStats?.count ?: 0) > 0) {
            val animeValueStats = context.getTranslation("$root.animestats")
            "\n" + animeValueStats
                .withVariable("watched", animeStats?.count ?: 0)
                .withVariable("epwatched", animeStats?.episodesWatched ?: 0)
                .withVariable("meanscore", animeStats?.meanScore?.toString() ?: "--")
                .withVariable("standardDeviation", animeStats?.standardDeviation?.toString() ?: "--")
        } else ""

        val mangaPart = mangaStatsTitle + if ((mangaStats?.count ?: 0) > 0) {
            val mangaValueStats = context.getTranslation("$root.mangastats")
            "\n" + mangaValueStats
                .withVariable("read", mangaStats?.count ?: 0)
                .withVariable("volread", mangaStats?.volumesRead ?: 0)
                .withVariable("chapread", mangaStats?.chaptersRead ?: 0)
                .withVariable("meanscore", mangaStats?.meanScore?.toString() ?: "--")
                .withVariable("standardDeviation", mangaStats?.standardDeviation?.toString() ?: "--")
        } else ""

        parts.add(animePart)
        parts.add(mangaPart)

        val otherStats = context.getTranslation("title.otherstats")

        eb.addField(otherStats, parts.joinToString("\n\n"), false)


        user.favourites?.let outer@{
            it.anime?.nodes?.let { animeList ->
                if (animeList.isEmpty()) return@let
                val top = animeList.take(5)

                eb.addField(
                    context.getTranslation("title.favorite.anime"),
                    StringUtils.splitMessage(
                        top.joinToString("\n") { anime ->
                            "⁎ [${anime?.title?.english ?: anime?.title?.romaji}](${anime?.siteUrl})"
                        }, 800, MessageEmbed.VALUE_MAX_LENGTH
                    ).first(),
                    true
                )
            }
            it.manga?.nodes?.let { mangaList ->
                if (mangaList.isEmpty()) return@let

                eb.addField(
                    context.getTranslation("title.favorite.manga"),
                    StringUtils.splitMessage(
                        mangaList.joinToString("\n") { manga ->
                            "⁎ [${manga?.title?.english ?: manga?.title?.romaji}](${manga?.siteUrl})"
                        }, 800, MessageEmbed.VALUE_MAX_LENGTH
                    ).first(),
                    true
                )
            }
            it.characters?.nodes?.let { characters ->
                if (characters.isEmpty()) return@let

                eb.addField(
                    context.getTranslation("title.favorite.characters"),
                    StringUtils.splitMessage(
                        characters.joinToString("\n") { character ->
                            "⁎ [${character?.name?.full}](${character?.siteUrl})"
                        }, 800, MessageEmbed.VALUE_MAX_LENGTH
                    ).first(),
                    true
                )
            }
        }

        sendEmbedRsp(context, eb.build())
    }


    // Character methods
    suspend fun searchCharacter(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val mediaName = context.rawArg.take(256)

        context.webManager.aniListApolloClient.query(
            SearchCharacterQuery(mediaName)
        ).enqueue(object : ApolloCall.Callback<SearchCharacterQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.first().root}.media.noresult")
                        .withVariable(PLACEHOLDER_ARG, mediaName)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<SearchCharacterQuery.Data>) {
                TaskManager.async(context) {
                    val characters = response.data?.page?.characters?.filterNotNull() ?: return@async
                    if (characters.isEmpty()) {
                        sendRsp(context, "weird")
                        return@async
                    }

                    val eb = Embedder(context)
                    val sb = StringBuilder()
                    for ((index, char) in characters.withIndex()) {
                        sb.append("[")
                            .append(index + 1)
                            .append("](")
                            .append(char.siteUrl ?: "")
                            .append(") - ")
                            .append(char.name?.first ?: "?")
                            .appendLine(char.name?.last?.let { " $it" } ?: "")
                    }

                    eb
                        .setTitle("Results for: $mediaName")
                        .setDescription(sb.toString())
                        .setFooter("Send the number to select that series or write anything else to cancel")

                    sendEmbedRsp(context, eb.build())

                    context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, {
                        it.channel.idLong == context.channelId && it.author.idLong == context.authorId
                    }, received@{
                        val index = it.message.contentRaw.toIntOrNull()
                        if (index == null || index < 1 || index > characters.size) {
                            sendRsp(context, "cancelled search")
                            return@received
                        }

                        getCharacterById(context, characters[index - 1].id)

                    })
                }
            }
        })
    }

    fun getCharacterByName(context: CommandContext, name: String) {
        context.webManager.aniListApolloClient.query(
            FindCharacterQuery(name)
        ).enqueue(object : ApolloCall.Callback<FindCharacterQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<FindCharacterQuery.Data>) {
                TaskManager.async(context) {
                    val mangaMedia: FindCharacterQuery.Character = response.data?.character ?: return@async
                    foundCharacter(context, mangaMedia.fragments.characterFragment)
                }
            }
        })
    }

    fun getCharacterById(context: CommandContext, id: Int) {
        context.webManager.aniListApolloClient.query(
            GetCharacterQuery(id)
        ).enqueue(object : ApolloCall.Callback<GetCharacterQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, "$id")
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<GetCharacterQuery.Data>) {
                TaskManager.async(context) {
                    val mangaMedia: GetCharacterQuery.Character = response.data?.character ?: return@async
                    foundCharacter(context, mangaMedia.fragments.characterFragment)
                }
            }
        })
    }

    suspend fun foundCharacter(context: CommandContext, character: CharacterFragment) {
        val nameList = mutableListOf<String>()
        character.name?.first?.let { nameList.add(it) }
        character.name?.last?.let { nameList.add(it) }
        val fullName = nameList.joinToString(" ")

        val firstname = context.getTranslation("title.firstname")
        val lastname = context.getTranslation("title.lastname")
        val namekanji = context.getTranslation("title.namekanji")
        val alternativenames = context.getTranslation("title.alternativenames")

        val anime = context.getTranslation("title.anime")
        val manga = context.getTranslation("title.manga")

        val eb = Embedder(context)
            .setThumbnail(character.image?.large)
            .setTitle(fullName, character.siteUrl)
            .setDescription(character.description?.take(MessageEmbed.TEXT_MAX_LENGTH))
            .addField(firstname, character.name?.first ?: "/", true)
            .addField(lastname, character.name?.last ?: "/", true)
            .addField(namekanji, character.name?.native_ ?: "/", true)

        val otherNames = character.name?.alternative?.filterNotNull()
        if (otherNames != null && otherNames.isNotEmpty() && (otherNames.size != 1 && otherNames[0].isBlank())) {
            eb.addField(alternativenames, otherNames.joinToString(), true)
        }

        val mediaEdges = character.media?.edges
        val animes = mutableListOf<String>()
        val mangas = mutableListOf<String>()

        if (mediaEdges != null) {
            for (edge in mediaEdges) {
                val node = edge?.node ?: break
                val type = node.type
                val title = node.title?.romaji ?: "error"
                val url = node.siteUrl ?: "error"
                val characterRole = edge.characterRole?.rawValue?.toUpperWordCase() ?: "?"

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

        eb.setFooter("Favourites ${character.favourites ?: 0} 💗")

        sendEmbedRsp(context, eb.build())
    }

    // Anime methods
    suspend fun searchAnime(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val animeName = context.rawArg.take(256)
        getAnimeByName(context, animeName)
    }

    fun getAnimeByName(context: CommandContext, name: String) {
        context.webManager.aniListApolloClient.query(
            FindAnimeQuery(name)
        ).enqueue(object : ApolloCall.Callback<FindAnimeQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<FindAnimeQuery.Data>) {
                TaskManager.async(context) {
                    val anime: FindAnimeQuery.Media = response.data?.media ?: return@async

                    foundAnime(context, anime.toAnilistAnime(anime.fragments.animeFragment))
                }
            }
        })
    }

    fun getAnimeById(context: CommandContext, id: Int) {
        context.webManager.aniListApolloClient.query(
            GetAnimeQuery(id)

        ).enqueue(object : ApolloCall.Callback<GetAnimeQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, "$id")
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<GetAnimeQuery.Data>) {
                TaskManager.async(context) {
                    val anime: GetAnimeQuery.Media = response.data?.media ?: return@async
                    foundAnime(context, anime.toAnilistAnime(anime.fragments.animeFragment))
                }
            }
        })
    }

    suspend fun foundAnime(context: CommandContext, animeMedia: AnilistAnimeMedia) {
        val media = animeMedia.media
        if (media.nsfw == true && context.isFromGuild && !context.textChannel.isNSFW) {
            val msg = context.getTranslation("${context.commandOrder.last().root}.nsfw")
                .withVariable("anime", media.title?.english ?: media.title?.romaji ?: context.rawArg)
            sendRsp(context, msg)
            return
        }
        val eb = Embedder(context)
            .setThumbnail(media.coverImage?.extraLarge)
            .setTitle(media.title?.english ?: media.title?.romaji ?: "?", media.siteUrl)

        var description: String = media.description ?: ""
        val italicRegex = "<i>(.*?)</i>".toRegex()

        for (res in italicRegex.findAll(description)) {
            description = description.replace(
                res.groups[0]?.value
                    ?: "$$$$$$$", "*${res.groups[1]?.value}*"
            )

        }

        if (description.isNotBlank()) {
            eb.setDescription(
                description
                    .replace("<br>", "\n")
                    .replace(("[" + Pattern.quote("\n") + "]{3,15}").toRegex(), "\n\n")
                    .take(MessageEmbed.TEXT_MAX_LENGTH)
            )
        }

        var alias = media.synonyms.joinToString()
        if (alias.isBlank()) alias = "/"


        val genres = context.getTranslation("title.genres")
        val othernames = context.getTranslation("title.othernames")
        val rating = context.getTranslation("title.rating")

        val format = context.getTranslation("title.format")
        val episodes = context.getTranslation("title.episodes")
        val avgepisodelength = context.getTranslation("title.avgepisodelength")


        val status = context.getTranslation("title.status")
        val startdate = context.getTranslation("title.startdate")
        val enddate = context.getTranslation("title.enddate")


        eb.addField(genres, media.genres.joinToString("\n"), true)
            .addField(othernames, alias, true)
            .addField(rating, (media.averageScore?.toString() ?: "?") + "%", true)

            .addField(format, media.format?.toUCC() ?: "/", true)
            .addField(episodes, animeMedia.anime.episodes?.toString() ?: "/", true)
            .addField(avgepisodelength, media.duration?.toString() ?: "/", true)

            .addField(status, media.status?.toUCC() ?: "/", true)
            .addField(startdate, formatDate(media.startDate), true)
            .addField(enddate, formatDate(media.endDate), true)

        val next = media.nextAiringEpisode
        if (next != null) {
            val nextepisode = context.getTranslation("title.nextepisode")
            val airingat = context.getTranslation("title.airingat")

            val epochMillis = next.airingAt * 1000L
            val dateTime = epochMillis.asEpochMillisToDateTime(context.getTimeZoneId())
            eb.addField(nextepisode, next.episode.toString(), true)
            eb.addField(airingat, dateTime, true)
        }

        val favourites = context.getTranslation("footer.favourites")
            .withVariable("amount", media.favourites ?: 0)
        eb.setFooter(favourites)

        sendEmbedRsp(context, eb.build())
    }

    // Manga methods
    suspend fun searchManga(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val animeName = context.rawArg.take(256)
        getMangaByName(context, animeName)
    }

    fun getMangaByName(context: CommandContext, name: String) {
        context.webManager.aniListApolloClient.query(
            FindMangaQuery(name)
        ).enqueue(object : ApolloCall.Callback<FindMangaQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, name)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<FindMangaQuery.Data>) {
                TaskManager.async(context) {
                    val mangaMedia: FindMangaQuery.Media = response.data?.media ?: return@async
                    foundManga(context, mangaMedia.toAnilistManga(mangaMedia.fragments.mangaFragment))
                }
            }
        })
    }

    fun getMangaById(context: CommandContext, id: Int) {
        context.webManager.aniListApolloClient.query(
            GetMangaQuery(id)
        ).enqueue(object : ApolloCall.Callback<GetMangaQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.last().root}.noresult")
                        .withVariable(PLACEHOLDER_ARG, "$id")
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<GetMangaQuery.Data>) {
                TaskManager.async(context) {
                    val mangaMedia: GetMangaQuery.Media = response.data?.media ?: return@async
                    foundManga(context, mangaMedia.toAnilistManga(mangaMedia.fragments.mangaFragment))
                }
            }
        })
    }

    suspend fun foundManga(context: CommandContext, mangaMedia: AnilistMangaMedia) {
        val media = mangaMedia.media
        if (media.nsfw == true && context.isFromGuild && !context.textChannel.isNSFW) {
            val msg = context.getTranslation("${context.commandOrder.last().root}.nsfw")
                .withVariable("manga", media.title?.english ?: media.title?.romaji ?: context.rawArg)
            sendRsp(context, msg)
            return
        }

        val eb = Embedder(context)
            .setThumbnail(media.coverImage?.extraLarge)
            .setTitle(media.title?.english ?: media.title?.romaji ?: "?", media.siteUrl)

        var description: String = media.description ?: ""
        val italicRegex = "<i>(.*?)</i>".toRegex()

        for (res in italicRegex.findAll(description)) {
            description = description.replace(
                res.groups[0]?.value
                    ?: "$$$$$$$", "*${res.groups[1]?.value}*"
            )

        }

        if (description.isNotBlank())
            eb.setDescription(
                description
                    .replace("<br>", "\n")
                    .replace(("[" + Pattern.quote("\n") + "]{3,15}").toRegex(), "\n\n")
                    .take(MessageEmbed.TEXT_MAX_LENGTH)
            )

        var alias = media.synonyms.joinToString()
        if (alias.isBlank()) alias = "/"

        val genres = context.getTranslation("title.genres")
        val othernames = context.getTranslation("title.othernames")
        val rating = context.getTranslation("title.rating")

        val format = context.getTranslation("title.format")
        val volumes = context.getTranslation("title.volumes")
        val chapters = context.getTranslation("title.chapters")

        val status = context.getTranslation("title.status")
        val startdate = context.getTranslation("title.startdate")
        val enddate = context.getTranslation("title.enddate")


        eb.addField(genres, media.genres.joinToString("\n").let { if (it.isEmpty()) "/" else it }, true)
        eb.addField(othernames, alias, true)
        eb.addField(rating, (media.averageScore?.toString() ?: "?") + "%", true)

        eb.addField(format, media.format?.toUCC() ?: "/", true)
        eb.addField(volumes, "${mangaMedia.manga.volumes ?: 0}", true)
        eb.addField(chapters, "${mangaMedia.manga.chapters ?: 0}", true)


        eb.addField(status, media.status?.toUCC() ?: "/", true)
        eb.addField(startdate, formatDate(media.startDate), true)
        eb.addField(enddate, formatDate(media.endDate), true)

        val next = media.nextAiringEpisode
        if (next != null) {
            val nextepisode = context.getTranslation("title.nextepisode")
            val airingat = context.getTranslation("title.airingat")

            val epochMillis = next.airingAt * 1000L
            val dateTime = epochMillis.asEpochMillisToDateTime(context.getTimeZoneId())
            eb.addField(nextepisode, next.episode.toString(), true)
            eb.addField(airingat, dateTime, true)
        }

        val favourites = context.getTranslation("footer.favourites")
            .withVariable("amount", media.favourites ?: 0)
        eb.setFooter(favourites)

        sendEmbedRsp(context, eb.build())
    }


    // Media methods
    suspend fun searchMedia(context: CommandContext, mediaType: MediaType) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val mediaName = context.rawArg.take(256)

        context.webManager.aniListApolloClient.query(
            SearchMediaQuery(mediaName, mediaType)
        ).enqueue(object : ApolloCall.Callback<SearchMediaQuery.Data>() {
            override fun onFailure(e: ApolloException) {
                TaskManager.async(context) {
                    val msg = context.getTranslation("${context.commandOrder.first().root}.media.noresult")
                        .withVariable(PLACEHOLDER_ARG, mediaName)
                    sendRsp(context, msg)
                }
            }

            override fun onResponse(response: Response<SearchMediaQuery.Data>) {
                TaskManager.async(context) {
                    val medias = response.data?.page?.media?.filterNotNull() ?: return@async
                    if (medias.isEmpty()) {
                        sendRsp(context, "weird")
                        return@async
                    }

                    val eb = Embedder(context)
                    val sb = StringBuilder()
                    for ((index, anime) in medias.withIndex()) {
                        sb.append("[").append(index + 1).append("](").append(
                            anime.siteUrl
                                ?: ""
                        ).append(") - ").append(
                            anime.title?.romaji
                                ?: "?"
                        ).append(anime.title?.english?.let { " | $it" } ?: "").append(" ` ")
                            .append(anime.favourites ?: 0).appendLine(" \uD83D\uDC97`")
                    }

                    eb
                        .setTitle("Results for: $mediaName")
                        .setDescription(sb.toString())
                        .setFooter("Send the number to select that series or write anything else to cancel")

                    sendEmbedRsp(context, eb.build())

                    context.container.eventWaiter.waitFor(MessageReceivedEvent::class.java, {
                        it.channel.idLong == context.channelId && it.author.idLong == context.authorId
                    }, received@{
                        val index = it.message.contentRaw.toIntOrNull()
                        if (index == null || index < 1 || index > medias.size) {
                            sendRsp(context, "cancelled search")
                            return@received
                        }

                        if (mediaType == MediaType.ANIME) {
                            getAnimeById(context, medias[index - 1].id)
                        } else if (mediaType == MediaType.MANGA) {
                            getMangaById(context, medias[index - 1].id)
                        }
                    })
                }
            }
        })
    }

    fun GetAnimeQuery.Media.toAnilistAnime(animeFagment: AnimeFragment): AnilistAnimeMedia {
        val media = this.fragments.mediaFragment
        return AnilistAnimeMedia(
            media.toAnilistMedia(),
            AnilistAnime(animeFagment.episodes)
        )
    }

    fun GetMangaQuery.Media.toAnilistManga(mangaFragment: MangaFragment): AnilistMangaMedia {
        val media = this.fragments.mediaFragment
        return AnilistMangaMedia(
            media.toAnilistMedia(),
            AnilistManga(
                mangaFragment.chapters,
                mangaFragment.volumes
            )
        )
    }

    fun FindAnimeQuery.Media.toAnilistAnime(animeFagment: AnimeFragment): AnilistAnimeMedia {
        val media = this.fragments.mediaFragment
        return AnilistAnimeMedia(
            media.toAnilistMedia(),
            AnilistAnime(animeFagment.episodes)
        )
    }

    fun FindMangaQuery.Media.toAnilistManga(mangaFragment: MangaFragment): AnilistMangaMedia {
        val media = this.fragments.mediaFragment
        return AnilistMangaMedia(
            media.toAnilistMedia(),
            AnilistManga(
                mangaFragment.chapters,
                mangaFragment.volumes
            )
        )
    }

    fun MediaFragment.toAnilistMedia(): AnilistMedia {
        return AnilistMedia(
            AnilistTitle(
                this.title?.romaji,
                this.title?.english,
                this.title?.native_,
                this.title?.userPreferred
            ),
            this.synonyms?.filterNotNull() ?: emptyList(),
            this.type,
            this.status,
            AnilistDate(
                this.startDate?.year,
                this.startDate?.month,
                this.startDate?.day
            ),
            AnilistDate(
                this.endDate?.year,
                this.endDate?.month,
                this.endDate?.day
            ),
            this.format,
            this.description,
            AnilistCoverImage(
                this.coverImage?.extraLarge,
                this.coverImage?.color
            ),
            this.duration,
            this.siteUrl,
            this.averageScore,
            this.genres?.filterNotNull() ?: emptyList(),
            AnilistTrailer(
                this.trailer?.site
            ),
            this.favourites,
            this.studios?.edges?.filterNotNull()?.map {
                AnilistStudio(
                    it.isMain,
                    it.node?.name,
                    it.node?.siteUrl
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode?.let {
                AnilistAiringEpisode(
                    it.episode,
                    it.airingAt
                )
            }
        )
    }

    // Util methods
    private fun formatDate(date: AnilistDate?): String {
        if (date == null) return "/"
        val year = date.year
        val month = date.month
        val day = date.day
        if (year == null || month == null || day == null) return "/"
        return "$year-$month-$day"
    }

}