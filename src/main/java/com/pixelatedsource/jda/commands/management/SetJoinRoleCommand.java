package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;

import java.util.HashMap;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class SetJoinRoleCommand extends Command {

    public SetJoinRoleCommand() {
        this.commandName = "setjoinrole";
        this.description = "Setup a role that a user get's when he/she/it joins";
        this.usage = PREFIX + commandName + " <role | null>";
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
                        long joinRoleId;
                        if (args[0].matches("\\d+") && guild.getRoleById(args[0]) != null) joinRoleId = Long.parseLong(args[0]);
                        else if (event.getMessage().getMentionedRoles().size() > 0) joinRoleId = event.getMessage().getMentionedRoles().get(0).getIdLong();
                        else joinRoleId = -1;
                        if (joinRoleId != -1) {
                            if (joinRoles.replace(guild.getIdLong(), joinRoleId) == null) joinRoles.put(guild.getIdLong(), joinRoleId);
                            new Thread(() -> PixelSniper.mySQL.setRole(guild.getIdLong(), joinRoleId, RoleType.JOIN)).start();
                            event.reply("JoinRole changed to **@" + guild.getRoleById(joinRoleId).getName() + "** by **" + event.getFullAuthorName() + "**");
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
