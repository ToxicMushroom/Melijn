package com.pixelatedsource.jda.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import net.dv8tion.jda.core.EmbedBuilder;
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
                        String roleName;
                        if (args[1].equalsIgnoreCase("everyone")) {
                            roleName = "everyone";
                            PixelatedBot.mySQL.addPermission(guild, null, args[2]);
                        } else if (roles.size() == 1) {
                            roleName = roles.get(0).getName();
                            PixelatedBot.mySQL.addPermission(guild, roles.get(0), args[2]);
                        } else {
                            roleName = event.getJDA().getRoleById(args[1]).getName();
                            PixelatedBot.mySQL.addPermission(guild, event.getJDA().getRoleById(args[1]), args[2]);
                        }
                        event.reply("`" + args[2] + "`" + " has been added to " + roleName);
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
                        String roleName;
                        if (args[1].equalsIgnoreCase("everyone")) {
                            roleName = "everyone";
                            PixelatedBot.mySQL.removePermission(guild, null, args[2]);
                        } else if (roles.size() == 1) {
                            roleName = roles.get(0).getName();
                            PixelatedBot.mySQL.removePermission(guild, roles.get(0), args[2]);
                        } else {
                            roleName = event.getJDA().getRoleById(args[1]).getName();
                            PixelatedBot.mySQL.removePermission(guild, event.getJDA().getRoleById(args[1]), args[2]);
                        }
                        event.reply("`" + args[2] + "`" + " has been deleted from " + roleName);
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
                        String roleName;
                        if (args[1].equalsIgnoreCase("everyone")) {
                            roleName = "everyone";
                            PixelatedBot.mySQL.clearPermissions(guild, null);
                        } else if (roles.size() == 1) {
                            roleName = roles.get(0).getName();
                            PixelatedBot.mySQL.clearPermissions(guild, roles.get(0));
                        } else {
                            roleName = event.getJDA().getRoleById(args[1]).getName();
                            PixelatedBot.mySQL.clearPermissions(guild, event.getJDA().getRoleById(args[1]));
                        }
                        event.reply("Permissions cleared for " + roleName);
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
                    if (args.length == 2) {
                        List<String> lijst;
                        Role role;

                        if (args[1].equalsIgnoreCase("everyone")) {
                            role = null;
                        } else if (roles.size() == 1) role = roles.get(0);
                        else role = event.getJDA().getRoleById(args[1]);
                        String roleName = role == null ? "everyone" : role.getName();


                        lijst = PixelatedBot.mySQL.getPermissions(guild, role);
                        StringBuilder builder = new StringBuilder();
                        for (String s : lijst) {
                            builder.append(s).append("\n");
                        }
                        if (builder.toString().length() > 1800) {
                            int part = 1;
                            builder = new StringBuilder();
                            for (String s : lijst) {
                                if (builder.toString().length() + s.length() > 1800) {
                                    EmbedBuilder eb = new EmbedBuilder();
                                    eb.setTitle("Permission of " + roleName + " #" + part);
                                    eb.setColor(Helpers.EmbedColor);
                                    eb.setDescription(builder.toString());
                                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                                    event.reply(eb.build());
                                    builder = new StringBuilder();
                                    part++;
                                }
                                builder.append(s).append("\n");
                            }
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Permission of " + roleName + " #"+ (part + 1));
                            eb.setColor(Helpers.EmbedColor);
                            eb.setDescription(builder.toString());
                            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                            event.reply(eb.build());
                        } else {
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Permission of " + roleName);
                            eb.setColor(Helpers.EmbedColor);
                            eb.setDescription(builder.toString());
                            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                            event.reply(eb.build());
                        }
                    } else {
                        event.reply("Fill in all the values please.\nUse " + PixelatedBot.PREFIX + "help to check the usage of the command.");
                        return;
                    }
                } else {
                    event.reply(Helpers.noPerms + "`" + this.name + ".view`.");
                    return;
                }
                break;
            default:
                break;
        }
    }
}
