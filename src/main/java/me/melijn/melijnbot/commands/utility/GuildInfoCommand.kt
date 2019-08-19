package me.melijn.melijnbot.commands.utility

import me.melijn.melijnbot.objects.command.AbstractCommand
import me.melijn.melijnbot.objects.command.CommandCategory
import me.melijn.melijnbot.objects.command.CommandContext
import me.melijn.melijnbot.objects.command.RunCondition
import me.melijn.melijnbot.objects.embed.Embedder
import me.melijn.melijnbot.objects.translation.Translateable
import me.melijn.melijnbot.objects.utils.asFullLongGMTString
import me.melijn.melijnbot.objects.utils.asTag
import me.melijn.melijnbot.objects.utils.sendEmbed
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

    override fun execute(context: CommandContext) {
        var guild = context.getGuild()
        if (context.args.isNotEmpty()) {
            if (context.args[0].matches(Regex("\\d+"))) {
                guild = context.jda.shardManager?.getGuildById(context.args[0]) ?: guild
            }
        }


        val title1 = Translateable("$root.response1.field1.title").string(context)
        val title2 = Translateable("$root.response1.field2.title").string(context)
        val title3 = Translateable("$root.response1.field3.title").string(context)

        val value1 = replaceFieldVar(Translateable("$root.response1.field1.value").string(context), guild)
        val value2 = replaceFieldVar(Translateable("$root.response1.field2.value").string(context), guild)
        val value31 = replaceFieldVar(Translateable("$root.response1.field3.value.part1").string(context), guild)
        val value32 = replaceFieldVar(Translateable("$root.response1.field3.value.part2").string(context), guild)
        val value33 = replaceFieldVar(Translateable("$root.response1.field3.value.part3").string(context), guild)
        val value34 = replaceFieldVar(Translateable("$root.response1.field3.value.part4").string(context), guild)

        var value3 = ""
        if (guild.iconUrl != null) value3 += replaceFieldVar(value31, guild)
        if (guild.bannerUrl != null) value3 += replaceFieldVar(value32, guild)
        if (guild.splashUrl != null) value3 += replaceFieldVar(value33, guild)
        if (guild.vanityUrl != null) value3 += replaceFieldVar(value34, guild)
        if (value3.isEmpty()) value3 = "/"

        val eb = Embedder(context)
        if (guild.iconUrl != null) eb.setThumbnail(guild.iconUrl)
        eb.addField(title1, value1, false)
        eb.addField(title2, value2, false)
        eb.addField(title3, value3, false)
        sendEmbed(context, eb.build())
    }

    private fun replaceFieldVar(string: String, guild: Guild): String {
        val botCount = guild.memberCache.stream().filter { member -> member.user.isBot }.count()
        return string
                .replace("%guildName%", guild.name)
                .replace("%guildId%", guild.id)
                .replace("%iconUrl%", (if (guild.iconUrl != null) "${guild.iconUrl}?size=2048" else "").toString())
                .replace("%bannerUrl%", (if (guild.bannerUrl != null) "${guild.bannerUrl}?size=2048" else "").toString())
                .replace("%splashUrl%", (if (guild.splashUrl != null) "${guild.splashUrl}?size=2048" else "").toString())
                .replace("%vanityUrl%", (if (guild.vanityUrl != null) "${guild.vanityUrl}?size=2048" else "").toString())
                .replace("%creationDate%", guild.timeCreated.asFullLongGMTString())
                .replace("%region%", guild.region.name)
                .replace("%isVip%", if (guild.region.isVip) "yes" else "no")
                .replace("%boostCount%", guild.boostCount.toString())
                .replace("%boostTier%", guild.boostTier.key.toString())
                .replace("%memberCount%", guild.memberCache.size().toString())
                .replace("%roleCount%", guild.roleCache.size().toString())
                .replace("%textChannelCount%", guild.textChannelCache.size().toString())
                .replace("%voiceChannelCount%", guild.voiceChannelCache.size().toString())
                .replace("%categoryCount%", guild.categoryCache.size().toString())
                .replace("%owner%", (if (guild.owner != null) guild.owner?.asTag else "NONE").toString())
                .replace("%verificationLevel%", guild.verificationLevel.name)
                .replace("%botCount%", botCount.toString())
                .replace("%userCount%", (guild.memberCache.size() - botCount).toString())
                .replace("%botPercent%", (((botCount.toDouble() / guild.memberCache.size()) * 10000).roundToLong() / 100.0).toString())
                .replace("%userPercent%", ((((guild.memberCache.size() - botCount.toDouble()) / guild.memberCache.size()) * 10000).roundToLong() / 100.0).toString())
                .replace("%mfa%", guild.requiredMFALevel.name)
    }
}