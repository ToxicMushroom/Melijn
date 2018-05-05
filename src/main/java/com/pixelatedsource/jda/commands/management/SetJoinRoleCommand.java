package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import net.dv8tion.jda.core.entities.Guild;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetJoinRoleCommand extends Command {

    public SetJoinRoleCommand() {
        this.commandName = "setjoinrole";
        this.description = "Setup a role that a user get's when he/she/it joins";
        this.usage = PREFIX + commandName + " <role | null>";
        this.aliases = new String[]{"sjr"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                Guild guild = event.getGuild();
                String[] args = event.getArgs().split("\\s+");
                String role = PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (role != null && role.matches("\\d+") && guild.getRoleById(role) != null)
                        event.reply("Current JoinRole: **@"+ guild.getRoleById(role).getName() + "**");
                    else
                        event.reply("Current JoinRole: **null**");
                } else {
                    String muteRoleId = "null";
                    if (args[0].matches("\\d+") && guild.getRoleById(args[0]) != null)
                        muteRoleId = guild.getRoleById(args[0]).getId();
                    else if (event.getMessage().getMentionedRoles().size() > 0)
                        muteRoleId = event.getMessage().getMentionedRoles().get(0).getId();
                    if (PixelSniper.mySQL.setRole(guild, muteRoleId, RoleType.JOIN)) {
                        String oldRoleName = role == null || role.equalsIgnoreCase("null") ? "null" : "@" + guild.getRoleById(role).getName();
                        String newRoleName = muteRoleId.equalsIgnoreCase("null") ? "null" : "@" + guild.getRoleById(muteRoleId).getName();
                        event.reply("JoinRole changed from **" + oldRoleName + "** to **" + newRoleName + "**");
                    } else {
                        event.reply("Failed to set role.");
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
