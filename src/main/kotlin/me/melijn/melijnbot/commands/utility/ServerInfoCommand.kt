package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.internals.command.AbstractCommand
import me.melijn.melijnbot.internals.command.CommandCategory
import me.melijn.melijnbot.internals.command.ICommandContext
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

    override suspend fun execute(context: ICommandContext) {
        var guild = context.guild
        if (context.args.isNotEmpty()) {
            if (context.args[0].matches(Regex("\\d+"))) {
                guild = context.jda.shardManager?.getGuildById(context.args[0]) ?: guild
            }
        }

        val isSupporter = context.daoManager.supporterWrapper.getGuilds().contains(guild.idLong)
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")

        val response = "$root.response1"
        val title1 = context.getTranslation("$response.field1.title")
        val title2 = context.getTranslation("$response.field2.title")
        val title3 = context.getTranslation("$response.field3.title")

        val value1 = replaceFieldVar1(context, guild, "$response.field1.value", isSupporter, yes, no)
        val value2 = replaceFieldVar2(context, guild, "$response.field2.value")
        val value31 = context.getTranslation("$response.field3.value.part1")
        val value32 = context.getTranslation("$response.field3.value.part2")
        val value33 = context.getTranslation("$response.field3.value.part3")
        val value34 = context.getTranslation("$response.field3.value.part4")

        var value3 = ""
        if (guild.iconUrl != null) value3 += value31.withVariable("iconUrl", sizedUrl(guild.iconUrl))
        if (guild.bannerUrl != null) value3 += value32.withVariable("iconUrl", sizedUrl(guild.bannerUrl))
        if (guild.splashUrl != null) value3 += value33.withVariable("iconUrl", sizedUrl(guild.splashUrl))
        if (guild.vanityUrl != null) value3 += value34.withVariable("iconUrl", sizedUrl(guild.vanityUrl))
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

    private suspend fun replaceFieldVar1(
        context: ICommandContext,
        guild: Guild,
        path: String,
        isSupporter: Boolean,
        yes: String,
        no: String
    ): String {
        return context.getTranslation(path)
            .withSafeVariable("serverName", guild.name)
            .withVariable("serverId", guild.id)
            .withVariable("creationDate", guild.timeCreated.asLongLongGMTString())
            .withVariable("region", guild.region.toUCC())
            .withVariable("isVip", if (guild.region.isVip) yes else no)
            .withVariable("supportsMelijn", if (isSupporter) yes else no)
            .withSafeVariable("owner", guild.retrieveOwner().awaitOrNull()?.asTag ?: "NONE")
            .withVariable("verificationLevel", guild.verificationLevel.toUCC())
            .withVariable("mfa", guild.requiredMFALevel.toUCC())
    }

    private suspend fun replaceFieldVar2(
        context: ICommandContext,
        guild: Guild,
        path: String
    ): String {
        val botCount = guild.memberCache
            .stream()
            .filter { member -> member.user.isBot }
            .count()

        return context.getTranslation(path)
            .withVariable("memberCount", guild.memberCount.toString())
            .withVariable("userCount", (guild.memberCount - botCount).toString())
            .withVariable("botCount", botCount.toString())
            .withVariable("boostCount", guild.boostCount.toString())
            .withVariable("boostTier", guild.boostTier.key.toString())
            .withVariable("roleCount", guild.roleCache.size().toString())
            .withVariable("textChannelCount", guild.textChannelCache.size().toString())
            .withVariable("voiceChannelCount", guild.voiceChannelCache.size().toString())
            .withVariable("categoryCount", guild.categoryCache.size().toString())
            .withVariable(
                "botPercent",
                (((botCount.toDouble() / guild.memberCount) * 10000).roundToLong() / 100.0).toString()
            )
            .withVariable(
                "userPercent",
                ((((guild.memberCount - botCount.toDouble()) / guild.memberCount) * 10000).roundToLong() / 100.0).toString()
            )
    }

    private fun sizedUrl(iconUrl: String?) = iconUrl?.let { "${it}?size=2048" } ?: ""
}