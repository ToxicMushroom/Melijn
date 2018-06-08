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

public class SetMuteRoleCommand extends Command {

    public SetMuteRoleCommand() {
        this.commandName = "setmuterole";
        this.description = "Set the role that will be added to the user when he/she gets muted";
        this.usage = PREFIX + commandName + " <@role | roleId>";
        this.aliases = new String[]{"smr"};
        this.category = Category.MANAGEMENT;
    }

    public static HashMap<Long, Long> muteRoles = PixelSniper.mySQL.getRoleMap(RoleType.MUTE);

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                long role = muteRoles.getOrDefault(guild.getId(), -1L);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (role != -1 && guild.getRoleById(role) != null) event.reply("Current MuteRole: **@" + guild.getRoleById(role).getName() + "**");
                    else event.reply("Current MuteRole is unset");
                } else {
                    if (args[0].equalsIgnoreCase("null")) {
                        muteRoles.remove(guild.getIdLong());
                        new Thread(() -> PixelSniper.mySQL.removeRole(guild.getIdLong(), RoleType.JOIN)).start();
                        event.reply("MuteRole has been unset by **" + event.getFullAuthorName() + "**");
                    } else {
                        Role muteRole = Helpers.getRoleByArgs(event, args[0]);
                        if (muteRole != null) {
                            if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).getPosition() > muteRole.getPosition()) {
                                if (muteRoles.replace(guild.getIdLong(), muteRole.getIdLong()) == null)
                                    muteRoles.put(guild.getIdLong(), muteRole.getIdLong());
                                new Thread(() -> PixelSniper.mySQL.setRole(guild.getIdLong(), muteRole.getIdLong(), RoleType.MUTE)).start();
                                event.reply("MuteRole changed to **@" + muteRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                            } else {
                                event.reply("The MuteRole hasn't been changed due: **@" + muteRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
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
