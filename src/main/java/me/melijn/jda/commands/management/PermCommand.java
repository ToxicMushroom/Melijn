package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static me.melijn.jda.Melijn.PREFIX;

public class PermCommand extends Command {

    public PermCommand() {
        this.commandName = "perm";
        this.description = "Main command to manage access to commands for specific users/roles";
        this.usage = PREFIX + commandName + " <add | remove | view | clear | copy | list>";
        this.extra = "A permission is just the name of the command and you'll get more info for each sub section of the command if you just use it wrong or without more arguments";
        this.aliases = new String[]{"permission"};
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.id = 71;
    }

    @Override
    protected void execute(CommandEvent event) {
        String prefix = event.getVariables().prefixes.get(event.getGuild().getIdLong());
        String[] args = event.getArgs().split("\\s+");
        MySQL mySQL = event.getMySQL();

        Member member = event.getMember();
        Guild guild = event.getGuild();
        Message message = event.getMessage();
        JDA jda = event.getJDA();
        List<Role> mentionedRoles = message.getMentionedRoles();
        List<User> mentionedUsers = message.getMentionedUsers();
        switch (args[0]) {
            case "add":
                if (!event.hasPerm(member, commandName + ".add", 1)) {
                    event.reply("You need the permission `" + commandName + ".add` to execute this command.");
                    return;
                }

                if (args.length != 3) {
                    event.reply(prefix + commandName + " add <role | user | everyone> <permission>");
                    return;
                }

                if (!event.getHelpers().perms.contains(args[2])) {
                    event.reply("Unknown permission\n" + prefix + commandName + " list");
                    return;
                }

                String mode = retrieveMode(args[1]);
                switch (mode) {
                    case "user":
                        if (mentionedUsers.size() == 1) {
                            User target = mentionedUsers.get(0);
                            mySQL.addUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "#" + target.getDiscriminator() + "`");
                        }
                        break;
                    case "role":
                        if (mentionedRoles.size() == 1) {
                            Role target = mentionedRoles.get(0);
                            mySQL.addRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` added to `@" + target.getName() + "`");
                        }
                        break;
                    case "id":
                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                            event.reply(prefix + commandName + " add <role | user | everyone> <permission>");
                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                            User target = jda.getUserById(args[1]);
                            mySQL.addUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "#" + target.getDiscriminator() + "`");
                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                            Role target = guild.getRoleById(args[1]);
                            mySQL.addRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "`");
                        } else {
                            event.reply("NANI!?");
                            return;
                        }
                        break;
                    case "everyone":
                        mySQL.addRolePermission(guild.getIdLong(), guild.getIdLong(), args[2]);
                        event.reply("Permission: `" + args[2] + "` added to `@everyone`");
                        break;
                    case "name":
                        if (guild.getRolesByName(args[1], true).size() > 0) {
                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                mySQL.addRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` added to `%s`", args[1]));
                            } else {
                                mySQL.addRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` added to `%s`", guild.getRolesByName(args[1], true).get(0).getName()));
                            }
                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByName(args[1], false).get(0).getUser()));
                            } else {
                                mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByName(args[1], true).get(0).getUser()));
                            }
                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByNickname(args[1], false).get(0).getUser()));
                            } else {
                                mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByNickname(args[1], true).get(0).getUser()));
                            }
                        } else {
                            event.reply("No role or user found with that name or nickname");
                        }
                        break;
                    default:
                        event.reply(prefix + commandName + " add <role | user | everyone> <permission>");
                        break;
                }
                break;
            case "remove":
                if (!event.hasPerm(member, this.commandName + ".remove", 1)) {
                    event.reply("You need the permission `" + commandName + ".remove` to execute this command.");
                    return;
                }
                if (args.length != 3) {
                    event.reply(prefix + commandName + " remove <role | user | everyone> <permission>");
                    return;
                }
                if (!event.getHelpers().perms.contains(args[2])) {
                    event.reply("Unknown permission\n" + prefix + commandName + " list");
                    return;
                }

                mode = retrieveMode(args[1]);
                switch (mode) {
                    case "user":
                        if (mentionedUsers.size() == 1) {
                            User target = mentionedUsers.get(0);
                            mySQL.removeUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "#" + target.getDiscriminator() + "`");
                        }
                        break;
                    case "role":
                        if (mentionedRoles.size() == 1) {
                            Role target = mentionedRoles.get(0);
                            mySQL.removeRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "`");
                        }
                        break;
                    case "id":
                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                            event.reply(prefix + commandName + " remove <role | user | everyone> <permission>");
                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                            User target = jda.getUserById(args[1]);
                            mySQL.removeUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "#" + target.getDiscriminator() + "`");
                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                            Role target = guild.getRoleById(args[1]);
                            mySQL.removeRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "`");
                        } else {
                            event.reply("NANI!?");
                            return;
                        }
                        break;
                    case "everyone":
                        mySQL.removeRolePermission(guild.getIdLong(), guild.getIdLong(), args[2]);
                        event.reply("Permission: `" + args[2] + "` removed from `@everyone`");
                        break;
                    case "name":
                        if (guild.getRolesByName(args[1], true).size() > 0) {
                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                mySQL.removeRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%s`", args[1]));
                            } else {
                                mySQL.removeRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%s`", guild.getRolesByName(args[1], true).get(0).getName()));
                            }
                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByName(args[1], false).get(0).getUser()));
                            } else {
                                mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByName(args[1], true).get(0).getUser()));
                            }
                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByNickname(args[1], false).get(0).getUser()));
                            } else {
                                mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong(), args[2]);
                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByNickname(args[1], true).get(0).getUser()));
                            }
                        } else {
                            event.reply("No role or user found with that name or nickname");
                        }
                        break;
                    default:
                        event.reply(prefix + commandName + " remove <role | user | everyone> <permission>");
                        break;
                }
                break;
            case "clear":
                if (!event.hasPerm(member, this.commandName + ".clear", 1)) {
                    event.reply("You need the permission `" + commandName + ".clear` to execute this command.");
                    return;
                }

                if (args.length != 2) {
                    event.reply(prefix + commandName + " clear <role | user | everyone>");
                    return;
                }

                mode = retrieveMode(args[1]);
                switch (mode) {
                    case "user":
                        if (mentionedUsers.size() > 0) {
                            User target = mentionedUsers.get(0);
                            mySQL.clearUserPermissions(guild.getIdLong(), target.getIdLong());
                            event.reply("Permissions off `" + target.getName() + "#" + target.getDiscriminator() + "` have been cleared");
                        }
                        break;
                    case "role":
                        if (mentionedRoles.size() > 0) {
                            Role target = mentionedRoles.get(0);
                            mySQL.clearRolePermissions(guild.getIdLong(), target.getIdLong());
                            event.reply("Permissions off `@" + target.getName() + "` have been cleared");
                        }
                        break;
                    case "id":
                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                            event.reply(prefix + commandName + " clear <role | user | everyone>");
                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                            User target = jda.getUserById(args[1]);
                            mySQL.clearUserPermissions(guild.getIdLong(), target.getIdLong());
                            event.reply("Permissions off `" + target.getName() + "#" + target.getDiscriminator() + "` have been cleared");
                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                            Role target = guild.getRoleById(args[1]);
                            mySQL.clearRolePermissions(guild.getIdLong(), target.getIdLong());
                            event.reply("Permissions off `" + target.getName() + "` have been cleared");
                        } else {
                            event.reply("NANI!?");
                            return;
                        }
                        break;
                    case "everyone":
                        mySQL.clearRolePermissions(guild.getIdLong(), guild.getIdLong());
                        event.reply("Permissions off `@everyone` have been cleared");
                        break;
                    case "name":
                        if (guild.getRolesByName(args[1], true).size() > 0) {
                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                mySQL.clearRolePermissions(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong());
                                event.reply(String.format("Permissions of `%s` have been cleared", args[1]));
                            } else {
                                mySQL.clearRolePermissions(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong());
                                event.reply(String.format("Permissions of `%s` have been cleared", guild.getRolesByName(args[1], true).get(0).getName()));
                            }
                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong());
                                event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByName(args[1], false).get(0).getUser()));
                            } else {
                                mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong());
                                event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByName(args[1], true).get(0).getUser()));
                            }
                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong());
                                event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByNickname(args[1], false).get(0).getUser()));
                            } else {
                                mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong());
                                event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByNickname(args[1], true).get(0).getUser()));
                            }
                        } else {
                            event.reply("No role or user found with that name or nickname");
                        }
                        break;
                    default:
                        event.reply(prefix + commandName + " clear <role | user | everyone>");
                        break;
                }
                break;
            case "view":
                if (!event.hasPerm(member, this.commandName + ".view", 0)) {
                    event.reply("You need the permission `" + commandName + ".view` to execute this command.");
                    return;
                }

                if (args.length != 2) {
                    event.reply(prefix + commandName + " view <role | user | everyone>");
                    return;
                }

                Set<String> lijst = new HashSet<>();
                String targetName = "error";

                mode = retrieveMode(args[1]);
                switch (mode) {
                    case "user":
                        if (mentionedUsers.size() > 0) {
                            lijst = mySQL.getUserPermissions(guild.getIdLong(), mentionedUsers.get(0).getIdLong());
                            targetName = mentionedUsers.get(0).getName() + "#" + mentionedUsers.get(0).getDiscriminator();
                        }
                        break;
                    case "role":
                        if (mentionedRoles.size() > 0) {
                            lijst = mySQL.getRolePermissions(guild.getIdLong(), mentionedRoles.get(0).getIdLong());
                            targetName = mentionedRoles.get(0).getName();
                        }
                        break;
                    case "id":
                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                            event.reply(prefix + commandName + " view <role | user | everyone>");
                        } else if (guild.getMemberById(args[1]) != null) {
                            User target = jda.getUserById(args[1]);
                            lijst = mySQL.getUserPermissions(guild.getIdLong(), target.getIdLong());
                            targetName = target.getName() + "#" + target.getDiscriminator();
                        } else if (guild.getRoleById(args[1]) != null) {
                            Role role = guild.getRoleById(args[1]);
                            lijst = mySQL.getRolePermissions(guild.getIdLong(), role.getIdLong());
                            targetName = role.getName();
                        } else {
                            event.reply("NANI!?");
                            return;
                        }
                        break;
                    case "everyone":
                        lijst = mySQL.getRolePermissions(guild.getIdLong(), guild.getIdLong());
                        targetName = "@everyone";
                        break;
                    case "name":
                        if (guild.getRolesByName(args[1], true).size() > 0) {
                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                Role role = guild.getRolesByName(args[1], false).get(0);
                                lijst = mySQL.getRolePermissions(guild.getIdLong(), role.getIdLong());
                                targetName = role.getName();
                            } else {
                                Role role = guild.getRolesByName(args[1], true).get(0);
                                lijst = mySQL.getRolePermissions(guild.getIdLong(), role.getIdLong());
                                targetName = role.getName();
                            }
                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                User user = guild.getMembersByName(args[1], false).get(0).getUser();
                                lijst = mySQL.getUserPermissions(guild.getIdLong(), user.getIdLong());
                                targetName = user.getName() + "#" + user.getDiscriminator();
                            } else {
                                User user = guild.getMembersByName(args[1], true).get(0).getUser();
                                lijst = mySQL.getUserPermissions(guild.getIdLong(), user.getIdLong());
                                targetName = user.getName() + "#" + user.getDiscriminator();
                            }
                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                User user = guild.getMembersByNickname(args[1], false).get(0).getUser();
                                lijst = mySQL.getUserPermissions(guild.getIdLong(), user.getIdLong());
                                targetName = user.getName() + "#" + user.getDiscriminator();
                            } else {
                                User user = guild.getMembersByNickname(args[1], true).get(0).getUser();
                                lijst = mySQL.getUserPermissions(guild.getIdLong(), user.getIdLong());
                                targetName = user.getName() + "#" + user.getDiscriminator();
                            }
                        } else {
                            event.reply("No role or user found with that name or nickname");
                        }
                        break;
                    default:
                        event.reply(prefix + commandName + " view <role | user | everyone>");
                        return;
                }
                int partNumber = 0;
                StringBuilder sb = new StringBuilder();
                for (String s : lijst) {
                    sb.append(s).append("\n");
                    if (sb.length() > 1900) {
                        event.reply(new Embedder(event.getVariables(), event.getGuild())
                                .setTitle("Permissions off `" + targetName + "` part #" + partNumber++)
                                .setDescription(sb.toString())
                                .build());
                        sb = new StringBuilder();
                    }
                }
                String title = partNumber == 0 ? "Permissions off `" + targetName + "`" : "Permissions off `" + targetName + "` part #" + partNumber;
                event.reply(new Embedder(event.getVariables(), event.getGuild())
                        .setTitle(title)
                        .setDescription(sb.toString())
                        .build());
                break;
            case "list":
                sb = new StringBuilder();
                int count = 0;

                for (String perm : event.getHelpers().perms) {
                    sb.append(++count).append(". [").append(perm).append("]").append("\n");
                }
                event.getMessageHelper().sendSplitCodeBlock(event.getTextChannel(), sb.toString(), "INI");
                break;
            case "copy":
                if (!event.hasPerm(member, this.commandName + ".copy", 1)) {
                    event.reply("You need the permission `" + commandName + ".copy` to execute this command.");
                    return;
                }

                if (args.length != 3) {
                    event.reply(prefix + commandName + " copy <role | user | everyone> <role | user | everyone>");
                    return;
                }


                String transmitterMode = retrieveMode(args[1]);
                String receiverMode = retrieveMode(args[2]);
                User transmitter = null;
                User receiver = null;
                Role transmitterRole = null;
                Role receiverRole = null;

                switch (transmitterMode) {
                    case "user":
                        if (mentionedUsers.size() > 1) {
                            transmitter = mentionedUsers.get(0);
                        }
                        break;
                    case "role":
                        if (mentionedRoles.size() > 1) {
                            transmitterRole = mentionedRoles.get(0);
                        }
                        break;
                    case "id":
                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                            event.reply(prefix + commandName + " copy <role | user | everyone> <role | user | everyone>");
                            return;
                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                            transmitter = jda.getUserById(args[1]);
                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                            transmitterRole = guild.getRoleById(args[1]);
                        } else {
                            event.reply("NANI!?");
                            return;
                        }
                        break;
                    case "everyone":
                        transmitterRole = guild.getRoleById(guild.getId());
                        break;
                    case "name":
                        if (guild.getRolesByName(args[1], true).size() > 0) {
                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                transmitterRole = guild.getRolesByName(args[1], false).get(0);
                            } else {
                                transmitterRole = guild.getRolesByName(args[1], true).get(0);
                            }
                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                transmitter = guild.getMembersByName(args[1], false).get(0).getUser();
                            } else {
                                transmitter = guild.getMembersByName(args[1], true).get(0).getUser();
                            }
                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                transmitter = guild.getMembersByNickname(args[1], false).get(0).getUser();
                            } else {
                                transmitter = guild.getMembersByNickname(args[1], true).get(0).getUser();
                            }
                        }
                        break;
                    default:
                        event.reply(prefix + commandName + " copy <role | user | everyone> <role | user | everyone>");
                        return;
                }

                switch (receiverMode) {
                    case "user":
                        if (mentionedUsers.size() > 0) {
                            receiver = mentionedUsers.get(mentionedUsers.size() - 1);
                        }
                        break;
                    case "role":
                        if (mentionedRoles.size() > 0) {
                            receiverRole = mentionedRoles.get(mentionedRoles.size() - 1);
                        }
                        break;
                    case "id":
                        if (guild.getRoleById(args[2]) == null && guild.getMemberById(args[2]) == null) {
                            event.reply(prefix + commandName + " copy <role | user | everyone> <role | user | everyone>");
                            return;
                        } else if (guild.getRoleById(args[2]) == null && guild.getMemberById(args[2]) != null) {
                            receiver = jda.getUserById(args[2]);
                        } else if (guild.getRoleById(args[2]) != null && guild.getMemberById(args[2]) == null) {
                            receiverRole = guild.getRoleById(args[2]);
                        } else {
                            event.reply("NANI!?");
                            return;
                        }
                        break;
                    case "everyone":
                        receiverRole = guild.getRoleById(guild.getId());
                        break;
                    case "name":
                        if (guild.getRolesByName(args[2], true).size() > 0) {
                            if (guild.getRolesByName(args[2], false).size() > 0) {
                                receiverRole = guild.getRolesByName(args[2], false).get(0);
                            } else {
                                receiverRole = guild.getRolesByName(args[2], true).get(0);
                            }
                        } else if (guild.getMembersByName(args[2], true).size() > 0) {
                            if (guild.getMembersByName(args[2], false).size() > 0) {
                                receiver = guild.getMembersByName(args[2], false).get(0).getUser();
                            } else {
                                receiver = guild.getMembersByName(args[2], true).get(0).getUser();
                            }
                        } else if (guild.getMembersByNickname(args[2], true).size() > 0) {
                            if (guild.getMembersByNickname(args[2], false).size() > 0) {
                                receiver = guild.getMembersByNickname(args[2], false).get(0).getUser();
                            } else {
                                receiver = guild.getMembersByNickname(args[2], true).get(0).getUser();
                            }
                        }
                        break;
                    default:
                        event.reply(prefix + commandName + " copy <role | user | everyone> <role | user | everyone>");
                        return;
                }

                if (transmitter != null && receiver != null) {
                    mySQL.copyUserPermissions(guild.getIdLong(), transmitter.getIdLong(), receiver.getIdLong());
                    event.reply("Copied all permissions from `" + transmitter.getName() + "#" + transmitter.getDiscriminator() + "` to `" + receiver.getName() + "#" + receiver.getDiscriminator() + "`");
                } else if (transmitter != null && receiverRole != null) {
                    mySQL.copyUserRolePermissions(guild.getIdLong(), transmitter.getIdLong(), receiverRole.getIdLong());
                } else if (transmitterRole != null && receiverRole != null) {
                    mySQL.copyRolePermissions(guild.getIdLong(), transmitterRole.getIdLong(), receiverRole.getIdLong());
                } else if (transmitterRole != null && receiver != null) {
                    mySQL.copyRoleUserPermissions(guild.getIdLong(), transmitterRole.getIdLong(), receiver.getIdLong());
                }
                break;
            default:
                event.sendUsage(this, event);
                break;
        }
    }

    private String retrieveMode(String arg) {
        if (arg.matches("<@" + "\\d+" + ">")) return "user";
        else if (arg.matches("<@&" + "\\d+" + ">")) return "role";
        else if (arg.matches("\\d+")) return "id";
        else if (arg.matches("everyone")) return "everyone";
        else if (arg.length() > 0) return "name";
        else return "default";
    }
}
