package com.pixelatedsource.jda.commands.util;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

public class RoleInfoCommand extends Command {

    public RoleInfoCommand() {
        this.name = "roleinfo";
        this.aliases = new String[]{"rankinfo"};
        this.help = "Shows you a list of all the guild's roles and their id's";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), this.name, 0)) {
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
                    event.reply("Roles of " + guild.getName() + " part **#" + i + "**\n```INI\n" + sb.toString() + "```");
                    sb = new StringBuilder();
                    i++;
                }
            }
            if (sb.toString().length() != 0) event.reply("Roles of " + guild.getName() + " part **#" + i + "**\n```INI\n" + sb.toString() + "```");
        }
    }
}
