package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetJoinRoleCommand extends Command {

    public SetJoinRoleCommand() {
        this.commandName = "setJoinRole";
        this.description = "Setup a role that a user get's on join";
        this.usage = PREFIX + commandName + " [role | null]";
        this.aliases = new String[]{"sjr"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> joinRoles = PixelSniper.mySQL.getRoleMap(RoleType.JOIN);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                long role = joinRoles.getOrDefault(guild.getId(), -1L);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (role != -1 && guild.getRoleById(role) != null) event.reply("Current JoinRole: **@" + guild.getRoleById(role).getName() + "**");
                    else event.reply("Current JoinRole is unset");
                } else {
                    if (args[0].equalsIgnoreCase("null")) {
                        joinRoles.remove(guild.getIdLong());
                        new Thread(() -> PixelSniper.mySQL.removeRole(guild.getIdLong(), RoleType.JOIN)).start();
                        event.reply("JoinRole has been unset by **" + event.getFullAuthorName() + "**");
                    } else {
                        Role joinRole = Helpers.getRoleByArgs(event, args[0]);
                        if (joinRole != null) {
                            if (joinRole.getIdLong() != guild.getIdLong()) {
                                if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).getPosition() > joinRole.getPosition()) {
                                    if (joinRoles.replace(guild.getIdLong(), joinRole.getIdLong()) == null)
                                        joinRoles.put(guild.getIdLong(), joinRole.getIdLong());
                                    new Thread(() -> PixelSniper.mySQL.setRole(guild.getIdLong(), joinRole.getIdLong(), RoleType.JOIN)).start();
                                    event.reply("JoinRole changed to **@" + joinRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                                } else {
                                    event.reply("The JoinRole hasn't been changed due: **@" + joinRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
                                }
                            } else {
                                event.reply("The @everyone role cannot be the JoinRole because everyone has it");
                            }
                        } else {
                            MessageHelper.sendUsage(this, event);
                        }
                    }
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
