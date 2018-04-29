package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.*;

import java.util.ArrayList;
import java.util.List;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;
import static com.pixelatedsource.jda.PixelSniper.mySQL;

public class PermCommand extends Command {

    public PermCommand() {
        this.commandName = "perm";
        this.description = "You can edit the user's acces to your demands ;D";
        this.usage = PREFIX + this.commandName + " <add | remove | clear | copy | info | list> <@role | roleId | @user | userId> [permission]\nA permission is just the name of the command.";
        this.aliases = new String[]{"permission"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            String[] args = event.getArgs().split("\\s+");
            Member member = event.getGuild().getMember(event.getAuthor());
            Guild guild = event.getGuild();
            Message message = event.getMessage();
            JDA jda = event.getJDA();
            List<Role> mentionedRoles = message.getMentionedRoles();
            List<User> mentionedUsers = message.getMentionedUsers();
            if (args.length < 2) {
                MessageHelper.sendUsage(this, event);
                return;
            }
            switch (args[0]) {
                case "add":
                    if (Helpers.hasPerm(member, this.commandName + ".add", 1)) {
                        if (args.length == 3) {
                            if (Helpers.perms.contains(args[2])) {
                                String mode = "default";
                                if (args[1].matches("<@" + "\\d+" + ">")) mode = "user";
                                if (args[1].matches("<@&" + "\\d+" + ">")) mode = "role";
                                if (args[1].matches("\\d+")) mode = "id";
                                if (args[1].matches("everyone")) mode = "everyone";

                                switch (mode) {
                                    case "user":
                                        if (mentionedUsers.size() == 1) {
                                            User target = mentionedUsers.get(0);
                                            mySQL.addUserPermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        }
                                        break;
                                    case "role":
                                        if (mentionedRoles.size() == 1) {
                                            Role target = mentionedRoles.get(0);
                                            mySQL.addRolePermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `@" + target.getName() + "`");
                                        }
                                        break;
                                    case "id":
                                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                            MessageHelper.sendUsage(this, event);
                                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                            User target = jda.getUserById(args[1]);
                                            mySQL.addUserPermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                            Role target =  guild.getRoleById(args[1]);
                                            mySQL.addRolePermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` added to `@" + target.getName() + "`");
                                        } else {
                                            event.reply("NANI!?");
                                            return;
                                        }
                                        break;
                                    case "everyone":
                                        mySQL.addRolePermission(guild, guild.getRoleById(guild.getId()), args[2]);
                                        event.reply("Permission: `" + args[2] + "` added to `@everyone`");
                                        break;
                                    default:
                                        MessageHelper.sendUsage(this, event);
                                        break;
                                }
                            } else {
                                event.reply(">perm list".replaceFirst(">", mySQL.getPrefix(event.getGuild())));
                            }
                        } else {
                            MessageHelper.sendUsage(this, event);
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".add`.");
                    }
                    break;
                case "remove":
                    if (Helpers.hasPerm(member, this.commandName + ".remove", 1)) {
                        if (args.length == 3) {
                            if (Helpers.perms.contains(args[2])) {
                                String mode = "default";
                                if (args[1].matches("<@" + "\\d+" + ">")) mode = "user";
                                if (args[1].matches("<@&" + "\\d+" + ">")) mode = "role";
                                if (args[1].matches("\\d+")) mode = "id";
                                if (args[1].matches("everyone")) mode = "everyone";

                                switch (mode) {
                                    case "user":
                                        if (mentionedUsers.size() == 1) {
                                            User target = mentionedUsers.get(0);
                                            mySQL.removeUserPermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        }
                                        break;
                                    case "role":
                                        if (mentionedRoles.size() == 1) {
                                            Role target = mentionedRoles.get(0);
                                            mySQL.removeRolePermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `@" + target.getName() + "`");
                                        }
                                        break;
                                    case "id":
                                        if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                            MessageHelper.sendUsage(this, event);
                                        } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                            User target = jda.getUserById(args[1]);
                                            mySQL.removeUserPermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `" + target.getName() + "#" + target.getDiscriminator() + "`");
                                        } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                            Role target =  guild.getRoleById(args[1]);
                                            mySQL.removeRolePermission(guild, target, args[2]);
                                            event.reply("Permission: `" + args[2] + "` removed from `@" + target.getName() + "`");
                                        } else {
                                            event.reply("NANI!?");
                                            return;
                                        }
                                        break;
                                    case "everyone":
                                        mySQL.removeRolePermission(guild, guild.getRoleById(guild.getId()), args[2]);
                                        event.reply("Permission: `" + args[2] + "` removed from `@everyone`");
                                        break;
                                    default:
                                        MessageHelper.sendUsage(this, event);
                                        break;
                                }
                            } else {
                                event.reply(">perm list".replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild())));
                            }
                        } else {
                            MessageHelper.sendUsage(this, event);
                            return;
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".remove`.");
                        return;
                    }
                    break;
                case "clear":
                    if (Helpers.hasPerm(member, this.commandName + ".clear", 1)) {
                        if (args.length == 2) {
                            String mode = "default";
                            if (args[1].matches("<@" + "\\d+" + ">")) mode = "user";
                            if (args[1].matches("<@&" + "\\d+" + ">")) mode = "role";
                            if (args[1].matches("\\d+")) mode = "id";
                            if (args[1].matches("everyone")) mode = "everyone";

                            switch (mode) {
                                case "user":
                                    if (mentionedUsers.size() == 1) {
                                        User target = mentionedUsers.get(0);
                                        mySQL.clearUserPermissions(guild, target);
                                        event.reply("Permissions off `" + target.getName() + "#" + target.getDiscriminator() + "` have been cleared");
                                    }
                                    break;
                                case "role":
                                    if (mentionedRoles.size() == 1) {
                                        Role target = mentionedRoles.get(0);
                                        mySQL.clearRolePermissions(guild, target);
                                        event.reply("Permissions off `@" + target.getName() + "` have been cleared");
                                    }
                                    break;
                                case "id":
                                    if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                        MessageHelper.sendUsage(this, event);
                                    } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                        User target = jda.getUserById(args[1]);
                                        mySQL.clearUserPermissions(guild, target);
                                        event.reply("Permissions off `" + target.getName() + "#" + target.getDiscriminator() + "` have been cleared");
                                    } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                        Role target =  guild.getRoleById(args[1]);
                                        mySQL.clearRolePermissions(guild, target);
                                        event.reply("Permissions off `@" + target.getName() + "` have been cleared");
                                    } else {
                                        event.reply("NANI!?");
                                        return;
                                    }
                                    break;
                                case "everyone":
                                    mySQL.clearRolePermissions(guild, guild.getRoleById(guild.getId()));
                                    event.reply("Permissions off `@everyone` have been cleared");
                                    break;
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    break;
                            }
                        } else {
                            MessageHelper.sendUsage(this, event);
                            return;
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
                            String targetName = "default";
                            String mode = "default";
                            if (args[1].matches("<@" + "\\d+" + ">")) mode = "user";
                            if (args[1].matches("<@&" + "\\d+" + ">")) mode = "role";
                            if (args[1].matches("\\d+")) mode = "id";
                            if (args[1].matches("everyone")) mode = "everyone";

                            switch (mode) {
                                case "user":
                                    if (mentionedUsers.size() == 1) {
                                        lijst = mySQL.getUserPermissions(guild, mentionedUsers.get(0));
                                        targetName = mentionedUsers.get(0).getName() + "#" + mentionedUsers.get(0).getDiscriminator();
                                    }
                                    break;
                                case "role":
                                    if (mentionedRoles.size() == 1) {
                                        lijst = mySQL.getRolePermissions(guild, mentionedRoles.get(0));
                                        targetName = "@" + mentionedRoles.get(0).getName();

                                    }
                                    break;
                                case "id":
                                    if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                        MessageHelper.sendUsage(this, event);
                                    } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                        User target = jda.getUserById(args[1]);
                                        lijst = mySQL.getUserPermissions(guild, target);
                                        targetName = target.getName() + "#" + target.getDiscriminator();
                                    } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                        lijst = mySQL.getRolePermissions(guild, guild.getRoleById(args[1]));
                                        targetName = "@" + mentionedRoles.get(0).getName();
                                    } else {
                                        event.reply("NANI!?");
                                        return;
                                    }
                                    break;
                                case "everyone":
                                    lijst = mySQL.getRolePermissions(guild, guild.getRoleById(guild.getId()));
                                    targetName = "@everyone";
                                    break;
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    return;
                            }
                            int partNumber = 0;
                            StringBuilder sb = new StringBuilder();
                            for (String s : lijst) {
                                sb.append(s);
                                if (sb.toString().length() > 1900) {
                                    event.reply(new EmbedBuilder()
                                            .setTitle("Permissions off `" + targetName + "` part #" + partNumber++)
                                            .setColor(Helpers.EmbedColor)
                                            .setDescription(sb.toString())
                                            .build());
                                    sb = new StringBuilder();
                                }
                            }
                            String title = partNumber == 0 ? "Permissions off `" + targetName + "`" : "Permissions off `" + targetName + "` part #" + partNumber;
                            event.reply(new EmbedBuilder()
                                    .setTitle(title)
                                    .setColor(Helpers.EmbedColor)
                                    .setDescription(sb.toString())
                                    .build());
                        } else {
                            MessageHelper.sendUsage(this, event);
                            return;
                        }
                    } else {
                        event.reply(Helpers.noPerms + "`" + this.commandName + ".view`.");
                        return;
                    }
                    break;
                case "copy":
                    if (Helpers.hasPerm(member, this.commandName + ".copy", 1)) {
                        if (args.length == 3) {
                            String transmitterMode = "default";
                            String receiverMode = "default";
                            User transmitter = null;
                            User receiver = null;
                            Role transmitterRole = null;
                            Role receiverRole = null ;
                            if (args[1].matches("<@" + "\\d+" + ">")) transmitterMode = "user";
                            if (args[1].matches("<@&" + "\\d+" + ">")) transmitterMode = "role";
                            if (args[1].matches("\\d+")) transmitterMode = "id";
                            if (args[1].matches("everyone")) transmitterMode = "everyone";
                            if (args[2].matches("<@" + "\\d+" + ">")) receiverMode = "user";
                            if (args[2].matches("<@&" + "\\d+" + ">")) receiverMode = "role";
                            if (args[2].matches("\\d+")) receiverMode = "id";
                            if (args[2].matches("everyone")) receiverMode = "everyone";

                            switch (transmitterMode) {
                                case "user":
                                    if (mentionedUsers.size() == 1) {
                                        transmitter = mentionedUsers.get(0);
                                    }
                                    break;
                                case "role":
                                    if (mentionedRoles.size() == 1) {
                                        transmitterRole = mentionedRoles.get(0);
                                    }
                                    break;
                                case "id":
                                    if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                        MessageHelper.sendUsage(this, event);
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
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    return;
                            }

                            switch (receiverMode) {
                                case "user":
                                    if (mentionedUsers.size() == 1) {
                                        receiver = mentionedUsers.get(0);
                                    }
                                    break;
                                case "role":
                                    if (mentionedRoles.size() == 1) {
                                        receiverRole = mentionedRoles.get(0);
                                    }
                                    break;
                                case "id":
                                    if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) == null) {
                                        MessageHelper.sendUsage(this, event);
                                        return;
                                    } else if (guild.getRoleById(args[1]) == null && guild.getMemberById(args[1]) != null) {
                                        receiver = jda.getUserById(args[1]);
                                    } else if (guild.getRoleById(args[1]) != null && guild.getMemberById(args[1]) == null) {
                                        receiverRole = guild.getRoleById(args[1]);
                                    } else {
                                        event.reply("NANI!?");
                                        return;
                                    }
                                    break;
                                case "everyone":
                                    receiverRole = guild.getRoleById(guild.getId());
                                    break;
                                default:
                                    MessageHelper.sendUsage(this, event);
                                    return;
                            }

                            if (transmitter != null && receiver != null) {
                                mySQL.copyUserPermissions(guild, transmitter, receiver);
                                event.reply("Copied all permissions from `" + transmitter.getName() + "#" + transmitter.getDiscriminator() + "` to `" + receiver.getName() + "#" + receiver.getDiscriminator() + "`");
                            } else if (transmitter != null && receiverRole != null) {
                                mySQL.copyUserRolePermissions(transmitter, receiverRole);
                            } else if (transmitterRole != null && receiverRole != null) {
                                mySQL.copyRolePermissions(transmitterRole, receiverRole);
                            } else if (transmitterRole != null && receiver != null) {
                                mySQL.copyRoleUserPermissions(transmitterRole, receiver);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
