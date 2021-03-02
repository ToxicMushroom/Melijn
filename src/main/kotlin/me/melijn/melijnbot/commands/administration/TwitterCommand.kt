package me.melijn.melijnbot.commands.administration

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import me.melijn.melijnbot.database.socialmedia.TwitterWebhook
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.getIntegerFromArgNMessage
import me.melijn.melijnbot.internals.utils.isPositiveNumber
import me.melijn.melijnbot.internals.utils.isPremiumUser
import me.melijn.melijnbot.internals.utils.message.*
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class TwitterCommand : AbstractCommand("command.twitter") {

    init {
        id = 244
        name = "twitter"
        children = arrayOf(
            SetupWebhookArg(root),
            ListArg(root),
            RemoveAtArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    companion object {
        private const val TWITTER_LIMIT = 1
        private const val PREMIUM_TWITTER_LIMIT = 5
        private const val TWITTER_LIMIT_PATH = "premium.feature.twitter.limit"
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        override suspend fun execute(context: ICommandContext) {
            val twitterWrapper = context.daoManager.twitterWrapper
            val list = twitterWrapper.getAll(context.guildId)
            if (list.isEmpty()) {
                sendRsp(context, "You don't track any twitter users")
                return
            }

            val index = (getIntegerFromArgNMessage(context, 0, 1, list.size) ?: return) - 1
            val toRemove = list[index]
            twitterWrapper.delete(context.guildId, toRemove.handle)

            val msg = "Removed tracking `" + toRemove.handle + "`"
            sendRsp(context, msg)
        }
    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            val list = context.daoManager.twitterWrapper.getAll(context.guildId)
            if (list.isEmpty()) {
                sendRsp(context, "You don't track any twitter users")
                return
            }

            var msg = "Tracked Twitter Users```INI\n[index] - [@user] - [twitterId]\n"
            for ((index, webHook) in list.withIndex().sortedBy { it.value.handle }) {
                msg += "${index + 1}. - [" + webHook.handle + "] - " + webHook.twitterUserId + "\n"
            }
            msg += "```"
            sendRsp(context, msg)
        }
    }

    class SetupWebhookArg(parent: String) : AbstractCommand("$parent.setupwebhook") {

        init {
            name = "setupWebhook"
            aliases = arrayOf("sw")
        }

        override suspend fun execute(context: ICommandContext) {
            val twitters = context.daoManager.twitterWrapper.getAll(context.guildId)
            if (twitters.size > TWITTER_LIMIT && !isPremiumUser(context)) {
                val replaceMap = mapOf(
                    "limit" to "$TWITTER_LIMIT",
                    "premiumLimit" to "$PREMIUM_TWITTER_LIMIT"
                )

                sendFeatureRequiresGuildPremiumMessage(context, TWITTER_LIMIT_PATH, replaceMap)
                return
            } else if (twitters.size >= PREMIUM_TWITTER_LIMIT) {
                val msg = context.getTranslation("$root.limit.total")
                    .withVariable("limit", "$PREMIUM_TWITTER_LIMIT")
                sendRsp(context, msg)
                return
            }

            val eb = Embedder(context)
                .setDescription("Whose twitter feed do you wanna track? Example: `@PixelHamster`")
            sendEmbedRspAwaitEL(context, eb.build())

            context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
                it.channel.idLong == context.channelId && it.author.idLong == context.authorId
            }, {
                var response = it.message.contentRaw
                if (!response.startsWith("@")) response = "@$response"
                val res = context.webManager.httpClient.post<String>("https://tweeterid.com/ajax.php") {
                    body = FormDataContent(Parameters.build {
                        append("input", response)
                    })
                }
                if (!res.isPositiveNumber()) {
                    sendRsp(context, "Couldn't find a user for your input, check for spelling.")
                    return@waitFor
                }

                eb.setDescription(
                    "Okay, I'm now tracking `$response` for new tweets~\n" +
                        "What discord [webhook](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks)" +
                        " should I send the tweets to?"
                )
                sendEmbedRspAwaitEL(context, eb.build())

                context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, { event2 ->
                    event2.channel.idLong == context.channelId && event2.author.idLong == context.authorId
                }, { event2 ->
                    val response2 = event2.message.contentRaw

                    eb.setDescription(
                        "Okay, I'm now tracking $response for new tweets and will resend them to your webhook."
                    )

                    context.daoManager.twitterWrapper.store(
                        TwitterWebhook(
                            context.guildId,
                            response2,
                            emptySet(),
                            response,
                            res.toLong(),
                            0,
                            0,
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            true
                        )
                    )

                    sendEmbedRsp(context, eb.build())
                })
            })
        }
    }

    override suspend fun execute(context: ICommandContext) {
        sendSyntax(context)
    }
}