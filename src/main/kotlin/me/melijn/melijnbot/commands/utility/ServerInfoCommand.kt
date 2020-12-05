package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.CommandContext
import me.melijn.melijnbot.internals.command.RunCondition
import me.melijn.melijnbot.internals.embed.Embedder
import me.melijn.melijnbot.internals.utils.*
import me.melijn.melijnbot.internals.utils.message.sendEmbedRsp
import net.dv8tion.jda.api.entities.Guild
import kotlin.math.roundToLong

class ServerInfoCommand : AbstractCommand("command.serverinfo") {

    init {
        id = 7
        name = "serverInfo"
        aliases = arrayOf("guild", "server", "guildInfo")
        runConditions = arrayOf(RunCondition.GUILD)
        commandCategory = CommandCategory.UTILITY
    }

    override suspend fun execute(context: CommandContext) {
        var guild = context.guild
        if (context.args.isNotEmpty()) {
            if (context.args[0].matches(Regex("\\d+"))) {
                guild = context.jda.shardManager?.getGuildById(context.args[0]) ?: guild
            }
        }


        val title1 = context.getTranslation("$root.response1.field1.title")
        val title2 = context.getTranslation("$root.response1.field2.title")
        val title3 = context.getTranslation("$root.response1.field3.title")

        val value1 = replaceFieldVar(context, guild, "$root.response1.field1.value")
        val value2 = replaceFieldVar(context, guild, "$root.response1.field2.value")
        val value31 = replaceFieldVar(context, guild, "$root.response1.field3.value.part1")
        val value32 = replaceFieldVar(context, guild, "$root.response1.field3.value.part2")
        val value33 = replaceFieldVar(context, guild, "$root.response1.field3.value.part3")
        val value34 = replaceFieldVar(context, guild, "$root.response1.field3.value.part4")

        var value3 = ""
        if (guild.iconUrl != null) value3 += replaceFieldVar(context, guild, value31)
        if (guild.bannerUrl != null) value3 += replaceFieldVar(context, guild, value32)
        if (guild.splashUrl != null) value3 += replaceFieldVar(context, guild, value33)
        if (guild.vanityUrl != null) value3 += replaceFieldVar(context, guild, value34)
        if (value3.isEmpty()) value3 = "/"

        val eb = Embedder(context)
            .setThumbnail(guild.iconUrl)
            .setDescription(
                """
                |```INI
                |[${title1}]```$value1
                |
                |```INI
                |[${title2}]```$value2
                |                                
                |```INI
                |[${title3}]```$value3
            """.trimMargin()
            )

        sendEmbedRsp(context, eb.build())
    }

    private suspend fun replaceFieldVar(context: CommandContext, guild: Guild, path: String): String {
        val isSupporter = context.daoManager.supporterWrapper.getGuilds().contains(guild.idLong)
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")
        return replaceFieldVar(context.getTranslation(path), guild, isSupporter, yes, no)
    }

    private suspend fun replaceFieldVar(
        string: String,
        guild: Guild,
        isSupporter: Boolean,
        yes: String,
        no: String
    ): String {
        val botCount = guild.memberCache
            .stream()
            .filter { member -> member.user.isBot }
            .count()

        return string
            .withVariable("serverName", guild.name)
            .withVariable("serverId", guild.id)
            .withVariable("iconUrl", (if (guild.iconUrl != null) "${guild.iconUrl}?size=2048" else "").toString())
            .withVariable("bannerUrl", (if (guild.bannerUrl != null) "${guild.bannerUrl}?size=2048" else "").toString())
            .withVariable("splashUrl", (if (guild.splashUrl != null) "${guild.splashUrl}?size=2048" else "").toString())
            .withVariable("vanityUrl", (if (guild.vanityUrl != null) "${guild.vanityUrl}?size=2048" else "").toString())
            .withVariable("creationDate", guild.timeCreated.asLongLongGMTString())
            .withVariable("region", guild.region.toUCC())
            .withVariable("isVip", if (guild.region.isVip) yes else no)
            .withVariable("supportsMelijn", if (isSupporter) yes else no)
            .withVariable("boostCount", guild.boostCount.toString())
            .withVariable("boostTier", guild.boostTier.key.toString())
            .withVariable("memberCount", guild.memberCount.toString())
            .withVariable("roleCount", guild.roleCache.size().toString())
            .withVariable("textChannelCount", guild.textChannelCache.size().toString())
            .withVariable("voiceChannelCount", guild.voiceChannelCache.size().toString())
            .withVariable("categoryCount", guild.categoryCache.size().toString())
            .withVariable("owner", guild.retrieveOwner().awaitOrNull()?.asTag ?: "NONE")
            .withVariable("verificationLevel", guild.verificationLevel.toUCC())
            .withVariable("botCount", botCount.toString())
            .withVariable("userCount", (guild.memberCount - botCount).toString())
            .withVariable(
                "botPercent",
                (((botCount.toDouble() / guild.memberCount) * 10000).roundToLong() / 100.0).toString()
            )
            .withVariable(
                "userPercent",
                ((((guild.memberCount - botCount.toDouble()) / guild.memberCount) * 10000).roundToLong() / 100.0).toString()
            )
            .withVariable("mfa", guild.requiredMFALevel.toUCC())
    }
}