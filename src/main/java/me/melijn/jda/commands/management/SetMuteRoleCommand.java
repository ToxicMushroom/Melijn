package me.melijn.jda.commands.management;

import me.melijn.jda.blub.*;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import static me.melijn.jda.Melijn.PREFIX;

public class SetMuteRoleCommand extends Command {

    public SetMuteRoleCommand() {
        this.commandName = "setMuteRole";
        this.description = "Sets the role that will be added to users when they get muted";
        this.extra = "The mute role should be higher then the default role and shouldn't have talking permission";
        this.usage = PREFIX + commandName + " [role]";
        this.needs = new Need[]{Need.GUILD};
        this.aliases = new String[]{"smr"};
        this.category = Category.MANAGEMENT;
        this.id = 49;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            long role = event.getVariables().muteRoleCache.get(guild.getIdLong());
            if (event.getArgs().isEmpty()) {
                if (role == -1 || guild.getRoleById(role) == null)
                    event.reply("Current MuteRole is unset");
                else event.reply("Current MuteRole: **@" + guild.getRoleById(role).getName() + "**");
                return;
            }

            if (args[0].equalsIgnoreCase("null")) {
                event.async(() -> {
                    event.getMySQL().removeRole(guild.getIdLong(), RoleType.JOIN);
                    event.getVariables().muteRoleCache.invalidate(guild.getIdLong());
                });
                event.reply("MuteRole has been unset by **" + event.getFullAuthorName() + "**");
            } else {
                Role muteRole = event.getHelpers().getRoleByArgs(event, args[0]);
                if (muteRole == null) {
                    event.sendUsage(this, event);
                    return;
                }
                if (muteRole.getIdLong() == guild.getIdLong()) {
                    event.reply("The @everyone role cannot be as the MuteRole because everyone has it");
                    return;
                }
                if (guild.getSelfMember().getRoles().size() == 0 || !guild.getSelfMember().getRoles().get(0).canInteract(muteRole)) {
                    event.reply("" +
                            "The MuteRole hasn't been changed due: **@" + muteRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\n" +
                            "This means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)"
                    );
                    return;
                }
                event.async(() -> {
                    event.getMySQL().setRole(guild.getIdLong(), muteRole.getIdLong(), RoleType.MUTE);
                    event.getVariables().muteRoleCache.put(guild.getIdLong(), muteRole.getIdLong());
                });
                event.reply("MuteRole changed to **@" + muteRole.getName() + "** by **" + event.getFullAuthorName() + "**");
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
