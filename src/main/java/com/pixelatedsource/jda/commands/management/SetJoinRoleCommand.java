package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.RoleType;
import com.pixelatedsource.jda.utils.MessageHelper;
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
            if (Helpers.hasPerm(event.getMember(), this.commandName, 1)) {
                Guild guild = event.getGuild();
                String joinRoleId = PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN);
                String[] args = event.getArgs().split("\\s+");
                if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                    String id;
                    if (event.getMessage().getMentionedRoles().size() == 1) {
                        id = event.getMessage().getMentionedRoles().get(0).getId();
                    } else if (args[0].matches("\\d+") && guild.getRoleById(args[0]) != null) {
                        id = args[0];
                    } else if (args[0].equalsIgnoreCase("null")) {
                        id = null;
                    } else {
                        MessageHelper.sendUsage(this, event);
                        return;
                    }
                    if (PixelSniper.mySQL.setRole(event.getGuild(), id, RoleType.JOIN)) {
                        String role1Name = guild.getRoleById(joinRoleId) != null ? "@" + guild.getRoleById(joinRoleId).getName() : joinRoleId;
                        String role2Name = guild.getRoleById(PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN)) != null ?
                                "@" + guild.getRoleById(PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN)).getName() :
                                PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN);
                        event.reply("JoinRole has been changed from **" + role1Name + "** to **" + role2Name + "**");
                    }
                } else {
                    String currentRoleName = guild.getRoleById(PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN)) != null ?
                            "@" + guild.getRoleById(PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN)).getName() :
                            PixelSniper.mySQL.getRoleId(guild, RoleType.JOIN);
                    event.reply("**" + currentRoleName + "**");
                }
            } else {
                event.reply("You need the permission `" + commandName + "` to execute this command.");
            }
        } else {
            event.reply(Helpers.guildOnly);
        }
    }
}
