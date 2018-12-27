package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;
import java.util.List;

import static me.melijn.jda.Melijn.PREFIX;

public class PermCommand extends Command {

    public PermCommand() {
        this.commandName = "perm";
        this.description = "Main command to manage access to commands for specific users/roles";
        this.usage = PREFIX + commandName + " <add | remove | view | clear | copy | list>";
        this.extra = "A permission is just the name of the command and you'll get more info for each sub section of the command if you just use it wrong or without more arguments";
        this.aliases = new String[]{"permission"};
        this.category = Category.MANAGEMENT;
        this.id = 71;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            String prefix = SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong());
            String[] args = event.getArgs().split("\\s+");
            Member member = event.getGuild().getMember(event.getAuthor());
            Guild guild = event.getGuild();
            Message message = event.getMessage();
            JDA jda = event.getJDA();
            List<Role> mentionedRoles = message.getMentionedRoles();
            List<User> mentionedUsers = message.getMentionedUsers();
            switch (args[0]) {
                case "add":
                    if (Helpers.hasPerm(member, this.commandName + ".add", 1)) {
                        if (args.length == 3) {
                            if (Helpers.perms.contains(args[2])) {
                                String mode = retrieveMode(args[1]);

                                switch (mode) {
                                    case "user":
                                        if (mentionedUsers.size() == 1) {
                                            User target = mentionedUsers.get(0);
                                            Melijn.mySQL.addUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        }
                                        break;
                                    case "role":
                                        if (mentionedRoles.size() == 1) {
                                            Role target = mentionedRoles.get(0);
                                            Melijn.mySQL.addRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `@" + target.getName() + "`");
                                        }
                                        break;
                                    case "id":
                                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                            event.reply(prefix + commandName + " add <role | user | everyone> <permission>");
                                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                            User target = jda.getUserById(args[1]);
                                            Melijn.mySQL.addUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                            Role target = guild.getRoleById(args[1]);
                                            Melijn.mySQL.addRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `@" + target.getName() + "`");
                                        } else {
                                            event.reply("NANI!?");
                                            return;
                                        }
                                        break;
                                    case "everyone":
                                        Melijn.mySQL.addRolePermission(guild.getIdLong(), guild.getIdLong(), args[2]);
                                        event.reply("Permission: `" + args[2] + "` added to `@everyone`");
                                        break;
                                    case "name":
                                        if (guild.getRolesByName(args[1], true).size() > 0) {
                                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                                Melijn.mySQL.addRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` added to `@%s`", args[1]));
                                            } else {
                                                Melijn.mySQL.addRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` added to `@%s`", guild.getRolesByName(args[1], true).get(0).getName()));
                                            }
                                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                                Melijn.mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByName(args[1], false).get(0).getUser()));
                                            } else {
                                                Melijn.mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByName(args[1], true).get(0).getUser()));
                                            }
                                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                                Melijn.mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` added to `%#s`", guild.getMembersByNickname(args[1], false).get(0).getUser()));
                                            } else {
                                                Melijn.mySQL.addUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong(), args[2]);
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
                            } else {
                                event.reply("Unknown permission\n" + prefix + commandName + " list");
                            }
                        } else {
                            event.reply(prefix + commandName + " add <role | user | everyone> <permission>");
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".add`.");
                    }
                    break;
                case "remove":
                    if (Helpers.hasPerm(member, this.commandName + ".remove", 1)) {
                        if (args.length == 3) {
                            if (Helpers.perms.contains(args[2])) {
                                String mode = retrieveMode(args[1]);

                                switch (mode) {
                                    case "user":
                                        if (mentionedUsers.size() == 1) {
                                            User target = mentionedUsers.get(0);
                                            Melijn.mySQL.removeUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        }
                                        break;
                                    case "role":
                                        if (mentionedRoles.size() == 1) {
                                            Role target = mentionedRoles.get(0);
                                            Melijn.mySQL.removeRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `@" + target.getName() + "`");
                                        }
                                        break;
                                    case "id":
                                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                            event.reply(prefix + commandName + " remove <role | user | everyone> <permission>");
                                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                            User target = jda.getUserById(args[1]);
                                            Melijn.mySQL.removeUserPermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                            Role target = guild.getRoleById(args[1]);
                                            Melijn.mySQL.removeRolePermission(guild.getIdLong(), target.getIdLong(), args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `@" + target.getName() + "`");
                                        } else {
                                            event.reply("NANI!?");
                                            return;
                                        }
                                        break;
                                    case "everyone":
                                        Melijn.mySQL.removeRolePermission(guild.getIdLong(), guild.getIdLong(), args[2]);
                                        event.reply("Permission: `" + args[2] + "` removed from `@everyone`");
                                        break;
                                    case "name":
                                        if (guild.getRolesByName(args[1], true).size() > 0) {
                                            if (guild.getRolesByName(args[1], false).size() > 0) {
                                                Melijn.mySQL.removeRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` removed from `@%s`", args[1]));
                                            } else {
                                                Melijn.mySQL.removeRolePermission(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` removed from `@%s`", guild.getRolesByName(args[1], true).get(0).getName()));
                                            }
                                        } else if (guild.getMembersByName(args[1], true).size() > 0) {
                                            if (guild.getMembersByName(args[1], false).size() > 0) {
                                                Melijn.mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByName(args[1], false).get(0).getUser()));
                                            } else {
                                                Melijn.mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByName(args[1], true).get(0).getUser()));
                                            }
                                        } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                                            if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                                Melijn.mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong(), args[2]);
                                                event.reply(String.format("Permission: `" + args[2] + "` removed from `%#s`", guild.getMembersByNickname(args[1], false).get(0).getUser()));
                                            } else {
                                                Melijn.mySQL.removeUserPermission(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong(), args[2]);
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
                            } else {
                                event.reply("Unknown permission\n" + prefix + commandName + " list");
                            }
                        } else {
                            event.reply(prefix + commandName + " remove <role | user | everyone> <permission>");
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".remove`.");
                        return;
                    }
                    break;
                case "clear":
                    if (Helpers.hasPerm(member, this.commandName + ".clear", 1)) {
                        if (args.length == 2) {
                            String mode = retrieveMode(args[1]);
                            switch (mode) {
                                case "user":
                                    if (mentionedUsers.size() > 0) {
                                        User target = mentionedUsers.get(0);
                                        Melijn.mySQL.clearUserPermissions(guild.getIdLong(), target.getIdLong());
                                        event.reply("Permissions off `" + target.getName() + "#" + target.getDiscriminator() + "` have been cleared");
                                    }
                                    break;
                                case "role":
                                    if (mentionedRoles.size() > 0) {
                                        Role target = mentionedRoles.get(0);
                                        Melijn.mySQL.clearRolePermissions(guild.getIdLong(), target.getIdLong());
                                        event.reply("Permissions off `@" + target.getName() + "` have been cleared");
                                    }
                                    break;
                                case "id":
                                    if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                        event.reply(prefix + commandName + " clear <role | user | everyone>");
                                    } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                        User target = jda.getUserById(args[1]);
                                        Melijn.mySQL.clearUserPermissions(guild.getIdLong(), target.getIdLong());
                                        event.reply("Permissions off `" + target.getName() + "#" + target.getDiscriminator() + "` have been cleared");
                                    } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                        Role target = guild.getRoleById(args[1]);
                                        Melijn.mySQL.clearRolePermissions(guild.getIdLong(), target.getIdLong());
                                        event.reply("Permissions off `@" + target.getName() + "` have been cleared");
                                    } else {
                                        event.reply("NANI!?");
                                        return;
                                    }
                                    break;
                                case "everyone":
                                    Melijn.mySQL.clearRolePermissions(guild.getIdLong(), guild.getIdLong());
                                    event.reply("Permissions off `@everyone` have been cleared");
                                    break;
                                case "name":
                                    if (guild.getRolesByName(args[1], true).size() > 0) {
                                        if (guild.getRolesByName(args[1], false).size() > 0) {
                                            Melijn.mySQL.clearRolePermissions(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong());
                                            event.reply(String.format("Permissions of `@%s` have been cleared", args[1]));
                                        } else {
                                            Melijn.mySQL.clearRolePermissions(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong());
                                            event.reply(String.format("Permissions of `@%s` have been cleared", guild.getRolesByName(args[1], true).get(0).getName()));
                                        }
                                    } else if (guild.getMembersByName(args[1], true).size() > 0) {
                                        if (guild.getMembersByName(args[1], false).size() > 0) {
                                            Melijn.mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong());
                                            event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByName(args[1], false).get(0).getUser()));
                                        } else {
                                            Melijn.mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong());
                                            event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByName(args[1], true).get(0).getUser()));
                                        }
                                    } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                                        if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                            Melijn.mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong());
                                            event.reply(String.format("Permissions of `%#s` have been cleared", guild.getMembersByNickname(args[1], false).get(0).getUser()));
                                        } else {
                                            Melijn.mySQL.clearUserPermissions(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong());
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
                        } else {
                            event.reply(prefix + commandName + " clear <role | user | everyone>");
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".clear`.");
                        return;
                    }
                    break;
                case "view":
                    if (Helpers.hasPerm(member, this.commandName + ".view", 0)) {
                        if (args.length == 2) {
                            List<String> lijst = new ArrayList<>();
                            String targetName = "error";
                            String mode = retrieveMode(args[1]);
                            switch (mode) {
                                case "user":
                                    if (mentionedUsers.size() > 0) {
                                        lijst = Melijn.mySQL.getUserPermissions(guild.getIdLong(), mentionedUsers.get(0).getIdLong());
                                        targetName = mentionedUsers.get(0).getName() + "#" + mentionedUsers.get(0).getDiscriminator();
                                    }
                                    break;
                                case "role":
                                    if (mentionedRoles.size() > 0) {
                                        lijst = Melijn.mySQL.getRolePermissions(guild.getIdLong(), mentionedRoles.get(0).getIdLong());
                                        targetName = "@" + mentionedRoles.get(0).getName();
                                    }
                                    break;
                                case "id":
                                    if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                        event.reply(prefix + commandName + " view <role | user | everyone>");
                                    } else if (guild.getMemberById(args[1]) != null) {
                                        User target = jda.getUserById(args[1]);
                                        lijst = Melijn.mySQL.getUserPermissions(guild.getIdLong(), target.getIdLong());
                                        targetName = target.getName() + "#" + target.getDiscriminator();
                                    } else if (guild.getRoleById(args[1]) != null) {
                                        Role role = guild.getRoleById(args[1]);
                                        lijst = Melijn.mySQL.getRolePermissions(guild.getIdLong(), role.getIdLong());
                                        targetName = "@" + role.getName();
                                    } else {
                                        event.reply("NANI!?");
                                        return;
                                    }
                                    break;
                                case "everyone":
                                    lijst = Melijn.mySQL.getRolePermissions(guild.getIdLong(), guild.getIdLong());
                                    targetName = "@everyone";
                                    break;
                                case "name":
                                    if (guild.getRolesByName(args[1], true).size() > 0) {
                                        if (guild.getRolesByName(args[1], false).size() > 0) {
                                            lijst = Melijn.mySQL.getRolePermissions(guild.getIdLong(), guild.getRolesByName(args[1], false).get(0).getIdLong());
                                            targetName = String.format("@%s", guild.getRolesByName(args[1], false).get(0));
                                        } else {
                                            lijst = Melijn.mySQL.getRolePermissions(guild.getIdLong(), guild.getRolesByName(args[1], true).get(0).getIdLong());
                                            targetName = String.format("@%s", guild.getRolesByName(args[1], true).get(0));
                                        }
                                    } else if (guild.getMembersByName(args[1], true).size() > 0) {
                                        if (guild.getMembersByName(args[1], false).size() > 0) {
                                            lijst = Melijn.mySQL.getUserPermissions(guild.getIdLong(), guild.getMembersByName(args[1], false).get(0).getUser().getIdLong());
                                            targetName = String.format("%#s", guild.getMembersByName(args[1], false).get(0).getUser());
                                        } else {
                                            lijst = Melijn.mySQL.getUserPermissions(guild.getIdLong(), guild.getMembersByName(args[1], true).get(0).getUser().getIdLong());
                                            targetName = String.format("%#s", guild.getMembersByName(args[1], false).get(0).getUser());
                                        }
                                    } else if (guild.getMembersByNickname(args[1], true).size() > 0) {
                                        if (guild.getMembersByNickname(args[1], false).size() > 0) {
                                            lijst = Melijn.mySQL.getUserPermissions(guild.getIdLong(), guild.getMembersByNickname(args[1], false).get(0).getUser().getIdLong());
                                            targetName = String.format("%#s", guild.getMembersByNickname(args[1], false).get(0).getUser());
                                        } else {
                                            lijst = Melijn.mySQL.getUserPermissions(guild.getIdLong(), guild.getMembersByNickname(args[1], true).get(0).getUser().getIdLong());
                                            targetName = String.format("%#s", guild.getMembersByNickname(args[1], false).get(0).getUser());
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
                                    event.reply(new Embedder(event.getGuild())
                                            .setTitle("Permissions off `" + targetName + "` part #" + partNumber++)
                                            .setDescription(sb.toString())
                                            .build());
                                    sb = new StringBuilder();
                                }
                            }
                            String title = partNumber == 0 ? "Permissions off `" + targetName + "`" : "Permissions off `" + targetName + "` part #" + partNumber;
                            event.reply(new Embedder(event.getGuild())
                                    .setTitle(title)
                                    .setDescription(sb.toString())
                                    .build());
                        } else {
                            event.reply(prefix + commandName + " view <role | user | everyone>");
                            return;
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".view`.");
                        return;
                    }
                    break;
                case "list":
                    StringBuilder sb = new StringBuilder();
                    int i = 1;
                    int count = 0;
                    for (String s : Helpers.perms) {
                        sb.append(++count).append(". [").append(s).append("]").append("\n");
                        if (sb.length() > 1900) {
                            event.getChannel().sendMessage("Permissions list part **#" + i + "**\n```INI\n" + sb.toString() + "```").queue();
                            sb = new StringBuilder();
                            i++;
                        }
                    }
                    if (sb.length() != 0)
                        event.reply("Permissions list part **#" + i + "**\n```INI\n" + sb.toString() + "```");
                    break;
                case "copy":
                    if (Helpers.hasPerm(member, this.commandName + ".copy", 1)) {
                        if (args.length == 3) {
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
                                Melijn.mySQL.copyUserPermissions(guild.getIdLong(), transmitter.getIdLong(), receiver.getIdLong());
                                event.reply("Copied all permissions from `" + transmitter.getName() + "#" + transmitter.getDiscriminator() + "` to `" + receiver.getName() + "#" + receiver.getDiscriminator() + "`");
                            } else if (transmitter != null && receiverRole != null) {
                                Melijn.mySQL.copyUserRolePermissions(guild.getIdLong(), transmitter.getIdLong(), receiverRole.getIdLong());
                            } else if (transmitterRole != null && receiverRole != null) {
                                Melijn.mySQL.copyRolePermissions(guild.getIdLong(), transmitterRole.getIdLong(), receiverRole.getIdLong());
                            } else if (transmitterRole != null && receiver != null) {
                                Melijn.mySQL.copyRoleUserPermissions(guild.getIdLong(), transmitterRole.getIdLong(), receiver.getIdLong());
                            }
                        } else {
                            event.reply(prefix + commandName + " copy <role | user | everyone> <role | user | everyone>");
                        }
                    }
                    break;
                default:
                    MessageHelper.sendUsage(this, event);
                    break;
            }
        } else {
            event.reply(Helpers.guildOnly);
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
