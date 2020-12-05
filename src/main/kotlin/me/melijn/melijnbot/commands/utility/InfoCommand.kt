package me.melijn.melijnbot.commands.utility

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import me.duncte123.weebJava.WeebInfo
import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.awaitOrNull
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import me.melijn.melijnbot.internals.utils.withVariable
import net.dv8tion.jda.api.JDAInfo

class InfoCommand : AbstractCommand("command.info") {

    init {
        id = 14
        name = "info"
        aliases = arrayOf("bot", "botInfo", "melijn", "version", "versions")
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        val title1 = context.getTranslation("$root.field1.title")
        val value1 = replaceValueOneVars(context.getTranslation("$root.field1.value"), context)
        val title2 = context.getTranslation("$root.field2.title")
        val value2 = replaceValueTwoVars(context.getTranslation("$root.field2.value"), context)
        val title3 = context.getTranslation("$root.field3.title")
        val value3 = replaceValueThreeVars(context.getTranslation("$root.field3.value"), context)


        val eb = Embedder(context)
            .setThumbnail(context.selfUser.effectiveAvatarUrl)
            .addField(title1, value1, false)
            .addField(title2, value2, false)
            .addField(title3, value3, false)

        sendEmbedRsp(context, eb.build())
    }

    private fun replaceValueThreeVars(string: String, context: CommandContext): String = string
        .withVariable("javaVersion", System.getProperty("java.version"))
        .withVariable(
            "kotlinVersion",
            "${KotlinVersion.CURRENT.major}.${KotlinVersion.CURRENT.minor}.${KotlinVersion.CURRENT.patch}"
        )
        .withVariable("jdaVersion", JDAInfo.VERSION)
        .withVariable("lavaplayerVersion", PlayerLibrary.VERSION)
        .withVariable("weebVersion", WeebInfo.VERSION)
        .withVariable("dbVersion", context.daoManager.dbVersion)
        .withVariable("dbConnectorVersion", context.daoManager.connectorVersion)

    private fun replaceValueTwoVars(string: String, context: CommandContext): String = string
        .withVariable(
            "os",
            "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}"
        )
        .withVariable("commandCount", context.commandList.size.toString())

    private suspend fun replaceValueOneVars(string: String, context: CommandContext): String = string
        .withVariable(
            "ownerTag", context.jda.shardManager?.retrieveUserById(231459866630291459L)?.awaitOrNull()?.asTag
                ?: "ToxicMushroom#2610"
        )
        .withVariable("invite", "https://discord.gg/tfQ9s7u")
        .withVariable("botInvite", "https://melijn.com/invite")
        .withVariable("website", "https://melijn.com")
        .withVariable("contact", "merlijn@melijn.me")

}