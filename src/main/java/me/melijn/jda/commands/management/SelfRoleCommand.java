package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.cache.SnowflakeCacheView;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static me.melijn.jda.Melijn.PREFIX;

public class SelfRoleCommand extends Command {



    public SelfRoleCommand() {
        this.commandName = "selfRole";
        this.description = "Main command to manage SelfRoles";
        this.usage = PREFIX + commandName + " <add | remove | list> [role] [emote | emoji]";
        this.aliases = new String[]{"sr"};
        this.category = Category.MANAGEMENT;
        this.needs = new Need[]{Need.GUILD};
        this.id = 98;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Guild guild = event.getGuild();
            if (args.length == 0 || args[0].isEmpty()) {
                event.sendUsage(this, event);
                return;
            }
            Map<Long, String> cache = event.getVariables().selfRoles.getUnchecked(guild.getIdLong());
            switch (args[0].toLowerCase()) {
                case "add":
                    StringBuilder builder = new StringBuilder();
                    event.getArgs().codePoints().forEachOrdered(code -> {
                        char[] chars = Character.toChars(code);
                        if (chars.length > 1) {
                            StringBuilder hex0 = new StringBuilder(Integer.toHexString(chars[0]).toUpperCase());
                            StringBuilder hex1 = new StringBuilder(Integer.toHexString(chars[1]).toUpperCase());
                            while (hex0.length() < 4)
                                hex0.insert(0, "0");
                            while (hex1.length() < 4)
                                hex1.insert(0, "0");
                            builder.append("\\u").append(hex0).append("\\u").append(hex1);
                        } else {
                            StringBuilder hex = new StringBuilder(Integer.toHexString(code).toUpperCase());
                            while (hex.length() < 4)
                                hex.insert(0, "0");
                            builder.append("\\u").append(hex);
                        }
                    });
                    if (args.length < 3 || (event.getMessage().getEmotes().size() < 1 && !builder.toString().matches("\\\\u.*"))) {
                        event.reply(event.getVariables().prefixes.getUnchecked(guild.getIdLong()) + commandName + " add <role> <emote | emoji>");
                        return;
                    }
                    Role roleAdded = event.getHelpers().getRoleByArgs(event, args[1]);
                    if (roleAdded == null || roleAdded.getIdLong() == guild.getIdLong()) {
                        event.reply(event.getVariables().prefixes.getUnchecked(guild.getIdLong()) + commandName + " add <role> <emote | emoji>");
                        return;
                    }
                    String emote = event.getMessage().getEmotes().size() > 0 ? event.getMessage().getEmotes().get(0).getId() : args[2];
                    if (cache.keySet().contains(roleAdded.getIdLong()) && cache.get(roleAdded.getIdLong()).equalsIgnoreCase(emote)) {
                        event.reply("This SelfRole already exist: choose another role or emote/emoji.");
                        return;
                    }
                    event.async(() ->  {
                        event.getMySQL().addSelfRole(guild.getIdLong(), roleAdded.getIdLong(), emote);
                        event.getVariables().selfRoles.invalidate(guild.getIdLong());
                    });
                    event.reply("SelfRole added: **@" + roleAdded.getName() + "** by **" + event.getFullAuthorName() + "**");
                    break;
                case "remove":
                    if (args.length < 2) {
                        event.reply(event.getVariables().prefixes.getUnchecked(guild.getIdLong()) + commandName + " remove <role> [emote | emoji]");
                        return;
                    }
                    Role roleRemoved = event.getHelpers().getRoleByArgs(event, args[1]);
                    if (roleRemoved == null || roleRemoved.getIdLong() == guild.getIdLong()) {
                        event.reply(event.getVariables().prefixes.getUnchecked(guild.getIdLong()) + commandName + " remove <role> [emote | emoji]");
                        return;
                    }
                    String emote2 = event.getMessage().getEmotes().size() > 0 ? event.getMessage().getEmotes().get(0).getId() : (args.length < 3 ? "" : args[2]);
                    if (!cache.containsKey(roleRemoved.getIdLong()) || (emote2.isEmpty() && !cache.containsValue(emote2))) {
                        event.reply("This entry does not exist");
                        return;
                    }
                    if (emote2.isEmpty()) {
                        event.getMySQL().removeSelfRole(guild.getIdLong(), roleRemoved.getIdLong());
                        event.getVariables().selfRoles.invalidate(guild.getIdLong());
                        event.reply("SelfRole entries removed for role: **@" + roleRemoved.getName() + "** by **" + event.getFullAuthorName() + "**");
                    } else {
                        event.getMySQL().removeSelfRole(guild.getIdLong(), roleRemoved.getIdLong(), emote2);
                        event.getVariables().selfRoles.invalidate(guild.getIdLong());
                        event.reply("SelfRole entry removed for role: **@" + roleRemoved.getName() + "** by **" + event.getFullAuthorName() + "**");
                    }

                    break;
                case "list":
                    StringBuilder sb = new StringBuilder("**SelfRoles**\n```INI");
                    Map<Long, String> rolesIds = event.getVariables().selfRoles.getUnchecked(guild.getIdLong());

                    AtomicInteger i = new AtomicInteger();
                    rolesIds.forEach((id, mEmote) -> {
                        SnowflakeCacheView<Role> roles = guild.getRoleCache();
                        Role role = roles.getElementById(id);
                        if (role != null)
                            sb.append("\n").append(i.getAndIncrement()).append(" - [").append(role.getName()).append("] - ").append(id);
                    });
                    sb.append("```");
                    if (rolesIds.size() == 0) sb.append("There are no SelfRoles");
                    event.getMessageHelper().sendSplitMessage(event.getTextChannel(), sb.toString());
                    break;
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }
}
