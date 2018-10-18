package me.melijn.jda.commands.management;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gnu.trove.map.TLongObjectMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class SelfRoleCommand extends Command {

    public static final LoadingCache<Long, TLongObjectMap<String>> selfRoles = CacheBuilder.newBuilder()
            .maximumSize(30)
            .expireAfterAccess(1, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public TLongObjectMap<String> load(@NotNull Long key) {
                    return Melijn.mySQL.getSelfRoles(key);
                }
            });

    public SelfRoleCommand() {
        this.commandName = "selfRole";
        this.description = "Ma in command to add and remove SelfRoles";
        this.usage = PREFIX + commandName + " <add | remove | list> [role] [emote | emoji]";
        this.aliases = new String[]{"ssrlc"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
    }

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length == 0 || args[0].isBlank()) {
                MessageHelper.sendUsage(this, event);
                return;
            }
            switch (args[0].toLowerCase()) {
                case "add":
                    if (args.length < 2 && event.getMessage().getEmotes().size() > 0 || args[2].matches("\\\\u....")) {
                        event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " add <role> <emote | emoji>");
                        return;
                    }

                    String emote = event.getMessage().getEmotes().size() > 0 ? event.getMessage().getEmotes().get(0).getId() : args[2];
                    Role roleAdded = Helpers.getRoleByArgs(event, args[1]);
                    if (roleAdded != null && roleAdded.getIdLong() != guild.getIdLong()) {
                        Melijn.mySQL.addSelfRole(guild.getIdLong(), roleAdded.getIdLong(), emote);
                        event.reply("SelfRole added: **@" + roleAdded.getName() + "** by **" + event.getFullAuthorName() + "**");
                    } else {
                        event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " add <role> <emote | emoji>");
                    }
                    break;
                case "remove":
                    Role roleRemoved = Helpers.getRoleByArgs(event, args[1]);
                    if (roleRemoved != null && roleRemoved.getIdLong() != guild.getIdLong()) {
                        Melijn.mySQL.removeSelfRole(guild.getIdLong(), roleRemoved.getIdLong());
                        event.reply("SelfRole removed: **@" + roleRemoved.getName() + "** by **" + event.getFullAuthorName() + "**");
                    } else {
                        event.reply(SetPrefixCommand.prefixes.getUnchecked(guild.getIdLong()) + commandName + " add <role>");
                    }
                    break;
                case "list":
                    StringBuilder sb = new StringBuilder("**SelfRoles**\n```INI");
                    TLongObjectMap<String> rolesIds = selfRoles.getUnchecked(guild.getIdLong());
                    for (int i = 0; i < rolesIds.keys().length; i++) {
                        SnowflakeCacheView<Role> roles = guild.getRoleCache();
                        Role role = roles.getElementById(rolesIds.keys()[i]);
                        if (role != null)
                            sb.append("\n").append(i+1).append(" - [").append(role.getName()).append("] - ").append(rolesIds.get(rolesIds.keys()[i]));
                    }
                    sb.append("```");
                    if (rolesIds.keys().length == 0) sb.append("There are no SelfRoles");
                    MessageHelper.sendSplitMessage(event.getTextChannel(), sb.toString());
                    break;
            }
        }
    }
}
