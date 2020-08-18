package me.melijn.melijnbot.internals.models

import me.melijn.melijnbot.anilist.type.MediaFormat
import me.melijn.melijnbot.anilist.type.MediaStatus
import me.melijn.melijnbot.anilist.type.MediaType

data class AnilistMedia(
    val title: AnilistTitle?,
    val synonyms: List<String>,
    val type: MediaType?,
    val status: MediaStatus?,
    val startDate: AnilistDate?,
    val endDate: AnilistDate?,
    val format: MediaFormat?,
    val description: String?,
    val coverImage: AnilistCoverImage?,
    val episodes: Int?,
    val duration: Int?,
    val siteUrl: String?,
    val averageScore: Int?,
    val genres: List<String>,
    val trailer: AnilistTrailer?,
    val favourites: Int?,
    val studios: List<AnilistStudio>,
    val nsfw: Boolean?,
    val nextAiringEpisode: AnilistAiringEpisode?
) {
    data class AnilistTitle(
        val romaji: String?,
        val english: String?,
        val native: String?,
        val userPreferred: String?
    )

    data class AnilistDate(
        val year: Int?,
        val month: Int?,
        val day: Int?
    )

    data class AnilistCoverImage(
        val extraLarge: String?,
        val color: String?
    )

    data class AnilistTrailer(
        val site: String?
    )

    data class AnilistStudio(
        val isMain: Boolean,
        val name: String?,
        val siteUrl: String?
    )

    data class AnilistAiringEpisode(
        val airingAt: Int,
        val episode: Int
    )
}