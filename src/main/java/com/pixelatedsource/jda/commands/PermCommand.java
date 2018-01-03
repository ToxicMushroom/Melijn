package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

public class PermCommand extends Command {

    public PermCommand() {
        this.name = "perm";
        this.aliases = new String[]{"permission"};
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name + " <add|remove|clear|info> <role|roleId> [permission]\nCheck http://pixelnetwork.be/commands to see the permission for each command";
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        Member member = event.getGuild().getMember(event.getAuthor());
        Guild guild = event.getGuild();
        List<Role> roles = event.getMessage().getMentionedRoles();
        if (args.length < 2) {
            event.reply("Fill in all the values please.\nUse " + PixelatedBot.PREFIX + "help to check the usage of the command.");
            return;
        }
        switch (args[0]) {
            case "add":
                if (Helpers.hasPerm(member, this.name + ".add")) {
                    if (args.length == 3) {
                        if (roles.size() == 1)
                            PixelatedBot.mySQL.addPermission(guild, roles.get(0), args[2]);
                        else
                            PixelatedBot.mySQL.addPermission(guild, event.getJDA().getRoleById(args[1]), args[2]);
                    } else {
                        event.reply("Fill in all the values please.\nUse " + PixelatedBot.PREFIX + "help to check the usage of the command.");
                        return;
                    }
                } else {
                    event.reply(Helpers.noPerms + "`" + this.name + ".add`.");
                    return;
                }
                break;
            case "remove":
                if (Helpers.hasPerm(member, this.name + ".remove")) {
                    if (args.length == 3) {
                        if (roles.size() == 1)
                            PixelatedBot.mySQL.removePermission(guild, roles.get(0), args[2]);
                        else
                            PixelatedBot.mySQL.removePermission(guild, event.getJDA().getRoleById(args[1]), args[2]);
                    } else {
                        event.reply("Fill in all the values please.\nUse " + PixelatedBot.PREFIX + "help to check the usage of the command.");
                        return;
                    }
                } else {
                    event.reply(Helpers.noPerms + "`" + this.name + ".remove`.");
                    return;
                }
                break;
            case "clear":
                if (Helpers.hasPerm(member, this.name + ".clear")) {
                    if (args.length == 2) {
                        if (roles.size() == 1)
                            PixelatedBot.mySQL.clearPermissions(guild, roles.get(0));
                        else
                            PixelatedBot.mySQL.clearPermissions(guild, event.getJDA().getRoleById(args[1]));
                    } else {
                        event.reply("Fill in all the values please.\nUse " + PixelatedBot.PREFIX + "help to check the usage of the command.");
                        return;
                    }
                } else {
                    event.reply(Helpers.noPerms + "`" + this.name + ".remove`.");
                    return;
                }
                break;
            case "view":
                if (Helpers.hasPerm(member, this.name + ".view")) {

                }
                break;
            default:
                break;
        }
    }
}
