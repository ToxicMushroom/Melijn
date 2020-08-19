package me.melijn.melijnbot.commandutil.anime

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import me.melijn.melijnbot.anilist.*
import me.melijn.melijnbot.anilist.type.MediaType
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.*
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.toUCC
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.regex.Pattern

object AniListCommandUtil {

    suspend fun searchMedia(context: CommandContext, mediaType: MediaType) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val mediaName = context.rawArg.take(256)

        context.webManager.aniListApolloClient.query(
            SearchMediaQuery.builder()
                .name(mediaName)
                .type(mediaType)
                .build()
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
                    val medias = response.data?.Page()?.media() ?: return@async
                    if (medias.isEmpty()) {
                        sendRsp(context, "weird")
                        return@async
                    }

                    val eb = Embedder(context)
                    val sb = StringBuilder()
                    for ((index, anime) in medias.withIndex()) {
                        sb.append("[").append(index + 1).append("](").append(anime.siteUrl()
                            ?: "").append(") - ").append(anime.title()?.romaji()
                            ?: "?").append(anime.title()?.english()?.let { " | $it" } ?: "").append(" ` ")
                            .append(anime.favourites() ?: 0).appendLine(" \uD83D\uDC97`")
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
                            getAnimeById(context, medias[index - 1]?.id()
                                ?: return@received)
                        } else if (mediaType == MediaType.MANGA) {
                            getMangaById(context, medias[index - 1]?.id()
                                ?: return@received)
                        }
                    })
                }
            }
        })
    }

    suspend fun searchManga(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val animeName = context.rawArg.take(256)
        getMangaByName(context, animeName)
    }

    suspend fun searchAnime(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val animeName = context.rawArg.take(256)
        getAnimeByName(context, animeName)
    }

    suspend fun getAnimeByName(context: CommandContext, name: String) {
        context.webManager.aniListApolloClient.query(
            FindAnimeQuery.builder()
                .name(name)
                .build()
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
                    val char: FindAnimeQuery.Media = response.data?.Media() ?: return@async
                    foundAnime(context, char.toAnilistMedia())
                }
            }
        })
    }

    suspend fun getAnimeById(context: CommandContext, id: Int) {
        context.webManager.aniListApolloClient.query(
            GetAnimeQuery.builder()
                .id(id)
                .build()
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
                    val char: GetAnimeQuery.Media = response.data?.Media() ?: return@async
                    foundAnime(context, char.toAnilistMedia())
                }
            }
        })
    }

    suspend fun getMangaById(context: CommandContext, id: Int) {
        context.webManager.aniListApolloClient.query(
            GetMangaQuery.builder()
                .id(id)
                .build()
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
                    val char: GetMangaQuery.Media = response.data?.Media() ?: return@async
                    foundManga(context, char.toAnilistMedia())
                }
            }
        })
    }

    suspend fun getMangaByName(context: CommandContext, name: String) {
        context.webManager.aniListApolloClient.query(
            FindMangaQuery.builder()
                .name(name)
                .build()
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
                    val char: FindMangaQuery.Media = response.data?.Media() ?: return@async
                    foundManga(context, char.toAnilistMedia())
                }
            }
        })
    }


    suspend fun foundAnime(context: CommandContext, animeMedia: AnilistAnimeMedia) {
        if (animeMedia.nsfw == true && context.isFromGuild && !context.textChannel.isNSFW) {
            val msg = context.getTranslation("${context.commandOrder.last().root}.nsfw")
                .withVariable("anime", animeMedia.title?.english ?: animeMedia.title?.romaji ?: context.rawArg)
            sendRsp(context, msg)
            return
        }
        val eb = Embedder(context)
            .setThumbnail(animeMedia.coverImage?.extraLarge)
            .setTitle(animeMedia.title?.english ?: animeMedia.title?.romaji ?: "?", animeMedia.siteUrl)

        var description: String = animeMedia.description ?: ""
        val italicRegex = "<i>(.*?)</i>".toRegex()

        for (res in italicRegex.findAll(description)) {
            description = description.replace(res.groups[0]?.value
                ?: "$$$$$$$", "*${res.groups[1]?.value}*")

        }

        if (description.isNotBlank()) {
            eb.setDescription(
                description
                    .replace("<br>", "\n")
                    .replace(("[" + Pattern.quote("\n") + "]{3,15}").toRegex(), "\n\n")
                    .take(MessageEmbed.TEXT_MAX_LENGTH)
            )
        }

        var alias = animeMedia.synonyms.joinToString()
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


        eb.addField(genres, animeMedia.genres.joinToString("\n") ?: "/", true)
            .addField(othernames, alias, true)
            .addField(rating, (animeMedia.averageScore?.toString() ?: "?") + "%", true)

            .addField(format, animeMedia.format?.toUCC() ?: "/", true)
            .addField(episodes, animeMedia.episodes?.toString() ?: "/", true)
            .addField(avgepisodelength, animeMedia.duration?.toString() ?: "/", true)

            .addField(status, animeMedia.status?.toUCC() ?: "/", true)
            .addField(startdate, formatDate(animeMedia.startDate), true)
            .addField(enddate, formatDate(animeMedia.endDate), true)

        val next = animeMedia.nextAiringEpisode
        if (next != null) {
            val nextepisode = context.getTranslation("title.nextepisode")
            val airingat = context.getTranslation("title.airingat")

            val epochMillis = next.airingAt * 1000L
            val dateTime = epochMillis.asEpochMillisToDateTime(context.getTimeZoneId())
            eb.addField(nextepisode, next.episode.toString(), true)
            eb.addField(airingat, dateTime, true)
        }

        val favourites = context.getTranslation("footer.favourites")
            .withVariable("amount", animeMedia.favourites ?: 0)
        eb.setFooter(favourites)

        sendEmbedRsp(context, eb.build())
    }

    suspend fun foundManga(context: CommandContext, animeMedia: AnilistMangaMedia) {
        if (animeMedia.nsfw == true && context.isFromGuild && !context.textChannel.isNSFW) {
            val msg = context.getTranslation("${context.commandOrder.last().root}.nsfw")
                .withVariable("manga", animeMedia.title?.english ?: animeMedia.title?.romaji ?: context.rawArg)
            sendRsp(context, msg)
            return
        }

        val eb = Embedder(context)
            .setThumbnail(animeMedia.coverImage?.extraLarge)
            .setTitle(animeMedia.title?.english ?: animeMedia.title?.romaji ?: "?", animeMedia.siteUrl)

        var description: String = animeMedia.description ?: ""
        val italicRegex = "<i>(.*?)</i>".toRegex()

        for (res in italicRegex.findAll(description)) {
            description = description.replace(res.groups[0]?.value
                ?: "$$$$$$$", "*${res.groups[1]?.value}*")

        }

        if (description.isNotBlank())
            eb.setDescription(
                description
                    .replace("<br>", "\n")
                    .replace(("[" + Pattern.quote("\n") + "]{3,15}").toRegex(), "\n\n")
                    .take(MessageEmbed.TEXT_MAX_LENGTH)
            )

        var alias = animeMedia.synonyms.joinToString()
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


        eb.addField(genres, animeMedia.genres.joinToString("\n").let { if (it.isEmpty()) "/" else it }, true)
        eb.addField(othernames, alias, true)
        eb.addField(rating, (animeMedia.averageScore?.toString() ?: "?") + "%", true)

        eb.addField(format, animeMedia.format?.toUCC() ?: "/", true)
        eb.addField(volumes, "${animeMedia.volumes ?: 0}", true)
        eb.addField(chapters, "${animeMedia.chapters ?: 0}", true)


        eb.addField(status, animeMedia.status?.toUCC() ?: "/", true)
        eb.addField(startdate, formatDate(animeMedia.startDate), true)
        eb.addField(enddate, formatDate(animeMedia.endDate), true)

        val next = animeMedia.nextAiringEpisode
        if (next != null) {
            val nextepisode = context.getTranslation("title.nextepisode")
            val airingat = context.getTranslation("title.airingat")

            val epochMillis = next.airingAt * 1000L
            val dateTime = epochMillis.asEpochMillisToDateTime(context.getTimeZoneId())
            eb.addField(nextepisode, next.episode.toString(), true)
            eb.addField(airingat, dateTime, true)
        }

        val favourites = context.getTranslation("footer.favourites")
            .withVariable("amount", animeMedia.favourites ?: 0)
        eb.setFooter(favourites)

        sendEmbedRsp(context, eb.build())
    }

    private fun formatDate(date: AnilistDate?): String {
        if (date == null) return "/"
        val year = date.year
        val month = date.month
        val day = date.day
        if (year == null || month == null || day == null) return "/"
        return "$year-$month-$day"
    }

    fun GetAnimeQuery.Media.toAnilistMedia(): AnilistAnimeMedia {
        return AnilistAnimeMedia(
            AnilistTitle(
                this.title()?.romaji(),
                this.title()?.english(),
                this.title()?.native_(),
                this.title()?.userPreferred()
            ),
            this.synonyms()?.filterNotNull() ?: emptyList(),
            this.type(),
            this.status(),
            AnilistDate(
                this.startDate()?.year(),
                this.startDate()?.month(),
                this.startDate()?.day()
            ),
            AnilistDate(
                this.endDate()?.year(),
                this.endDate()?.month(),
                this.endDate()?.day()
            ),
            this.format(),
            this.description(),
            AnilistCoverImage(
                this.coverImage()?.extraLarge(),
                this.coverImage()?.color()
            ),
            this.episodes(),
            this.duration(),
            this.siteUrl(),
            this.averageScore(),
            this.genres()?.filterNotNull() ?: emptyList(),
            AnilistTrailer(
                this.trailer()?.site()
            ),
            this.favourites(),
            this.studios()?.edges()?.map {
                AnilistStudio(
                    it.isMain,
                    it.node()?.name(),
                    it.node()?.siteUrl()
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode()?.let {
                AnilistAiringEpisode(
                    it.episode(),
                    it.airingAt()
                )
            }
        )
    }

    fun FindAnimeQuery.Media.toAnilistMedia(): AnilistAnimeMedia {
        return AnilistAnimeMedia(
            AnilistTitle(
                this.title()?.romaji(),
                this.title()?.english(),
                this.title()?.native_(),
                this.title()?.userPreferred()
            ),
            this.synonyms()?.filterNotNull() ?: emptyList(),
            this.type(),
            this.status(),
            AnilistDate(
                this.startDate()?.year(),
                this.startDate()?.month(),
                this.startDate()?.day()
            ),
            AnilistDate(
                this.endDate()?.year(),
                this.endDate()?.month(),
                this.endDate()?.day()
            ),
            this.format(),
            this.description(),
            AnilistCoverImage(
                this.coverImage()?.extraLarge(),
                this.coverImage()?.color()
            ),
            this.episodes(),
            this.duration(),
            this.siteUrl(),
            this.averageScore(),
            this.genres()?.filterNotNull() ?: emptyList(),
            AnilistTrailer(
                this.trailer()?.site()
            ),
            this.favourites(),
            this.studios()?.edges()?.map {
                AnilistStudio(
                    it.isMain,
                    it.node()?.name(),
                    it.node()?.siteUrl()
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode()?.let {
                AnilistAiringEpisode(
                    it.episode(),
                    it.airingAt()
                )
            }
        )
    }

    fun GetMangaQuery.Media.toAnilistMedia(): AnilistMangaMedia {
        return AnilistMangaMedia(
            AnilistTitle(
                this.title()?.romaji(),
                this.title()?.english(),
                this.title()?.native_(),
                this.title()?.userPreferred()
            ),
            this.synonyms()?.filterNotNull() ?: emptyList(),
            this.type(),
            this.status(),
            AnilistDate(
                this.startDate()?.year(),
                this.startDate()?.month(),
                this.startDate()?.day()
            ),
            AnilistDate(
                this.endDate()?.year(),
                this.endDate()?.month(),
                this.endDate()?.day()
            ),
            this.format(),
            this.description(),
            AnilistCoverImage(
                this.coverImage()?.extraLarge(),
                this.coverImage()?.color()
            ),
            this.chapters(),
            this.volumes(),
            this.duration(),
            this.siteUrl(),
            this.averageScore(),
            this.genres()?.filterNotNull() ?: emptyList(),
            AnilistTrailer(
                this.trailer()?.site()
            ),
            this.favourites(),
            this.studios()?.edges()?.map {
                AnilistStudio(
                    it.isMain,
                    it.node()?.name(),
                    it.node()?.siteUrl()
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode()?.let {
                AnilistAiringEpisode(
                    it.episode(),
                    it.airingAt()
                )
            }
        )
    }

    fun FindMangaQuery.Media.toAnilistMedia(): AnilistMangaMedia {
        return AnilistMangaMedia(
            AnilistTitle(
                this.title()?.romaji(),
                this.title()?.english(),
                this.title()?.native_(),
                this.title()?.userPreferred()
            ),
            this.synonyms()?.filterNotNull() ?: emptyList(),
            this.type(),
            this.status(),
            AnilistDate(
                this.startDate()?.year(),
                this.startDate()?.month(),
                this.startDate()?.day()
            ),
            AnilistDate(
                this.endDate()?.year(),
                this.endDate()?.month(),
                this.endDate()?.day()
            ),
            this.format(),
            this.description(),
            AnilistCoverImage(
                this.coverImage()?.extraLarge(),
                this.coverImage()?.color()
            ),
            this.chapters(),
            this.volumes(),
            this.duration(),
            this.siteUrl(),
            this.averageScore(),
            this.genres()?.filterNotNull() ?: emptyList(),
            AnilistTrailer(
                this.trailer()?.site()
            ),
            this.favourites(),
            this.studios()?.edges()?.map {
                AnilistStudio(
                    it.isMain,
                    it.node()?.name(),
                    it.node()?.siteUrl()
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode()?.let {
                AnilistAiringEpisode(
                    it.episode(),
                    it.airingAt()
                )
            }
        )
    }
}