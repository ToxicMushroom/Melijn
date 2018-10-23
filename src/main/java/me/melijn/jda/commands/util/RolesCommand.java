package me.melijn.jda.commands.util;

import me.melijn.jda.Helpers;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;

import static me.melijn.jda.Melijn.PREFIX;

public class RolesCommand extends Command {

    public RolesCommand() {
        this.commandName = "roles";
        this.description = "Shows you a list of all the guild's roles and their id's";
        this.usage = PREFIX + commandName;
        this.aliases = new String[]{"rolelist"};
        this.category = Category.UTILS;
        this.needs = new Need[]{Need.GUILD};
    }


    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), this.commandName, 0)) {
            Guild guild = event.getGuild();
            SnowflakeCacheView<Role> roles = guild.getRoleCache();
            StringBuilder sb = new StringBuilder();
            int i = 1;
            int count = 1;
            for (Role role : roles) {
                sb.append(count++).append(" - [").append(role.getName()).append("] - ").append(role.getId()).append("\n");
                if (sb.length() > 1850) {
                    event.reply("Roles of " + guild.getName() + " part **#" + i + "**\n```INI\n" + sb.toString() + "```");
                    sb = new StringBuilder();
                    i++;
                }
            }
            if (sb.length() != 0)
                event.reply("Roles of " + guild.getName() + " part **#" + i + "**\n```INI\n" + sb.toString() + "```");
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
