package com.pixelatedsource.jda.commands.util;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class RoleInfoCommand extends Command {

    public RoleInfoCommand() {
        this.commandName = "roleinfo";
        this.description = "Shows you a list of all the guild's roles and their id's";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"rankinfo"};
        this.category = Category.UTILS;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
                String[] args = event.getArgs().split("\\s+");
                JDA jda = event.getJDA();
                Guild guild = event.getGuild();
                if (args.length == 1 && !args[0].equalsIgnoreCase("")) if (jda.getGuildById(args[0]) != null) guild = jda.getGuildById(args[0]);
                List<Role> roles = guild.getRoles();
                StringBuilder sb = new StringBuilder();
                int i = 1;
                for (Role role : roles) {
                    sb.append("[").append(role.getName()).append("] - ").append(role.getId()).append("\n");
                    if (sb.toString().length() > 1850) {
                        event.getChannel().sendMessage("Roles of " + guild.getName() + " part **#" + i + "**\n```INI\n" + sb.toString() + "```").queue();
                        sb = new StringBuilder();
                        i++;
                    }
                }
                if (sb.toString().length() != 0) event.reply("Roles of " + guild.getName() + " part **#" + i + "**\n```INI\n" + sb.toString() + "```");
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
