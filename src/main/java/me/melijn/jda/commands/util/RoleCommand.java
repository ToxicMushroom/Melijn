package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import static me.melijn.jda.Melijn.PREFIX;

public class RoleCommand extends Command {

    public RoleCommand() {
        this.commandName = "role";
        this.description = "Shows information about the chosen role";
        this.usage = PREFIX + commandName + " <role>";
        this.aliases = new String[]{"roleinfo"};
        this.category = Category.UTILS;
        this.needs = new Need[]{Need.GUILD};
        this.id = 76;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length == 1 && !args[0].isBlank()) {
                Role role = Helpers.getRoleByArgs(event, args[0]);
                if (role != null) {
                    Color roleColor = role.getColor();
                    event.reply(new EmbedBuilder()
                            .setColor(roleColor)
                            .addField("Name", role.getName(), true)
                            .addField("ID", role.getId(), true)
                            .addField("Creation time", role.getCreationTime().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.SHORT)), false)
                            .addField("Position", (guild.getRoleCache().size() - role.getPosition() - 1) + "/" + guild.getRoleCache().size(), true)
                            .addField("Members", String.valueOf(guild.getMemberCache().stream().filter(member -> member.getRoles().contains(role)).count()), true)
                            .addField("Color", roleColor == null ? "none" : "Hex: **" + String.format("#%02X%02X%02X", roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue()) +
                                    "**\nRGB: (" + roleColor.getRed() + ", " + roleColor.getGreen() + ", " + roleColor.getBlue() + ")", true)
                            .addField("Mentionable", MessageHelper.capFirstChar(String.valueOf(role.isMentionable())), true)
                            .addField("Hoisted", MessageHelper.capFirstChar(String.valueOf(role.isHoisted())), true)
                            .addField("Managed", MessageHelper.capFirstChar(String.valueOf(role.isManaged())), true)
                            .setFooter(Helpers.getFooterStamp(), event.getAvatarUrl())
                            .build());
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
