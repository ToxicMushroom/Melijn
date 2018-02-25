package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import net.dv8tion.jda.core.entities.Role;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetMuteRoleCommand extends Command {

    public SetMuteRoleCommand() {
        this.commandName = "setmuterole";
        this.description = "Set the role that will be added to the user when he/she gets muted";
        this.usage = PREFIX + commandName + " <@role | roleId>";
        this.aliases = new String[] {"smr"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.getGuild() != null) {
            if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
                String[] args = event.getArgs().split("\\s+");
                String role = PixelSniper.mySQL.getRoleId(event.getGuild(), RoleType.MUTE);
                if (args.length == 0 || args[0].equalsIgnoreCase("")) {
                    if (event.getGuild().getRoleById(role) != null) event.reply(event.getGuild().getRoleById(role).getName());
                    else event.reply("null");
                } else {
                    Role mutedRole = null;
                    if (args[0].matches("\\d+")) mutedRole = event.getGuild().getRoleById(args[0]);
                    else if (event.getMessage().getMentionedRoles().size() > 0) mutedRole = event.getMessage().getMentionedRoles().get(0);

                    if (mutedRole != null) {
                        if (PixelSniper.mySQL.setRole(event.getGuild(), args[0], RoleType.MUTE)) {
                            event.reply("MuteRoleId changed from " + role + " to " + PixelSniper.mySQL.getRoleId(event.getGuild(), RoleType.MUTE));
                        } else {
                            event.reply("Failed to set role.");
                        }
                    } else {
                        event.reply(usage.replaceFirst(">", PixelSniper.mySQL.getPrefix(event.getGuild().getId())));
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
