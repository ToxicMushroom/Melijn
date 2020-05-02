package me.melijn.melijnbot.commands.anime

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import me.melijn.melijnbot.anilist.FindCharacterQuery
import me.melijn.melijnbot.anilist.type.MediaType
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.PLACEHOLDER_ARG
import me.melijn.melijnbot.objects.utils.StringUtils
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.sendMsg
import me.melijn.melijnbot.objects.utils.sendSyntax
import net.dv8tion.jda.api.entities.MessageEmbed

class AniListCommand : AbstractCommand("command.anilist") {

    init {
        id = 165
        name = "aniList"
        aliases = arrayOf("al")
        val animeArg = AnimeArg(root)
        children = arrayOf(
            animeArg,
            CharacterArg(root)
        )
        commandCategory = CommandCategory.ANIME
    }

    class AnimeArg(parent: String) : AbstractCommand("$parent.anime") {

        init {
            name = "anime"
            aliases = arrayOf("series", "movie", "ova", "ona")
        }

        override suspend fun execute(context: CommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val characterName = context.rawArg

            context.webManager.aniListApolloClient.query(
                FindCharacterQuery.builder()
                    .name(characterName)
                    .build()
            ).enqueue(object : ApolloCall.Callback<FindCharacterQuery.Data>() {
                override fun onFailure(e: ApolloException) {
                    context.taskManager.async {
                        val msg = context.getTranslation("$root.noresult")
                            .replace(PLACEHOLDER_ARG, characterName)
                        sendMsg(context, msg)
                    }
                }

                override fun onResponse(response: Response<FindCharacterQuery.Data>) {
                    context.taskManager.async {
                        val char: FindCharacterQuery.Character = response.data?.Character() ?: return@async
                        foundSeries(context, char)
                    }
                }
            })
        }
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

            val characterName = context.rawArg


            context.webManager.aniListApolloClient.query(
                FindCharacterQuery.builder()
                    .name(characterName)
                    .build()
            ).enqueue(object : ApolloCall.Callback<FindCharacterQuery.Data>() {
                override fun onFailure(e: ApolloException) {
                    context.taskManager.async {
                        val msg = context.getTranslation("$root.noresult")
                            .replace(PLACEHOLDER_ARG, characterName)
                        sendMsg(context, msg)
                    }
                }

                override fun onResponse(response: Response<FindCharacterQuery.Data>) {
                    context.taskManager.async {
                        val char: FindCharacterQuery.Character = response.data?.Character() ?: return@async
                        foundCharacter(context, char)
                    }
                }
            })
        }

        suspend fun foundCharacter(context: CommandContext, character: FindCharacterQuery.Character) {
            val eb = Embedder(context)
            val nameList = mutableListOf<String>()
            character.name()?.first()?.let { nameList.add(it) }
            character.name()?.last()?.let { nameList.add(it) }
            val fullName = nameList.joinToString(" ")

            eb.setThumbnail(character.image()?.large())
            eb.setTitle(fullName, character.siteUrl())
            eb.setDescription(character.description()?.take(MessageEmbed.TEXT_MAX_LENGTH))

            eb.addField("First name", character.name()?.first() ?: "/", true)

            eb.addField("Last name", character.name()?.last() ?: "/", true)

            eb.addField("Native name", character.name()?.native_() ?: "/", true)

            val otherNames = character.name()?.alternative()
            if (otherNames != null && otherNames.isNotEmpty() && (otherNames.size != 1 && otherNames[0].isBlank())) {
                eb.addField("Alternative names", otherNames.joinToString(), true)
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
                        eb.addField("Anime", split[0], true)
                    else {
                        eb.addField("Anime", split[0], true)
                        eb.addField("Anime..", split[1], true)
                        if (split.size > 2) {
                            eb.addField("Anime...", "rest doesn't fit, go to website", true)
                        }
                    }
                }
                if (mangas.isNotEmpty()) {
                    val split = StringUtils.splitMessage(mangas.joinToString("\n"), 600, MessageEmbed.VALUE_MAX_LENGTH)
                    if (split.size == 1)
                        eb.addField("Manga", split[0], true)
                    else {
                        eb.addField("Manga", split[0], true)
                        eb.addField("Manga..", split[1], true)
                        if (split.size > 2) {
                            eb.addField("Manga...", "rest doesn't fit, go to website", true)
                        }
                    }
                }
            }

            eb.setFooter("Favourites ${character.favourites() ?: 0} 💗")

            sendEmbed(context, eb.build())
        }
    }

    override suspend fun execute(context: CommandContext) {

    }
}