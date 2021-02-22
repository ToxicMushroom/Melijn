package me.melijn.melijnbot.commands.administration

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.message.sendSyntax
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

    class RemoveAtArg(parent: String) : AbstractCommand("$parent.removeat") {

        init {
            name = "removeAt"
            aliases = arrayOf("rma")
        }

        override suspend fun execute(context: ICommandContext) {
            TODO("Not yet implemented")
        }

    }

    class ListArg(parent: String) : AbstractCommand("$parent.list") {

        init {
            name = "list"
            aliases = arrayOf("ls")
        }

        override suspend fun execute(context: ICommandContext) {
            TODO("Not yet implemented")
        }

    }

    class SetupWebhookArg(parent: String) : AbstractCommand("$parent.setupwebhook") {

        init {
            name = "setupWebhook"
            aliases = arrayOf("sw")
        }

        override suspend fun execute(context: ICommandContext) {
            if (context.args.isEmpty()) {
                sendSyntax(context)
                return
            }

            val eb = Embedder(context)
                .setDescription("Whose twitter feed do you wanna track? Example: `@PixelHamster`")
            sendEmbedRsp(context, eb.build())

            context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, {
                it.channel.idLong == context.channelId && it.author.idLong == context.authorId
            }, {
                val response = it.message.contentRaw

                eb.setDescription(
                    "Okay, I'm now tracking $response for new tweets~\n" +
                        "What discord [webhook](https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks)" +
                        " should I send the tweets to?"
                )
                sendEmbedRsp(context, eb.build())

                context.container.eventWaiter.waitFor(GuildMessageReceivedEvent::class.java, { event2 ->
                    event2.channel.idLong == context.channelId && event2.author.idLong == context.authorId
                }, { event2 ->
                    val response2 = event2.message.contentRaw

                    eb.setDescription(
                        "Okay, I'm now tracking $response for new tweets and will resend them to $response2"
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