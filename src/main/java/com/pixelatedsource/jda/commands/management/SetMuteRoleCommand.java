package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import net.dv8tion.jda.core.entities.Guild;

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
                long roleId = muteRoles.getOrDefault(guild.getIdLong(), -1L);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (roleId != -1 && guild.getRoleById(roleId) != null) event.reply("Current MuteRole: **@" + guild.getRoleById(roleId).getName() + "**");
                    else event.reply("MuteRole is unset");
                } else {
                    long muteRoleId;
                    if (args[0].matches("\\d+") && guild.getRoleById(args[0]) != null) muteRoleId = Long.parseLong(args[0]);
                    else if (event.getMessage().getMentionedRoles().size() > 0) muteRoleId = event.getMessage().getMentionedRoles().get(0).getIdLong();
                    else muteRoleId = -1L;
                    new Thread(() -> {
                        PixelSniper.mySQL.setRole(guild.getIdLong(), muteRoleId, RoleType.MUTE);
                        if (muteRoles.replace(guild.getIdLong(), muteRoleId) == null)
                            muteRoles.put(guild.getIdLong(), muteRoleId);
                    }).start();

                    String oldRoleName = roleId == -1 && guild.getRoleById(roleId) == null ? "nothing" : "@" + guild.getRoleById(roleId).getName();
                    String newRoleName = muteRoleId == -1 ? "nothing" : "@" + guild.getRoleById(muteRoleId).getName();
                    event.reply("JoinRole changed from **" + oldRoleName + "** to **" + newRoleName + "** by **" + event.getFullAuthorName() + "**");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
