package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class MuteCommand extends Command {

    public MuteCommand() {
        this.commandName = "mute";
        this.description = "Mute user on your server and give them a nice message in pm.";
        this.usage = PREFIX + commandName + " <@user | userId> <reason>";
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"permmute"};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                if (args.length >= 2) {
                    User target;
                    String reason = event.getArgs().replaceFirst(args[0] + "\\s+", "");
                    if (event.getMessage().getMentionedUsers().size() > 0) target = event.getMessage().getMentionedUsers().get(0);
                    else target = event.getJDA().getUserById(args[0]);
                    if (target == null || event.getGuild().getMember(target) == null) {
                        event.reply("Unknown member! ");
                        return;
                    }
                    if (PixelSniper.mySQL.getRoleId(event.getGuild(), RoleType.MUTE) == null) {
                        event.reply("**No mute role set!**\nCreating Role..");
                        if (event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                            PixelSniper.mySQL.setRole(event.getGuild(), event.getGuild().getController().createRole().setColor(Color.gray).setMentionable(false).setName("muted").setPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT).complete().getId(), RoleType.MUTE);
                            event.reply("Role created. You can change the settings of the role to your desires in the role managment tab.\nThis role wil be added to the muted users so it should have no talk permissions!");
                        } else {
                            event.reply("No permission to create roles.\n" + "You can create a role yourself with the permissions you desire and set it with " + PixelSniper.mySQL.getPrefix(event.getGuild().getId()) + "setmuterole <@role | roleId>\nOr give the bot role managment permissions.");
                            return;
                        }
                    }
                    if (PixelSniper.mySQL.setPermMute(event.getAuthor(), target, event.getGuild(), reason)) {
                        event.getGuild().getController().addSingleRoleToMember(event.getGuild().getMember(target), event.getGuild().getRoleById(PixelSniper.mySQL.getRoleId(event.getGuild(), RoleType.MUTE))).queue();
                        event.getMessage().addReaction("\u2705").queue();
                    } else {
                        event.getMessage().addReaction("\u274C").queue();
                    }
                } else {
                    event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
