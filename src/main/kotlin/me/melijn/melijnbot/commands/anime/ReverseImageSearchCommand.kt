package me.melijn.melijnbot.commands.anime

import io.ktor.client.request.*
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getImageUrlFromArgsNMessage
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendRsp
import net.dv8tion.jda.api.utils.data.DataObject

class ReverseImageSearchCommand : AbstractCommand("command.reverseimagesearch") {

    init {
        id = 213
        name = "reverseImageSearch"
        aliases = arrayOf("ris", "saucenao")
        runConditions = arrayOf(RunCondition.USER_SUPPORTER, RunCondition.CHANNEL_NSFW)
        commandCategory = CommandCategory.ANIME
    }

    override suspend fun execute(context: ICommandContext) {
        val attachment = (getImageUrlFromArgsNMessage(context, 0) ?: return).second
        val query = "https://saucenao.com/search.php" +
            "?db=999" +
            "&api_key=${context.container.settings.api.sauceNao}" +
            "&output_type=2" +
            "&numres=5" +
            "&url=$attachment"

        val json = DataObject.fromJson(context.webManager.httpClient.get<String>(query))
        val results = json.getArray("results")
        if (results.length() == 0) {
            sendRsp(context, "No matches found :c")
            return
        }

        val sauceResultInfo = SauceResultInfo(results.getObject(0).getObject("header").getString("similarity"))

        for (i in 0 until results.length()) {
            val result = results.getObject(i)
            val data = result.getObject("data")
            val headers = result.getObject("header")

            if (headers.getString("similarity").toFloat() < 75.0 && i > 2) {
                continue
            }

            headers.getString("thumbnail", null)?.let {
                if (sauceResultInfo.imageUrl == null)
                    sauceResultInfo.imageUrl = it
            }

            data.getString("member_name", null)?.let {
                if (sauceResultInfo.creator == null)
                    sauceResultInfo.creator = it
            }

            data.getLong("member_id", -1).takeIf { it > -1 }?.let {
                if (sauceResultInfo.pixivUserId == null)
                    sauceResultInfo.pixivUserId = it
            }

            data.getLong("pixiv_id", -1).takeIf { it > -1 }?.let {
                if (sauceResultInfo.pixivPostId == null)
                    sauceResultInfo.pixivPostId = it
            }

            data.getLong("danbooru_id", -1).takeIf { it > -1 }?.let {
                if (sauceResultInfo.danbooruPostId == null)
                    sauceResultInfo.danbooruPostId = it
            }

            data.getLong("gelbooru_id", -1).takeIf { it > -1 }?.let {
                if (sauceResultInfo.gelbooruPostId == null)
                    sauceResultInfo.gelbooruPostId = it
            }

            data.getString("title", null)?.let {
                if (sauceResultInfo.title == null)
                    sauceResultInfo.title = it
            }

            data.getString("characters", null)?.let {
                if (sauceResultInfo.character == null)
                    sauceResultInfo.character = it
            }

            data.getString("material", null)?.let {
                if (sauceResultInfo.material == null)
                    sauceResultInfo.material = it
            }
        }

        val sb = StringBuilder()
        sauceResultInfo.title?.let {
            sb.append("**Title** ").appendLine(it)
        }
        sauceResultInfo.pixivUserId?.let {
            sb.appendLine("**Pixiv User** https://www.pixiv.net/en/users/${it}")
        }

        sauceResultInfo.pixivPostId?.let {
            sb.appendLine("**Pixiv Post** https://www.pixiv.net/en/artworks/${it}")
        }

        sauceResultInfo.danbooruPostId?.let {
            sb.appendLine("**Danbooru Post** https://danbooru.donmai.us/posts/${it}")
        }

        sauceResultInfo.gelbooruPostId?.let {
            sb.appendLine("**Gelbooru Post** https://gelbooru.com/index.php?page=post&s=view&id=${it}")
        }

        sb.appendLine()

        sauceResultInfo.creator?.let {
            sb.append("**Creator** ").appendLine(it)
        }

        sauceResultInfo.material?.let {
            sb.append("**Material** ").appendLine(it)
        }

        sauceResultInfo.character?.let {
            sb.append("**Character** ").appendLine(it)
        }

        val eb = Embedder(context)
            .setTitle("SauceNao Match")
            .setDescription(sb.toString())
            .setImage(attachment)
            .build()

        sendEmbedRsp(context, eb)
    }
}

data class SauceResultInfo(
    var matchPercentage: String,
    var imageUrl: String? = null,
    var creator: String? = null,
    var title: String? = null,
    var character: String? = null,
    var material: String? = null,
    var pixivUserId: Long? = null,
    var pixivPostId: Long? = null,
    var danbooruPostId: Long? = null,
    var gelbooruPostId: Long? = null
)