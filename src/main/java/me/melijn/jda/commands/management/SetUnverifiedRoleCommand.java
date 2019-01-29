package me.melijn.jda.commands.management;

import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import static me.melijn.jda.Melijn.PREFIX;

public class SetUnverifiedRoleCommand extends Command {



    public SetUnverifiedRoleCommand() {
        this.commandName = "setUnverifiedRole";
        this.usage = PREFIX + commandName + " [role | null]";
        this.description = "Sets the UnverifiedRole that will be added to unverified members when they join";
        this.needs = new Need[]{Need.GUILD};
        this.category = Category.MANAGEMENT;
        this.aliases = new String[]{"sur"};
        this.id = 6;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            long role = event.getVariables().unverifiedRoleCache.getUnchecked(guild.getIdLong());
            if (args.length == 0 || args[0].isEmpty()) {
                if (role != -1 && guild.getRoleById(role) != null)
                    event.reply("Current UnverifiedRole: **@" + guild.getRoleById(role).getName() + "**");
                else event.reply("Current UnverifiedRole is unset");
            } else {
                if (args[0].equalsIgnoreCase("null")) {
                    event.async(() -> {
                        event.getMySQL().removeRole(guild.getIdLong(), RoleType.UNVERIFIED);
                        event.getVariables().unverifiedRoleCache.invalidate(guild.getIdLong());
                    });
                    event.reply("UnverifiedRole has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    Role unverifiedRole = event.getHelpers().getRoleByArgs(event, args[0]);
                    if (unverifiedRole == null) {
                        event.sendUsage(this, event);
                        return;
                    }
                        if (unverifiedRole.getIdLong() != guild.getIdLong()) {
                            if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).canInteract(unverifiedRole)) {
                                event.async(() -> {
                                    event.getMySQL().setRole(guild.getIdLong(), unverifiedRole.getIdLong(), RoleType.UNVERIFIED);
                                    event.getVariables().unverifiedRoleCache.put(guild.getIdLong(), unverifiedRole.getIdLong());
                                });
                                event.reply("UnverifiedRole changed to **@" + unverifiedRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                            }
                            else {
                                event.reply("The UnverifiedRole hasn't been changed due: **@" + unverifiedRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
                            }
                        } else {
                            event.reply("The @everyone role cannot be as the UnverifiedRole because everyone has it");
                        }
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
