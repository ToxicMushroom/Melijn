package me.melijn.melijnbot.commands.administration

import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import me.melijn.melijnbot.database.socialmedia.TwitterWebhook
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.services.twitter.TweetInfo
import me.melijn.melijnbot.internals.translation.MESSAGE_UNKNOWN_TWEETTYPE
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.StringUtils.URL_PATTERN
import me.melijn.melijnbot.internals.utils.message.*
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent

class TwitterCommand : AbstractCommand("command.twitter") {

    init {
        id = 244
        name = "twitter"
        children = arrayOf(
            SetEnabledArg(root),
            SetupWebhookArg(root),
            ExcludedTweetTypes(root),
            ListArg(root),
            RemoveAtArg(root)
        )
        commandCategory = CommandCategory.ADMINISTRATION
    }

    class SetEnabledArg(parent: String) : AbstractCommand("$parent.setenabled") {

        init {
            name = "setEnabled"
            cooldown = 10_000
            aliases = arrayOf("se")
        }

        override suspend fun execute(context: ICommandContext) {
            val webhook = getTwitterWebhookByArgsNMessage(context, 0) ?: return
            val state = getBooleanFromArgNMessage(context, 1) ?: return
            webhook.enabled = state
            context.daoManager.twitterWrapper.store(webhook)

            sendRsp(context, "Set the webhook state for **%t%** to **%e%**"
                .withSafeVariable("t", webhook.handle)
                .withSafeVariable("e", webhook.enabled)
            )
        }
    }

    companion object {
        private const val TWITTER_LIMIT = 1
        private const val PREMIUM_TWITTER_LIMIT = 5
        private const val TWITTER_LIMIT_PATH = "premium.feature.twitter.limit"

        suspend fun getTwitterWebhookByArgsNMessage(context: ICommandContext, index: Int): TwitterWebhook? {
            val list = context.daoManager.twitterWrapper.getAll(context.guildId).sortedBy { it.handle }
            if (list.isEmpty()) {
                sendRsp(context, "You don't track any twitter users")
                return null
            }
            val id = getIntegerFromArgNMessage(context, index, 1, list.size) ?: return null
            return list[id - 1]
        }
    }

    class ExcludedTweetTypes(parent: String) : AbstractCommand("$parent.excludedtweettypes") {

        init {
            name = "excludedTweetTypes"
            aliases = arrayOf("ett")
            children = arrayOf(
                AddArg(root),
                RemoveArg(root),
                ListArg(root)
            )
        }

        class RemoveArg(parent: String) : AbstractCommand("$parent.remove") {

            init {
                name = "remove"
                aliases = arrayOf("rm")
            }

            override suspend fun execute(context: ICommandContext) {
                val twitterWebhook = getTwitterWebhookByArgsNMessage(context, 0) ?: return
                val toInclude =
                    getEnumFromArgNMessage<TweetInfo.TweetType>(context, 1, MESSAGE_UNKNOWN_TWEETTYPE) ?: return

                twitterWebhook.excludedTweetTypes = twitterWebhook.excludedTweetTypes - toInclude
                context.daoManager.twitterWrapper.store(twitterWebhook)

                sendRsp(
                    context, "Removed `%tweetType%` from `%handle%`'s ignored tweetTypes"
                        .withSafeVarInCodeblock("tweetType", toInclude.toUCC())
                        .withSafeVarInCodeblock("handle", twitterWebhook.handle)
                )
            }
        }

        class AddArg(parent: String) : AbstractCommand("$parent.add") {

            init {
                name = "add"
                aliases = arrayOf("a")
            }

            override suspend fun execute(context: ICommandContext) {
                val twitterWebhook = getTwitterWebhookByArgsNMessage(context, 0) ?: return
                val toExclude =
                    getEnumFromArgNMessage<TweetInfo.TweetType>(context, 1, MESSAGE_UNKNOWN_TWEETTYPE) ?: return

                twitterWebhook.excludedTweetTypes = twitterWebhook.excludedTweetTypes + toExclude
                context.daoManager.twitterWrapper.store(twitterWebhook)

                sendRsp(
                    context, "Added `%tweetType%` to `%handle%`'s ignored tweetTypes"
                        .withSafeVarInCodeblock("tweetType", toExclude.toUCC())
                        .withSafeVarInCodeblock("handle", twitterWebhook.handle)
                )
            }

        }

        class ListArg(parent: String) : AbstractCommand("$parent.list") {

            init {
                name = "list"
                aliases = arrayOf("ls")
            }

            override suspend fun execute(context: ICommandContext) {
                val twitterWebhook = getTwitterWebhookByArgsNMessage(context, 0) ?: return

                val title = "List of `%handle%`'s ignored tweetTypes:"
                    .withSafeVarInCodeblock("handle", twitterWebhook.handle)
                var body = "```INI"
                for (type in twitterWebhook.excludedTweetTypes) {
                    body += "\n - ${type.toUCC()}"
                }
                body += "```"
                sendRsp(context, title + body)
            }

        }

        override suspend fun execute(context: ICommandContext) {
            sendSyntax(context)
        }
    }

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        override suspend fun execute(context: ICommandContext) {
            val twitterWrapper = context.daoManager.twitterWrapper
            val toRemove = getTwitterWebhookByArgsNMessage(context, 0) ?: return
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

            var msg = "Tracked Twitter Users```INI\n[index] - [@user] - [twitterId] - [enabled]\n"
            for ((index, webHook) in list.withIndex().sortedBy { it.value.handle }) {
                msg += "${index + 1}. - [${webHook.handle}] - ${webHook.twitterUserId} - ${webHook.enabled}\n"
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
            } else if (twitters.size >= PREMIUM_TWITTER_LIMIT && context.guildId != 950974820827398235) { // hardcoded exception for a particul server
                val msg = context.getTranslation("$root.limit.total")
                    .withVariable("limit", "$PREMIUM_TWITTER_LIMIT")
                sendRsp(context, msg)
                return
            }
            // TODO: this is shit duplicate code, not extensible, needs to be fixed ^

            val eb = Embedder(context)
                .setDescription("Whose twitter feed do you wanna track? Example: `@PixelHamster`")
            sendEmbedRspAwaitEL(context, eb.build())

            context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
                it.channel.idLong == context.channelId && it.author.idLong == context.authorId
            }, {
                var response = it.message.contentRaw
                if (!response.startsWith("@")) response = "@$response"
                val res = context.webManager.proxiedHttpClient.post<String>("https://tweeterid.com/ajax.php") {
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
                }, fish@{ event2 ->
                    val response2 = event2.message.contentRaw
                    if (!response2.matches(URL_PATTERN)) {
                        sendRsp(
                            context,
                            "That is not a valid [webhook](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks) url. You can start over and provide a valid one."
                        )
                        return@fish
                    }

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