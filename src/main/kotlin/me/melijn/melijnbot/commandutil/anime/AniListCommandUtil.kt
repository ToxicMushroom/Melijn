package me.melijn.melijnbot.commandutil.anime

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import me.melijn.melijnbot.anilist.FindAnimeQuery
import me.melijn.melijnbot.anilist.GetAnimeQuery
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.models.AnilistMedia
import me.melijn.melijnbot.internals.threading.TaskManager
import me.melijn.melijnbot.internals.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.internals.utils.asEpochMillisToDateTime
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
import me.melijn.melijnbot.internals.utils.toUCC
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.entities.MessageEmbed
import java.util.regex.Pattern

object AniListCommandUtil {
    suspend fun searchAnime(context: CommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        val animeName = context.rawArg.take(256)
        searchAnime(context, animeName)
    }

    suspend fun searchAnime(context: CommandContext, name: String) {
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

    suspend fun searchAnime(context: CommandContext, id: Int) {
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

    suspend fun foundAnime(context: CommandContext, media: AnilistMedia) {
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


        eb.addField(genres, media.genres.joinToString("\n") ?: "/", true)
            .addField(othernames, alias, true)
            .addField(rating, (media.averageScore?.toString() ?: "?") + "%", true)

            .addField(format, media.format?.toUCC() ?: "/", true)
            .addField(episodes, media.episodes?.toString() ?: "/", true)
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

    private fun formatDate(date: AnilistMedia.AnilistDate?): String {
        if (date == null) return "/"
        val year = date.year
        val month = date.month
        val day = date.day
        if (year == null || month == null || day == null) return "/"
        return "$year-$month-$day"
    }

    fun GetAnimeQuery.Media.toAnilistMedia(): AnilistMedia {
        return AnilistMedia(
            AnilistMedia.AnilistTitle(
                this.title()?.romaji(),
                this.title()?.english(),
                this.title()?.native_(),
                this.title()?.userPreferred()
            ),
            this.synonyms()?.filterNotNull() ?: emptyList(),
            this.type(),
            this.status(),
            AnilistMedia.AnilistDate(
                this.startDate()?.year(),
                this.startDate()?.month(),
                this.startDate()?.day()
            ),
            AnilistMedia.AnilistDate(
                this.endDate()?.year(),
                this.endDate()?.month(),
                this.endDate()?.day()
            ),
            this.format(),
            this.description(),
            AnilistMedia.AnilistCoverImage(
                this.coverImage()?.extraLarge(),
                this.coverImage()?.color()
            ),
            this.episodes(),
            this.duration(),
            this.siteUrl(),
            this.averageScore(),
            this.genres()?.filterNotNull() ?: emptyList(),
            AnilistMedia.AnilistTrailer(
                this.trailer()?.site()
            ),
            this.favourites(),
            this.studios()?.edges()?.map {
                AnilistMedia.AnilistStudio(
                    it.isMain,
                    it.node()?.name(),
                    it.node()?.siteUrl()
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode()?.let {
                AnilistMedia.AnilistAiringEpisode(
                    it.episode(),
                    it.airingAt()
                )
            }
        )
    }

    fun FindAnimeQuery.Media.toAnilistMedia(): AnilistMedia {
        return AnilistMedia(
            AnilistMedia.AnilistTitle(
                this.title()?.romaji(),
                this.title()?.english(),
                this.title()?.native_(),
                this.title()?.userPreferred()
            ),
            this.synonyms()?.filterNotNull() ?: emptyList(),
            this.type(),
            this.status(),
            AnilistMedia.AnilistDate(
                this.startDate()?.year(),
                this.startDate()?.month(),
                this.startDate()?.day()
            ),
            AnilistMedia.AnilistDate(
                this.endDate()?.year(),
                this.endDate()?.month(),
                this.endDate()?.day()
            ),
            this.format(),
            this.description(),
            AnilistMedia.AnilistCoverImage(
                this.coverImage()?.extraLarge(),
                this.coverImage()?.color()
            ),
            this.episodes(),
            this.duration(),
            this.siteUrl(),
            this.averageScore(),
            this.genres()?.filterNotNull() ?: emptyList(),
            AnilistMedia.AnilistTrailer(
                this.trailer()?.site()
            ),
            this.favourites(),
            this.studios()?.edges()?.map {
                AnilistMedia.AnilistStudio(
                    it.isMain,
                    it.node()?.name(),
                    it.node()?.siteUrl()
                )
            } ?: emptyList(),
            this.isAdult,
            this.nextAiringEpisode()?.let {
                AnilistMedia.AnilistAiringEpisode(
                    it.episode(),
                    it.airingAt()
                )
            }
        )
    }
}