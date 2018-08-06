package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.*;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetUnverifiedRole extends Command {

    public SetUnverifiedRole() {
        this.commandName = "setUnverifiedRole";
        this.usage = PREFIX + commandName + " [role | null]";
        this.description = "Set's an unverified role that unverified members will get on join";
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"sur"};
    }

    public static HashMap<Long, Long> unverifiedRoles = PixelSniper.mySQL.getRoleMap(RoleType.UNVERIFIED);

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            long role = unverifiedRoles.getOrDefault(guild.getIdLong(), -1L);
            if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                if (role != -1 && guild.getRoleById(role) != null)
                    event.reply("Current UnverifiedRole: **@" + guild.getRoleById(role).getName() + "**");
                else event.reply("Current UnverifiedRole is unset");
            } else {
                if (args[0].equalsIgnoreCase("null")) {
                    unverifiedRoles.remove(guild.getIdLong());
                    new Thread(() -> PixelSniper.mySQL.removeRole(guild.getIdLong(), RoleType.UNVERIFIED)).start();
                    event.reply("UnverifiedRole has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    Role unverifiedRole = Helpers.getRoleByArgs(event, args[0]);
                    if (unverifiedRole != null) {
                        if (unverifiedRole.getIdLong() != guild.getIdLong()) {
                            if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).canInteract(unverifiedRole)) {
                                if (unverifiedRoles.replace(guild.getIdLong(), unverifiedRole.getIdLong()) == null)
                                    unverifiedRoles.put(guild.getIdLong(), unverifiedRole.getIdLong());
                                new Thread(() -> PixelSniper.mySQL.setRole(guild.getIdLong(), unverifiedRole.getIdLong(), RoleType.UNVERIFIED)).start();
                                event.reply("UnverifiedRole changed to **@" + unverifiedRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                            }
                            else {
                                event.reply("The UnverifiedRole hasn't been changed due: **@" + unverifiedRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
                            }
                        } else {
                            event.reply("The @everyone role cannot be as the UnverifiedRole because everyone has it");
                        }
                    } else {
                        MessageHelper.sendUsage(this, event);
                    }
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
