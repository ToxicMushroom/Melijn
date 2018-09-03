package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static me.melijn.jda.Melijn.PREFIX;

public class GuildInfoCommand extends Command {

    public GuildInfoCommand() {
        this.commandName = "guildinfo";
        this.description = "Show you information about the guild";
        this.usage = PREFIX + this.commandName + " <guildId>";
        this.aliases = new String[]{"serverinfo"};
        this.extra = "viewing another server's info only works if they have Melijn as member";
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                Guild guild = event.getGuild();
                if (args.length == 1 && args[0].matches("\\d+") && event.getJDA().getGuildById(args[0]) != null) guild = event.getJDA().getGuildById(args[0]);
                event.reply(new EmbedBuilder()
                        .setAuthor(guild.getName(), null, guild.getIconUrl() + "?size=2048")
                        .setColor(Helpers.EmbedColor)
                        .addField("ID", guild.getId(), true)
                        .addField("Icon", "[Download](" + guild.getIconUrl() + "?size=2048)", true)
                        .addField("Creation date", guild.getCreationTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)), false)
                        .addField("Region", guild.getRegion().getName(), true)
                        .addField("Vip servers", String.valueOf(guild.getRegion().isVip()), true)
                        .addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
                        .addField("Members", String.valueOf(guild.getMemberCache().size()), true)
                        .addField("Roles", String.valueOf(guild.getRoles().size()), true)
                        .addBlankField(true)
                        .addField("TextChannels", String.valueOf(guild.getTextChannels().size()), true)
                        .addField("VoiceChannels", String.valueOf(guild.getVoiceChannels().size()), true)
                        .addField("Categories", String.valueOf(guild.getCategories().size()), true)
                        .setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon())
                        .build());

            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        }
    }
}
