package me.melijn.melijnbot.commands.anime

import me.melijn.melijnbot.anilist.type.MediaType
import me.melijn.melijnbot.commandutil.anime.AniListCommandUtil
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.utils.message.sendSyntax

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
            SearchCharacterArg(root),
            UserArg(root),
            SearchUserArg(root)
        )
        commandCategory = CommandCategory.ANIME
    }

    class SearchUserArg(parent: String) : AbstractCommand("$parent.searchuser") {

        init {
            name = "searchUser"
            aliases = arrayOf("su")
        }

        suspend fun execute(context: ICommandContext) {
            AniListCommandUtil.searchUser(context)
        }
    }

    class MangaArg(parent: String) : AbstractCommand("$parent.manga") {

        init {
            name = "manga"
            aliases = arrayOf("m")
        }

        suspend fun execute(context: ICommandContext) {
            AniListCommandUtil.searchManga(context)
        }
    }

    class SearchMangaArg(parent: String) : AbstractCommand("$parent.searchmanga") {

        init {
            name = "searchManga"
            aliases = arrayOf("sm")
        }

        suspend fun execute(context: ICommandContext) {
            AniListCommandUtil.searchMedia(context, MediaType.MANGA)
        }
    }

    class UserArg(parent: String) : AbstractCommand("$parent.user") {

        init {
            name = "user"
            aliases = arrayOf("u", "profile", "userProfile", "up")
        }

        suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val userName = context.rawArg.take(256)

            AniListCommandUtil.getUserByName(context, userName)
        }
    }

    class AnimeArg(parent: String) : AbstractCommand("$parent.anime") {

        init {
            name = "anime"
            aliases = arrayOf("a", "series", "movie", "ova", "ona", "tv")
        }

        suspend fun execute(context: ICommandContext) {
            AniListCommandUtil.searchAnime(context)
        }
    }

    class SearchAnimeArg(parent: String) : AbstractCommand("$parent.searchanime") {

        init {
            name = "searchAnime"
            aliases = arrayOf("sa", "searchmovie", "searchova", "searchona")
        }

        suspend fun execute(context: ICommandContext) {
            AniListCommandUtil.searchMedia(context, MediaType.ANIME)
        }
    }

    class CharacterArg(parent: String) : AbstractCommand("$parent.character") {

        init {
            name = "character"
            aliases = arrayOf("c", "char")
        }

        suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val characterName = context.rawArg.take(256)

            AniListCommandUtil.getCharacterByName(context, characterName)
        }
    }

    class SearchCharacterArg(parent: String) : AbstractCommand("$parent.searchcharacter") {

        init {
            name = "searchCharacter"
            aliases = arrayOf("sc")
        }

        suspend fun execute(context: ICommandContext) {
            AniListCommandUtil.searchCharacter(context)
        }
    }

    suspend fun execute(context: ICommandContext) {
        if (context.args.isEmpty()) {
            sendSyntax(context)
            return
        }

        AniListCommandUtil.searchAnime(context)
    }
}