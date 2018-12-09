package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static me.melijn.jda.Melijn.PREFIX;

public class GuildInfoCommand extends Command {

    public GuildInfoCommand() {
        this.commandName = "guildInfo";
        this.description = "Shows information about the guild";
        this.usage = PREFIX + commandName + " [guildId]";
        this.aliases = new String[]{"serverInfo"};
        this.extra = "Viewing another guild their info only works if they have Melijn as member";
        this.category = Category.UTILS;
        this.permissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length == 1 && args[0].matches("\\d+") && event.getJDA().asBot().getShardManager().getGuildById(args[0]) != null)
                guild = event.getJDA().asBot().getShardManager().getGuildById(args[0]);
            event.reply(new Embedder(event.getGuild())
                    .setAuthor(guild.getName(), null, guild.getIconUrl() == null ? null : guild.getIconUrl() + "?size=2048")
                    .addField("ID", guild.getId(), true)
                    .addField("Icon", guild.getIconUrl() == null ? "none" : "[Download](" + guild.getIconUrl() + "?size=2048)", true)
                    .addField("Creation date", guild.getCreationTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)), false)
                    .addField("Region", guild.getRegion().getName(), true)
                    .addField("Vip servers", String.valueOf(guild.getRegion().isVip()), true)
                    .addField("Owner", guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator(), true)
                    .addField("Members", String.valueOf(guild.getMemberCache().size()), true)
                    .addField("Roles", String.valueOf(guild.getRoleCache().size()), true)
                    .addBlankField(true)
                    .addField("TextChannels", String.valueOf(guild.getTextChannelCache().size()), true)
                    .addField("VoiceChannels", String.valueOf(guild.getVoiceChannelCache().size()), true)
                    .addField("Categories", String.valueOf(guild.getCategoryCache().size()), true)
                    .setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon())
                    .build());

        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
