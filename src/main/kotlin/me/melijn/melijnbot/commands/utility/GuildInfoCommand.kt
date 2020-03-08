package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.utils.asLongLongGMTString
import me.melijn.melijnbot.objects.utils.asTag
import me.melijn.melijnbot.objects.utils.sendEmbed
import me.melijn.melijnbot.objects.utils.toUCC
import net.dv8tion.jda.api.entities.Guild
import kotlin.math.roundToLong

class GuildInfoCommand : AbstractCommand("command.guildinfo") {

    init {
        id = 7
        name = "guildInfo"
        aliases = arrayOf("guild", "server", "serverInfo")
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
        if (guild.iconUrl != null) {
            eb.setThumbnail(guild.iconUrl)
        }
        eb.addField(title1, value1, false)
        eb.addField(title2, value2, false)
        eb.addField(title3, value3, false)
        sendEmbed(context, eb.build())
    }

    private suspend fun replaceFieldVar(context: CommandContext, guild: Guild, path: String): String {
        val isSupporter = context.daoManager.supporterWrapper.guildSupporterIds.contains(guild.idLong)
        val yes = context.getTranslation("yes")
        val no = context.getTranslation("no")
        return replaceFieldVar(context.getTranslation(path), guild, isSupporter, yes, no)
    }

    private fun replaceFieldVar(string: String, guild: Guild, isSupporter: Boolean, yes: String, no: String): String {
        val botCount = guild.memberCache
            .stream()
            .filter { member -> member.user.isBot }
            .count()

        return string
            .replace("%guildName%", guild.name)
            .replace("%guildId%", guild.id)
            .replace("%iconUrl%", (if (guild.iconUrl != null) "${guild.iconUrl}?size=2048" else "").toString())
            .replace("%bannerUrl%", (if (guild.bannerUrl != null) "${guild.bannerUrl}?size=2048" else "").toString())
            .replace("%splashUrl%", (if (guild.splashUrl != null) "${guild.splashUrl}?size=2048" else "").toString())
            .replace("%vanityUrl%", (if (guild.vanityUrl != null) "${guild.vanityUrl}?size=2048" else "").toString())
            .replace("%creationDate%", guild.timeCreated.asLongLongGMTString())
            .replace("%region%", guild.region.toUCC())
            .replace("%isVip%", if (guild.region.isVip) yes else no)
            .replace("%supportsMelijn%", if (isSupporter) yes else no)
            .replace("%boostCount%", guild.boostCount.toString())
            .replace("%boostTier%", guild.boostTier.key.toString())
            .replace("%memberCount%", guild.memberCache.size().toString())
            .replace("%roleCount%", guild.roleCache.size().toString())
            .replace("%textChannelCount%", guild.textChannelCache.size().toString())
            .replace("%voiceChannelCount%", guild.voiceChannelCache.size().toString())
            .replace("%categoryCount%", guild.categoryCache.size().toString())
            .replace("%owner%", (if (guild.owner != null) guild.owner?.asTag else "NONE").toString())
            .replace("%verificationLevel%", guild.verificationLevel.toUCC())
            .replace("%botCount%", botCount.toString())
            .replace("%userCount%", (guild.memberCache.size() - botCount).toString())
            .replace("%botPercent%", (((botCount.toDouble() / guild.memberCache.size()) * 10000).roundToLong() / 100.0).toString())
            .replace("%userPercent%", ((((guild.memberCache.size() - botCount.toDouble()) / guild.memberCache.size()) * 10000).roundToLong() / 100.0).toString())
            .replace("%mfa%", guild.requiredMFALevel.toUCC())
    }
}