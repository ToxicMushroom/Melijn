package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.commands.utility.toUniversalDateFormat
import me.melijn.melijnbot.commands.utility.toUniversalDateTimeFormat
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import moe.ganen.jikankt.JikanKt
import moe.ganen.jikankt.exception.JikanException
import net.dv8tion.jda.api.entities.MessageEmbed
import java.lang.Integer.min

class MyAnimeListCommand : AbstractCommand("command.myanimelist") {

    val seriesArg: SeriesArg

    init {
        id = 158
        name = "myAnimeList"
        aliases = arrayOf("mal")
        seriesArg = SeriesArg(root)
        children = arrayOf(
            UserArg(root),
            seriesArg
        )
        commandCategory = CommandCategory.ANIME
    }


    class SeriesArg(parent: String) : AbstractCommand("$parent.series") {

        init {
            name = "series"
            aliases = arrayOf("anime")
        }

        override suspend fun execute(context: CommandContext) {
            showSeries(context)
        }

        suspend fun showSeries(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val series = context.rawArg.substring(0, min(256, context.rawArg.length))
            try {
                val result = JikanKt.searchAnime(series).results?.firstOrNull()
                if (result == null) {
                    val msg = context.getTranslation("$root.noresult")
                        .replace(PLACEHOLDER_ARG, context.args[0])
                    sendMsg(context, msg)
                    return
                }

                val missing = context.getTranslation("missing")

                val rating = context.getTranslation("rating")
                val episodes = context.getTranslation("episodes")
                val type = context.getTranslation("$root.type")

                val startDate = context.getTranslation("startdate")
                val endDate = context.getTranslation("enddate")
                val airing = context.getTranslation("airing")
                val description = context.getTranslation("description")

                val airingValue = result.airing?.let {
                    context.getTranslation(if (it) {
                        "yes"
                    } else {
                        "no"
                    })
                } ?: context.getTranslation("unknown")

                val eb = Embedder(context)
                eb.setTitle(result.title ?: missing, result.url)

                eb.addField(type, result.type?.toString() ?: missing, true)
                eb.addField(episodes, "${result.episodes ?: missing}", true)
                eb.addField(rating, "${result.score ?: "?"}/10.0", true)

                eb.addField(startDate, result.startDate?.toUniversalDateFormat() ?: "/", true)
                eb.addField(endDate, result.endDate?.toUniversalDateFormat() ?: "/", true)
                eb.addField(airing, airingValue, true)

                eb.addField(description, result.synopsis ?: missing, false)

                eb.setThumbnail(result.imageUrl)

                sendEmbed(context, eb.build())

            } catch (e: JikanException) {
                val msg = context.getTranslation("$root.noresult")
                    .replace(PLACEHOLDER_ARG, context.args[0])
                sendMsg(context, msg)
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

            val user = context.rawArg.substring(0, min(256, context.rawArg.length))

            try {
                val result = JikanKt.getUser(user)

                val about = context.getTranslation("about")
                val joinDate = context.getTranslation("joindate")


                val eb = Embedder(context)
                eb.setTitle("MAL User: ${result.username}", result.url)

                eb.addField(joinDate, result.joined?.toUniversalDateFormat() ?: "/", true)
                result.location?.let {
                    eb.addField(context.getTranslation("location"), it, true)
                }
                result.birthday?.let {
                    eb.addField(context.getTranslation("birthday"), it.toUniversalDateFormat(), true)
                }
                result.gender?.let {
                    eb.addField(context.getTranslation("gender"), it, true)
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
                    eb.addField(context.getTranslation("lastonline"), it.toUniversalDateTimeFormat(), false)
                }


                eb.setThumbnail(result.imageUrl)
                sendEmbed(context, eb.build())
            } catch (e: JikanException) {
                val msg = context.getTranslation("$root.noresult")
                    .replace(PLACEHOLDER_ARG, context.args[0])
                sendMsg(context, msg)
                return
            }
        }
    }

    override suspend fun execute(context: CommandContext) {
        seriesArg.showSeries(context)
    }
}
