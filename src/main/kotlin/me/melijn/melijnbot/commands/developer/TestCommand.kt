package me.melijn.melijnbot.commands.developer

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import org.slf4j.Logger
import org.slf4j.LoggerFactory


class TestCommand : AbstractCommand("command.test") {

    init {
        id = 20
        name = "test"
        commandCategory = CommandCategory.DEVELOPER
    }

    val logger: Logger = LoggerFactory.getLogger(TestCommand::class.java)

    override suspend fun execute(context: CommandContext) {
        val eb = Embedder(context)
            .setDescription("**Bot Stats**\n" +
                "Shards: `1`\n" +
                "Cached Users: `2`\n" +
                "Servers: `6`\n" +
                "Listening/VoiceChannels: `0/0`\n" +
                "Threads: `0`\n" +
                "Uptime: `00:24`\n" +
                "Queued Tracks: `0`\n" +
                "Music Players: `0`\n" +
                "\n" +
                "**Server Stats**\n" +
                "Cores: `4`\n" +
                "RAM Usage: `11653MB/15312MB`\n" +
                "System Uptime: `4d 02:49:42`\n" +
                "\n" +
                "**JVM Stats**\n" +
                "CPU Usage: `33.918%`\n" +
                "Ram Usage: `34MB/512MB`\n" +
                "Threads: `52/57`")
            .setThumbnail(context.selfMember.user.effectiveAvatarUrl)

        sendEmbedRsp(context, eb.build())
    }
}