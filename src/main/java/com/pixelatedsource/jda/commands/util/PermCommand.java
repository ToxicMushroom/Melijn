package com.pixelatedsource.jda.commands.util;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelatedBot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import java.util.List;

import static com.pixelatedsource.jda.PixelatedBot.mySQL;
import static net.dv8tion.jda.core.Permission.MESSAGE_EMBED_LINKS;

public class PermCommand extends Command {

    public PermCommand() {
        this.name = "perm";
        this.aliases = new String[]{"permission"};
        this.help = "Usage: " + PixelatedBot.PREFIX + this.name + " <add|remove|clear|info> <role|roleId> [permission]\nCheck http://pixelnetwork.be/commands to see the permission for each command";
        this.botPermissions = new Permission[]{MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] args = event.getArgs().split("\\s+");
        Member member = event.getGuild().getMember(event.getAuthor());
        Guild guild = event.getGuild();
        JDA jda = event.getJDA();
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
                                roleName = "@everyone";
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
                                roleName = jda.getRoleById(args[1]).getName();
                                mySQL.addPermission(guild, jda.getRoleById(args[1]), args[2]);
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
                                roleName = "@everyone";
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
                                roleName = jda.getRoleById(args[1]).getName();
                                mySQL.removePermission(guild, jda.getRoleById(args[1]), args[2]);
                            }
                            if (roleName.equals("error200002020")) event.reply("Error: the user that you tagged has no roles.");
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
                            roleName = "@everyone";
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
                            if (jda.getRoleById(args[1]) != null) {
                                roleName = jda.getRoleById(args[1]).getName();
                                mySQL.clearPermissions(guild, jda.getRoleById(args[1]));
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
                            if (!args[1].matches("\\d+") || jda.getRoleById(args[1]) == null) {
                                event.reply("`" + args[1] + "` is not a valid id. exampleId: '260424455270957058'");
                                return;
                            }
                            role = jda.getRoleById(args[1]);
                        }

                        String roleName;
                        if (role == null && error) {
                            event.reply("Error: the user that you tagged has no roles.");
                            return;
                        } else roleName = role == null ? "@everyone" : role.getName();

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
            case "copy":
                if (Helpers.hasPerm(member, this.name + ".copy", 1)) {
                    if (args.length == 3) {
                        if (args[1].equals(args[2])) {
                            event.reply("That doesn't make sense to me..");
                            return;
                        }
                        String[] rawArgs = event.getMessage().getContentRaw().split("\\s+");
                        List<Role> mentionedRoles = event.getMessage().getMentionedRoles();
                        List<Member> mentionedMembers = event.getMessage().getMentionedMembers();
                        for (Member memberCheck : mentionedMembers) {
                            if (memberCheck.getRoles().size() == 0) {
                                event.reply("One of the targets has no roles");
                                return;
                            }
                        }
                        int fallback = 0;
                        if (rawArgs[2].equalsIgnoreCase("everyone")) {
                            rawArgs[2] = guild.getRoles().get(0).getId();
                            fallback = 1;
                        }
                        if (rawArgs[3].equalsIgnoreCase("everyone")) {
                            rawArgs[3] = guild.getRoles().get(0).getId();
                            fallback = 2;
                        }
                        Role role1;
                        Role role2;
                        int mentionedMembersCount = mentionedMembers.size();
                        int mentionedRolesCount = mentionedRoles.size();
                        if (mentionedMembersCount == 2) {
                            if (rawArgs[2].equals("<@" + mentionedMembers.get(0).getUser().getId() + ">")) {
                                role1 = mentionedMembers.get(0).getRoles().get(0);
                                role2 = mentionedMembers.get(1).getRoles().get(0);
                            } else {
                                role1 = mentionedMembers.get(1).getRoles().get(0);
                                role2 = mentionedMembers.get(0).getRoles().get(0);
                            }
                        } else if (mentionedRolesCount == 2) {
                            if (rawArgs[2].equals("<@" + mentionedRoles.get(0).getId() + ">")) {
                                role1 = mentionedRoles.get(0);
                                role2 = mentionedRoles.get(1);
                            } else {
                                role1 = mentionedRoles.get(1);
                                role2 = mentionedRoles.get(0);
                            }
                        } else if (mentionedMembersCount == 1 && mentionedRolesCount == 1) {
                            if (rawArgs[2].equals("<@" + mentionedMembers.get(0).getUser().getId() + ">")) {
                                role1 = mentionedMembers.get(0).getRoles().get(0);
                                role2 = mentionedRoles.get(0);
                            } else {
                                role1 = mentionedRoles.get(0);
                                role2 = mentionedMembers.get(0).getRoles().get(0);
                            }
                        } else if (mentionedMembersCount == 1 && mentionedRolesCount == 0) {
                            if (rawArgs[2].equals("<@" + mentionedMembers.get(0).getUser().getId() + ">")) {
                                role1 = mentionedMembers.get(0).getRoles().get(0);
                                if (rawArgs[3].matches("\\d+")) {
                                    if (jda.getUserById(rawArgs[3]) != null) {
                                        if (guild.getMember(jda.getUserById(rawArgs[3])) != null) {
                                            if (guild.getMember(jda.getUserById(rawArgs[3])).getRoles().size() != 0) {
                                                role2 = guild.getMember(jda.getUserById(rawArgs[3])).getRoles().get(0);
                                            } else {
                                                event.reply("(Arg 2) Member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("(Arg 2) User isn't a member!");
                                            return;
                                        }
                                    } else if (jda.getRoleById(rawArgs[3]) != null) {
                                        role2 = jda.getRoleById(rawArgs[3]);
                                    } else {
                                        event.reply("(Arg 2) Unknown Id!");
                                        return;
                                    }
                                } else {
                                    event.reply("(Arg 2) Isn't an Id!");
                                    return;
                                }
                            } else {
                                role2 = mentionedMembers.get(0).getRoles().get(0);
                                if (rawArgs[2].matches("\\d+")) {
                                    if (jda.getUserById(rawArgs[2]) != null) {
                                        if (guild.getMember(jda.getUserById(rawArgs[2])) != null) {
                                            if (guild.getMember(jda.getUserById(rawArgs[2])).getRoles().size() != 0) {
                                                role1 = guild.getMember(jda.getUserById(rawArgs[2])).getRoles().get(0);
                                            } else {
                                                event.reply("(Arg 1) Member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("(Arg 1) User isn't a member!");
                                            return;
                                        }
                                    } else if (jda.getRoleById(rawArgs[2]) != null) {
                                        role1 = jda.getRoleById(rawArgs[2]);
                                    } else {
                                        event.reply("(Arg 1) Unknown Id!");
                                        return;
                                    }
                                } else {
                                    event.reply("(Arg 1) Isn't an Id");
                                    return;
                                }
                            }
                        } else if (mentionedMembersCount == 0 && mentionedRolesCount == 1) {
                            if (rawArgs[2].equals("<@" + mentionedRoles.get(0).getId() + ">")) {
                                role1 = mentionedRoles.get(0);
                                if (rawArgs[3].matches("\\d+")) {
                                    if (jda.getUserById(rawArgs[3]) != null) {
                                        if (guild.getMember(jda.getUserById(rawArgs[3])) != null) {
                                            if (guild.getMember(jda.getUserById(rawArgs[3])).getRoles().size() != 0) {
                                                role2 = guild.getMember(jda.getUserById(rawArgs[3])).getRoles().get(0);
                                            } else {
                                                event.reply("(Arg 2) Member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("(Arg 2) User isn't a member!");
                                            return;
                                        }
                                    } else if (jda.getRoleById(rawArgs[3]) != null) {
                                        role2 = jda.getRoleById(rawArgs[3]);
                                    } else {
                                        event.reply("(Arg 2) Unknown Id!");
                                        return;
                                    }
                                } else {
                                    event.reply("(Arg 2) isn't an Id!");
                                    return;
                                }
                            } else {
                                role2 = mentionedRoles.get(0);
                                if (rawArgs[2].matches("\\d+")) {
                                    if (jda.getUserById(rawArgs[2]) != null) {
                                        if (guild.getMember(jda.getUserById(rawArgs[2])) != null) {
                                            if (guild.getMember(jda.getUserById(rawArgs[2])).getRoles().size() != 0) {
                                                role1 = guild.getMember(jda.getUserById(rawArgs[2])).getRoles().get(0);
                                            } else {
                                                event.reply("(Arg 1) Member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("(Arg 1) User isn't a member!");
                                            return;
                                        }
                                    } else if (jda.getRoleById(rawArgs[2]) != null) {
                                        role1 = jda.getRoleById(rawArgs[2]);
                                    } else {
                                        event.reply("(Arg 1) Unknown Id!");
                                        return;
                                    }
                                } else {
                                    event.reply("(Arg 1) Isn't an Id!");
                                    return;
                                }
                            }
                        } else if (mentionedMembersCount == 0 && mentionedRolesCount == 0) {
                            if (rawArgs[2].matches("\\d+") && rawArgs[3].matches("\\d+")) {
                                if (guild.getRoleById(rawArgs[2]) != null) {
                                    role1 = guild.getRoleById(rawArgs[2]);
                                    if (guild.getRoleById(rawArgs[3]) != null) {
                                        role2 = guild.getRoleById(rawArgs[3]);
                                    } else if (jda.getUserById(rawArgs[3]) != null) {
                                        User user = jda.getUserById(rawArgs[3]);
                                        if (guild.getMember(user) != null) {
                                            if (guild.getMember(user).getRoles().size() != 0) {
                                                role2 = guild.getMember(user).getRoles().get(0);
                                            } else {
                                                event.reply("(Arg 2) The member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("(Arg 2) User isn't a member!");
                                            return;
                                        }
                                    } else {
                                        event.reply("(Arg 2) Unknown Id!");
                                        return;
                                    }
                                } else if (jda.getUserById(rawArgs[2]) != null) {
                                    if (jda.getUserById(rawArgs[3]) != null) {
                                        User user1 = jda.getUserById(rawArgs[2]);
                                        User user2 = jda.getUserById(rawArgs[3]);
                                        if (guild.getMember(user1) != null && guild.getMember(user2) != null) {
                                            if (guild.getMember(user1).getRoles().size() != 0 && guild.getMember(user2).getRoles().size() != 0) {
                                                role1 = guild.getMember(user1).getRoles().get(0);
                                                role2 = guild.getMember(user2).getRoles().get(0);
                                            } else {
                                                event.reply("At least 1 member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("At least 1 user isn't a member of this guild!");
                                            return;
                                        }
                                    } else if (guild.getRoleById(rawArgs[3]) != null) {
                                        role2 = guild.getRoleById(rawArgs[3]);
                                        User user = jda.getUserById(rawArgs[2]);
                                        if (guild.getMember(user) != null) {
                                            if (guild.getMember(user).getRoles().size() != 0) {
                                                role1 = guild.getMember(user).getRoles().get(0);
                                            } else {
                                                event.reply("(Arg 1) The member has no roles!");
                                                return;
                                            }
                                        } else {
                                            event.reply("(Arg 1) User isn't a member!");
                                            return;
                                        }
                                    } else {
                                        event.reply("(Arg 2) Unknown Id!");
                                        return;
                                    }
                                } else {
                                    event.reply("(Arg 1) Unknown Id!");
                                    return;
                                }
                            } else {
                                event.reply("Your args aren't Id's nor mentions!");
                                return;
                            }
                        } else {
                            event.reply("To many tagged roles or users!");
                            return;
                        }
                        if (fallback == 1) role1 = null;
                        if (fallback == 2) role2 = null;
                        if (role1 == role2) {
                            event.reply("That doesn't make sense to me..");
                            return;
                        }
                        PixelatedBot.mySQL.copyPermissions(guild, role1, role2);
                        String roleName1 = role1 == null ? "@everyone" : role1.getName();
                        String roleName2 = role2 == null ? "@everyone" : role2.getName();
                        event.reply("I copied all permissions from `" + roleName1 + "` to `" + roleName2 + "`.");
                    }
                }
                break;
            default:
                break;
        }
    }
}
