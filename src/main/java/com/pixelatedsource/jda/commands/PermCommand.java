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

import static com.pixelatedsource.jda.PixelatedBot.mySQL;

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
                if (Helpers.hasPerm(member, this.name + ".add", 1)) {
                    if (args.length == 3) {
                        if (Helpers.perms.contains(args[2])) {
                            String roleName;
                            if (args[1].equalsIgnoreCase("everyone")) {
                                roleName = "everyone";
                                mySQL.addPermission(guild, null, args[2]);
                            } else if (roles.size() == 1) {
                                roleName = roles.get(0).getName();
                                mySQL.addPermission(guild, roles.get(0), args[2]);
                            } else if (event.getMessage().getMentionedMembers().size() == 1) {
                                if (event.getMessage().getMentionedMembers().get(0).getRoles().size() != 0) {
                                    roleName = event.getMessage().getMentionedMembers().get(0).getRoles().get(0).getName();
                                    mySQL.addPermission(guild, event.getMessage().getMentionedMembers().get(0).getRoles().get(0), args[2]);
                                } else {
                                    roleName = "error200002020";
                                }
                            } else {
                                roleName = event.getJDA().getRoleById(args[1]).getName();
                                mySQL.addPermission(guild, event.getJDA().getRoleById(args[1]), args[2]);
                            }
                            switch (roleName) {
                                case "error200002020":
                                    event.reply("Error: the user that you tagged has no roles.");
                                    break;
                                default:
                                    event.reply("`" + args[2] + "`" + " has been added to " + roleName);
                                    break;
                            }
                        } else {
                            event.reply("The provided permission is not inside the list on http://pixelnetwork.be/commands");
                        }
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
                if (Helpers.hasPerm(member, this.name + ".remove", 1)) {
                    if (args.length == 3) {
                        if (Helpers.perms.contains(args[2])) {
                            String roleName;
                            if (args[1].equalsIgnoreCase("everyone")) {
                                roleName = "everyone";
                                mySQL.removePermission(guild, null, args[2]);
                            } else if (roles.size() == 1) {
                                roleName = roles.get(0).getName();
                                mySQL.removePermission(guild, roles.get(0), args[2]);
                            } else if (event.getMessage().getMentionedMembers().size() == 1) {
                                if (event.getMessage().getMentionedMembers().get(0).getRoles().size() != 0) {
                                    roleName = event.getMessage().getMentionedMembers().get(0).getRoles().get(0).getName();
                                    mySQL.removePermission(guild, event.getMessage().getMentionedMembers().get(0).getRoles().get(0), args[2]);
                                } else {
                                    roleName = "error200002020";
                                }
                            } else {
                                roleName = event.getJDA().getRoleById(args[1]).getName();
                                mySQL.removePermission(guild, event.getJDA().getRoleById(args[1]), args[2]);
                            }
                            if (roleName.equals("error200002020"))
                                event.reply("Error: the user that you tagged has no roles.");
                            else event.reply("`" + args[2] + "`" + " has been deleted from " + roleName);
                        } else {
                            event.reply("The provided permission is not inside the list on http://pixelnetwork.be/commands");
                        }
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
                if (Helpers.hasPerm(member, this.name + ".clear", 1)) {
                    if (args.length == 2) {
                        String roleName = "error";
                        if (args[1].equalsIgnoreCase("everyone")) {
                            roleName = "everyone";
                            mySQL.clearPermissions(guild, null);
                        } else if (roles.size() == 1) {
                            roleName = roles.get(0).getName();
                            mySQL.clearPermissions(guild, roles.get(0));
                        } else if (event.getMessage().getMentionedMembers().size() == 1) {
                            if (event.getMessage().getMentionedMembers().get(0).getRoles().size() != 0) {
                                roleName = event.getMessage().getMentionedMembers().get(0).getRoles().get(0).getName();
                                mySQL.clearPermissions(guild, event.getMessage().getMentionedMembers().get(0).getRoles().get(0));
                            } else {
                                roleName = "error200002020";
                            }
                        } else {
                            if (event.getJDA().getRoleById(args[1]) != null) {
                                roleName = event.getJDA().getRoleById(args[1]).getName();
                                mySQL.clearPermissions(guild, event.getJDA().getRoleById(args[1]));
                            }
                        }
                        switch (roleName) {
                            case "error":
                                event.reply("Error: " + args[1] + " is not a valid id.");
                                break;
                            case "error200002020":
                                event.reply("Error: the user that you tagged has no roles.");
                                break;
                            default:
                                event.reply("Permissions cleared for " + roleName);
                                break;
                        }
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
                if (Helpers.hasPerm(member, this.name + ".view", 0)) {
                    if (args.length == 2) {
                        List<String> lijst;
                        Role role;
                        boolean error = false;
                        if (args[1].equalsIgnoreCase("everyone")) {
                            role = null;
                        } else if (roles.size() == 1) {
                            role = roles.get(0);
                        } else if (event.getMessage().getMentionedMembers().size() == 1) {
                            if (event.getMessage().getMentionedMembers().get(0).getRoles().size() != 0) {
                                role = event.getMessage().getMentionedMembers().get(0).getRoles().get(0);
                            } else {
                                error = true;
                                role = null;
                            }
                        } else {
                            if (!args[1].matches("0-9") || event.getJDA().getRoleById(args[1]) == null) {
                                event.reply("`" + args[1] + "` is not a valid id. exampleId: '260424455270957058'");
                                return;
                            }
                            role = event.getJDA().getRoleById(args[1]);
                        }

                        String roleName;
                        if (role == null && error) {
                            event.reply("Error: the user that you tagged has no roles.");
                            return;
                        } else roleName = role == null ? "everyone" : role.getName();

                        lijst = mySQL.getPermissions(guild, role);
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
                                    eb.setTitle("Permissions of " + roleName + " #" + part);
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
                            eb.setTitle("Permissions of " + roleName + " #" + (part + 1));
                            eb.setColor(Helpers.EmbedColor);
                            eb.setDescription(builder.toString());
                            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                            event.reply(eb.build());
                        } else {
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Permissions of " + roleName);
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
