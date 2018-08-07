package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static me.melijn.jda.Melijn.PREFIX;

public class GuildInfoCommand extends Command {

    public GuildInfoCommand() {
        this.commandName = "guildinfo";
        this.description = "Show you information about the guild";
        this.usage = PREFIX + this.commandName;
        this.aliases = new String[]{"serverinfo"};
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                JDA jda = event.getJDA();
                Guild guild = event.getGuild();
                if (args.length == 1 && !args[0].equalsIgnoreCase("") && jda.getGuildById(args[0]) != null)
                    guild = jda.getGuildById(args[0]);
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle(guild.getName());
                eb.setThumbnail(guild.getIconUrl());
                eb.setColor(Helpers.EmbedColor);
                eb.addField("Icon", "[Download](" + guild.getIconUrl() + ")", false);
                eb.addField("Creation date", guild.getCreationTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)), false);
                eb.addField("Region", guild.getRegion().getName(), true);
                eb.addField("Vip servers", String.valueOf(guild.getRegion().isVip()), true);
                eb.addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), false);
                eb.addField("Members", String.valueOf(guild.getMembers().size()), true);
                if (guild.getSelfMember().hasPermission(Permission.BAN_MEMBERS)) eb.addField("Bans", String.valueOf(guild.getBanList().complete().size()), true);
                eb.addField("Roles", String.valueOf(guild.getRoles().size()), true);
                eb.addField("TextChannels", String.valueOf(guild.getTextChannels().size()), true);
                eb.addField("VoiceChannels", String.valueOf(guild.getVoiceChannels().size()), true);
                eb.addField("Categories", String.valueOf(guild.getCategories().size()), true);
                eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                event.reply(eb.build());
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        }
    }
}
