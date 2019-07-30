package me.melijn.melijnbot.commands.utility

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import me.duncte123.weebJava.WeebInfo
import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.sendEmbed
import net.dv8tion.jda.api.JDAInfo

class InfoCommand : AbstractCommand("command.info") {

    init {
        id = 14
        name = "info"
        aliases = arrayOf("bot", "botInfo", "melijn", "version", "versions")
        commandCategory = CommandCategory.UTILITY
    }

    override fun execute(context: CommandContext) {
        val title1 = Translateable("$root.field1.title").string(context)
        val value1 = replaceValueOneVars(Translateable("$root.field1.value").string(context), context)
        val title2 = Translateable("$root.field2.title").string(context)
        val value2 = replaceValueTwoVars(Translateable("$root.field2.value").string(context), context)
        val title3 = Translateable("$root.field3.title").string(context)
        val value3 = replaceValueThreeVars(Translateable("$root.field3.value").string(context), context)



        val eb = Embedder(context)

        eb.setThumbnail(context.getSelfUser().effectiveAvatarUrl)
        eb.addField(title1, value1, false)
        eb.addField(title2, value2, false)
        eb.addField(title3, value3, false)

        sendEmbed(context, eb.build())
    }

    private fun replaceValueThreeVars(string: String, context: CommandContext): String {
        return string
                .replace("%javaVersion%", System.getProperty("java.version"))
                .replace("%kotlinVersion%", "${KotlinVersion.CURRENT.major}.${KotlinVersion.CURRENT.minor}.${KotlinVersion.CURRENT.patch}")
                .replace("%jdaVersion%", JDAInfo.VERSION)
                .replace("%lavaplayerVersion%", PlayerLibrary.VERSION)
                .replace("%weebVersion%", WeebInfo.VERSION)
                .replace("%mysqlVersion%", context.daoManager.mySQLVersion)
                .replace("%mysqlConnectorVersion%", context.daoManager.connectorVersion)

    }

    private fun replaceValueTwoVars(string: String, context: CommandContext): String {
        return string
                .replace("%os%", "${System.getProperty("os.name")} ${System.getProperty("os.arch")} ${System.getProperty("os.version")}")
                .replace("%commandCount%", context.getCommands().size.toString())
    }

    private fun replaceValueOneVars(string: String, context: CommandContext): String {
        return string
                .replace("%ownerTag%", context.jda.shardManager?.getUserById(231459866630291459L)?.asTag ?: "ToxicMushroom#2610")
                .replace("%invite%", "https://discord.gg/E2RfZA9")
                .replace("%botInvite%", "https://melijn.com/invite?perms=true")
                .replace("%website%", "https://melijn.com")
    }
}