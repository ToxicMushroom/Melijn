package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class TempMuteCommand extends Command {

    public TempMuteCommand() {
        this.commandName = "tempmute";
        this.description = "Mute people and let the bot unmute them after the specified amount of time";
        this.usage = PREFIX + commandName + " <@user | userid> <time> <reason>\nTime examples: [1d = 1 day, 1s = 1second, 1m = 1minute, 1M = 1month]";
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                Guild guild = event.getGuild();
                if (args.length >= 3) {
                    User target = Helpers.getUserByArgsN(event, args[0]);
                    String time = args[1];
                    String reason = event.getArgs().replaceFirst(args[0], "").replaceFirst(" " + args[1] + " ", "");
                    if (target != null && guild.getMember(target) != null) {
                        if (MessageHelper.isRightFormat(time)) {
                            if (SetMuteRoleCommand.muteRoles.getOrDefault(guild.getIdLong(), -1L) == -1) {
                                event.reply("**No mute role set!**\nCreating Role..");
                                if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                                    createMuteRole(guild);
                                    event.reply("Role created. You can change the settings of the role to your desires in the role managment tab.\nThis role wil be added to the muted users so it should have no talk permissions!");
                                } else {
                                    event.reply("No permission to create roles.\n" + "You can create a role yourself with the permissions you desire and set it with " + SetPrefixCommand.prefixes.getOrDefault(guild.getIdLong(), ">") + "setmuterole <@role | roleId>\nOr give the bot role managment permissions.");
                                    return;
                                }
                            }
                            Role muteRole = guild.getRoleById(SetMuteRoleCommand.muteRoles.getOrDefault(guild.getIdLong(), -1L));
                            if (muteRole != null) {
                                new Thread(() -> guild.getController().addSingleRoleToMember(guild.getMember(target), muteRole).queue(s -> {
                                    if (PixelSniper.mySQL.setTempMute(event.getAuthor(), target, guild, reason, MessageHelper.easyFormatToSeconds(time))) {
                                        event.getMessage().addReaction("\u2705").queue();
                                    } else {
                                        event.getMessage().addReaction("\u274C").queue();
                                    }
                                })).start();
                            } else {
                                event.reply("Mute role is unset (cannot mute)");
                            }
                        } else {
                            event.reply("`" + time + "` is not the right format.\n**Format:** (number)(*timeunit*) *timeunit* = s, m, h, d, M or y\n**Example:** 1__m__ (1 __minute__)");
                        }
                    } else {
                        event.reply("Unknown " + (target == null ? "user" : "member"));
                    }
                } else {
                    MessageHelper.sendUsage(this, event);
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }

    private void createMuteRole(Guild guild) {
        long roleId = guild.getController().createRole()
                .setColor(Color.gray)
                .setMentionable(false)
                .setName("muted")
                .setPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT).complete().getIdLong();
        new Thread(() -> PixelSniper.mySQL.setRole(guild.getIdLong(), roleId, RoleType.MUTE)).start();
        SetMuteRoleCommand.muteRoles.putIfAbsent(guild.getIdLong(), roleId);
    }
}
