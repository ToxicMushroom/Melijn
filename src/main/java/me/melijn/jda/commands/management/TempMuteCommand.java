package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;

public class TempMuteCommand extends Command {

    public TempMuteCommand() {
        this.commandName = "tempmute";
        this.description = "Mute people and let the bot unmute them after the specified amount of time";
        this.usage = Melijn.PREFIX + commandName + " <member> <time> [reason]";
        this.extra = "Time examples: [1s = 1second, 1m = 1minute, 1h = 1hour, 1w = 1week, 1M = 1month, 1y = 1year]";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD, Need.ROLE};
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MANAGE_ROLES,
                Permission.MESSAGE_HISTORY
        };
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length > 1) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                String time = args[1];

                if (target != null && guild.getMember(target) != null) {
                    if (MessageHelper.isRightFormat(time)) {
                        Role muteRole = guild.getRoleById(SetMuteRoleCommand.muteRoleCache.getUnchecked(guild.getIdLong()));
                        if (muteRole == null) {
                            event.reply("**No mute role set!**\nCreating Role..");
                            muteRole = guild.getRoleById(createMuteRole(guild));
                            event.reply("Role created. You can change the settings of the role to your desires in the role managment tab.\nThis role wil be added to the muted members so it shouldn't have talk permissions!");
                        }
                        if (muteRole != null) {
                            if (Helpers.canNotInteract(event, muteRole)) return;
                            guild.getController().addSingleRoleToMember(guild.getMember(target), muteRole).queue(s -> {
                                String reason = event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "\\s+|" + args[0] + "\\s+" + args[1], "");
                                if (reason.length() <= 1000 && Melijn.mySQL.setTempMute(event.getAuthor(), target, guild, reason, MessageHelper.easyFormatToSeconds(time))) {
                                    event.getMessage().addReaction("\u2705").queue();
                                } else {
                                    event.getMessage().addReaction("\u274C").queue();
                                }
                            });
                        } else {
                            event.reply("Error: contact support -> err: 'TempMuteCommand <-> muterole is null'");
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
    }

    static long createMuteRole(Guild guild) {
        long roleId = guild.getController().createRole()
                .setColor(Color.gray)
                .setMentionable(false)
                .setName("muted")
                .setPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT).complete().getIdLong();
        TaskScheduler.async(() -> {
            Melijn.mySQL.setRole(guild.getIdLong(), roleId, RoleType.MUTE);
            SetMuteRoleCommand.muteRoleCache.put(guild.getIdLong(), roleId);
        });
        return roleId;
    }
}
