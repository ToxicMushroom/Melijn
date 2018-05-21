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
                    User target = null;
                    String time = args[1];
                    String reason = event.getArgs().replaceFirst(args[0], "").replaceFirst(" " + args[1] + " ", "");
                    if (event.getMessage().getMentionedUsers().size() > 0) target = event.getMessage().getMentionedUsers().get(0);
                    else if (args[0].matches("\\d+")) target = event.getJDA().retrieveUserById(args[0]).complete();
                    if (target == null || guild.getMember(target) == null) {
                        event.reply("Unknown member!");
                        return;
                    }
                    if (MessageHelper.isRightFormat(time)) {
                        if (PixelSniper.mySQL.getRoleId(guild.getIdLong(), RoleType.MUTE) == -1) {
                            event.reply("**No mute role set!**\nCreating Role..");
                            if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES)) {
                                PixelSniper.mySQL.setRole(guild.getIdLong(),
                                guild.getController().createRole()
                                                .setColor(Color.gray)
                                                .setMentionable(false)
                                                .setName("muted")
                                                .setPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT).complete().getIdLong(), RoleType.MUTE);
                                event.reply("Role created. You can change the settings of the role to your desires in the role managment tab.\nThis role wil be added to the muted users so it should have no talk permissions!");
                            } else {
                                event.reply("No permission to create roles.\n" + "You can create a role yourself with the permissions you desire and set it with " + SetPrefixCommand.prefixes.getOrDefault(guild.getIdLong(), ">") + "setmuterole <@role | roleId>\nOr give the bot role managment permissions.");
                                return;
                            }
                        }
                        if (PixelSniper.mySQL.setTempMute(event.getAuthor(), target, guild, reason, MessageHelper.easyFormatToSeconds(time))) {
                            guild.getController().addSingleRoleToMember(guild.getMember(target),
                            guild.getRoleById(PixelSniper.mySQL.getRoleId(guild.getIdLong(), RoleType.MUTE))).queue();
                            event.getMessage().addReaction("\u2705").queue();
                        } else {
                            event.getMessage().addReaction("\u274C").queue();
                        }
                    } else {
                        event.reply("`" + time + "` is not the right format.\n**Format:** (number)(*timeunit*) *timeunit* = s, m, h, d, M or y\n**Example:** 1__m__ (1 __minute__)");
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
}
