package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SetMuteRoleCommand extends Command {

    public static final LoadingCache<Long, Long> muteRoleCache = CacheBuilder.newBuilder()
            .maximumSize(10)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Long load(@NotNull Long key) {
                    return Melijn.mySQL.getRoleId(key, RoleType.MUTE);
                }
            });

    public SetMuteRoleCommand() {
        this.commandName = "setMuteRole";
        this.description = "Set the role that will be added to a user when muted";
        this.extra = "The mute role should be higher then the default role and shouldn't have talking permission";
        this.usage = PREFIX + commandName + " [role]";
        this.needs = new Need[]{Need.GUILD};
        this.aliases = new String[]{"smr"};
        this.category = Category.MANAGEMENT;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            Guild guild = event.getGuild();
            String[] args = event.getArgs().split("\\s+");
            long role = muteRoleCache.getUnchecked(guild.getIdLong());
            if (args.length == 0 || args[0].isBlank()) {
                if (role != -1 && guild.getRoleById(role) != null)
                    event.reply("Current MuteRole: **@" + guild.getRoleById(role).getName() + "**");
                else event.reply("Current MuteRole is unset");
            } else {
                if (args[0].equalsIgnoreCase("null")) {
                    TaskScheduler.async(() -> {
                        Melijn.mySQL.removeRole(guild.getIdLong(), RoleType.JOIN);
                        muteRoleCache.invalidate(guild.getIdLong());
                    });
                    event.reply("MuteRole has been unset by **" + event.getFullAuthorName() + "**");
                } else {
                    Role muteRole = Helpers.getRoleByArgs(event, args[0]);
                    if (muteRole != null) {
                        if (muteRole.getIdLong() != guild.getIdLong()) {
                            if (guild.getSelfMember().getRoles().size() != 0 && guild.getSelfMember().getRoles().get(0).canInteract(muteRole)) {
                                TaskScheduler.async(() -> {
                                    Melijn.mySQL.setRole(guild.getIdLong(), muteRole.getIdLong(), RoleType.MUTE);
                                    muteRoleCache.put(guild.getIdLong(), muteRole.getIdLong());
                                });
                                event.reply("MuteRole changed to **@" + muteRole.getName() + "** by **" + event.getFullAuthorName() + "**");
                            } else {
                                event.reply("The MuteRole hasn't been changed due: **@" + muteRole.getName() + "** is higher or equal in the role-hierarchy then my highest role.\nThis means that I will not be able to give the role to anyone ex.(Mods can't give people Admin it breaks logic)");
                            }
                        } else {
                            event.reply("The @everyone role cannot be as the MuteRole because everyone has it");
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
