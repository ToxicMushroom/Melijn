package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.awt.*;
import java.time.format.DateTimeFormatter;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class RoleCommand extends Command {

    public RoleCommand() {
        this.commandName = "role";
        this.description = "Shows you info about the chosen role";
        this.usage = PREFIX + commandName + " <role>";
        this.aliases = new String[]{"roleinfo"};
        this.category = Category.UTILS;
    }



    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                Guild guild = event.getGuild();
                if (args.length == 1 && !args[0].equalsIgnoreCase("")) {
                    Role role = Helpers.getRoleByArgs(event, args[0]);
                    if (role != null) {
                        Color roleColor = role.getColor();
                        event.reply(new EmbedBuilder()
                                .setColor(roleColor)
                                .addField("Name", role.getName(), true)
                                .addField("ID", role.getId(), true)
                                .addField("Creation time", role.getCreationTime().format(DateTimeFormatter.ofPattern("dd-MM-yyyy hh:mm:ss a")), false)
                                .addField("Position", (guild.getRoles().size() - role.getPosition() - 1) + "/" + guild.getRoles().size(), true)
                                .addField("Members", String.valueOf(guild.getMembers().stream().filter(member -> member.getRoles().contains(role)).count()), true)
                                .addField("Color", roleColor == null ? "none" : "Hex: **" + String.format("#%02X%02X%02X", roleColor.getRed(), roleColor.getGreen(), roleColor.getBlue()) +
                                        "**\nRGB: (" + roleColor.getRed() + ", " + roleColor.getGreen() + ", " + roleColor.getBlue() + ")", true)
                                .addField("Mentionable", MessageHelper.capFirstChar(String.valueOf(role.isMentionable())), true)
                                .addField("Hoisted", MessageHelper.capFirstChar(String.valueOf(role.isHoisted())), true)
                                .addField("Managed", MessageHelper.capFirstChar(String.valueOf(role.isManaged())), true)
                                .setFooter("Requested by " + event.getFullAuthorName() + " | " + Helpers.getFooterStamp(), event.getAvatarUrl())
                                .build());
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
