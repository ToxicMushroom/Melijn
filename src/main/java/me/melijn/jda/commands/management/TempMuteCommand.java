package me.melijn.jda.commands.management;

import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.util.function.Consumer;

import static me.melijn.jda.Melijn.PREFIX;

public class TempMuteCommand extends Command {

    public TempMuteCommand() {
        this.commandName = "tempmute";
        this.description = "Temporally mutes a member from your server and sends a message with information about the mute to that member";
        this.usage = PREFIX + commandName + " <member> <time> [reason]";
        this.extra = "Time examples: [1s = 1second, 1m = 1minute, 1h = 1hour, 1w = 1week, 1M = 1month, 1y = 1year]";
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD, Need.ROLE};
        this.permissions = new Permission[]{
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MANAGE_ROLES,
                Permission.MESSAGE_HISTORY
        };
        this.id = 50;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length > 1) {
                User target = Helpers.getUserByArgsN(event, args[0]);
                if (target == null || guild.getMember(target) == null) {
                    event.reply("Unknown " + (target == null ? "user" : "member"));
                    return;
                }
                if (MessageHelper.isRightFormat(args[1])) {
                    Role muteRole = guild.getRoleById(SetMuteRoleCommand.muteRoleCache.getUnchecked(guild.getIdLong()));
                    if (muteRole == null) {
                        event.reply("**No mute role set!**\nCreating Role..");
                        createMuteRole(guild, role -> {
                            event.reply("Role created. You can change the settings of the role to your desires in the role managment tab.\nThis role wil be added to the muted members so it shouldn't have talk permissions!");
                            doTempMute(event, role, target, args);
                        });
                    } else {
                        doTempMute(event, muteRole, target, args);
                    }
                } else {
                    event.reply("`" + args[1] + "` is not the right format.\n**Format:** (number)(*timeunit*) *timeunit* = s, m, h, d, M or y\n**Example:** 1__m__ (1 __minute__)");
                }
            } else {
                MessageHelper.sendUsage(this, event);
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private void doTempMute(CommandEvent event, Role muteRole, User target, String[] args) {
        if (Helpers.canNotInteract(event, muteRole)) return;
        Guild guild = muteRole.getGuild();
        guild.getController().addSingleRoleToMember(guild.getMember(target), muteRole).queue(s -> {
            String reason = event.getArgs().replaceFirst(args[0] + "\\s+" + args[1] + "\\s+|" + args[0] + "\\s+" + args[1], "");
            if (reason.length() <= 1000 && Melijn.mySQL.setTempMute(event.getAuthor(), target, guild, reason, MessageHelper.easyFormatToSeconds(args[1]))) {
                event.getMessage().addReaction("\u2705").queue();
            } else {
                event.getMessage().addReaction("\u274C").queue();
            }
        });
    }

    static void createMuteRole(Guild guild, Consumer<Role> role) {
        guild.getController().createRole()
                .setColor(Color.gray)
                .setMentionable(false)
                .setName("muted")
                .setPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY, Permission.VOICE_CONNECT).queue(newRole -> {
            role.accept(newRole);
            Melijn.mySQL.setRole(guild.getIdLong(), newRole.getIdLong(), RoleType.MUTE);
            SetMuteRoleCommand.muteRoleCache.put(guild.getIdLong(), newRole.getIdLong());
        });
    }
}
