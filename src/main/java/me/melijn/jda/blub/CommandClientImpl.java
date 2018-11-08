package me.melijn.jda.blub;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.DisableCommand;
import me.melijn.jda.commands.management.SetPrefixCommand;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.apache.commons.text.WordUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class CommandClientImpl extends ListenerAdapter implements CommandClient {

    private static final int INDEX_LIMIT = 200;
    private final TObjectIntMap<String> commandIndex;
    public final List<Command> commands;
    //private CommandListener listener = null;

    public static final LoadingCache<Long, Boolean> serverHasCC = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                public Boolean load(@NotNull Long key) {
                    return Melijn.mySQL.getCustomCommands(key).length() > 0;
                }
            });

    public CommandClientImpl(long ownerId, List<Command> commands) {
        if (ownerId == -1)
            throw new IllegalArgumentException("Owner ID was set null or not set! Please provide an User ID to register as the owner!");
        this.commandIndex = new TObjectIntHashMap<>();
        this.commands = new ArrayList<>();
        for (Command command : commands) {
            addCommand(command);
        }
    }



    /*@Override
    public void setListener(CommandListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandListener getListener() {
        return listener;
    }*/

    @Override
    public List<Command> getCommands() {
        return commands;
    }

    @Override
    public void addCommand(Command command) {
        addCommand(command, commands.size());
    }

    @Override
    public void addCommand(Command command, int index) {
        if (index > commands.size() || index < 0)
            throw new ArrayIndexOutOfBoundsException("Index specified is invalid: [" + index + "/" + commands.size() + "]");
        String name = command.getCommandName();
        synchronized (commandIndex) {
            if (commandIndex.containsKey(name))
                throw new IllegalArgumentException("Command added has a name or alias that has already been indexed: \"" + name + "\"!");
            for (String alias : command.getAliases()) {
                if (commandIndex.containsKey(alias))
                    throw new IllegalArgumentException("Command added has a name or alias that has already been indexed: \"" + alias + "\"!");
                commandIndex.put(alias, index);
            }
            commandIndex.put(name, index);
            if (index < commands.size())
                commandIndex.keySet().stream().filter(key -> commandIndex.get(key) > index).collect(Collectors.toList()).forEach(key -> commandIndex.put(key, commandIndex.get(key) + 1));
        }
        commands.add(index, command);
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;
            boolean nickname = event.getGuild() != null && event.getGuild().getSelfMember().getNickname() != null;

            if ((event.getGuild() != null && EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()) && event.getAuthor().getIdLong() != Melijn.OWNERID) ||
                    EvalCommand.userBlackList.contains(event.getAuthor().getIdLong()) ||
                    (event.getGuild() != null && EvalCommand.userBlackList.contains(event.getGuild().getOwnerIdLong())))
                return;

            //boolean[] isCommand = new boolean[]{false};
            String[] parts = null;
            String rawContent = event.getMessage().getContentRaw();
            String prefix = event.getGuild() != null ? SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong()) : Melijn.PREFIX;
            if (rawContent.toLowerCase().startsWith(prefix.toLowerCase()))
                parts = Arrays.copyOf(rawContent.substring(prefix.length()).trim().split("\\s+", 2), 2);
            else if (rawContent.toLowerCase().startsWith((String.valueOf(nickname ? "<@!" : "<@") + event.getJDA().getSelfUser().getId() + ">")))
                parts = Arrays.copyOf(rawContent.substring((String.valueOf(nickname ? "<@!" : "<@") + event.getJDA().getSelfUser().getId() + ">").length()).trim().split("\\s+", 2), 2);

            if (parts != null && (event.isFromType(ChannelType.PRIVATE) || event.getTextChannel().canTalk())) {
                String name = parts[0];
                String args = parts[1] == null ? "" : parts[1];
                if (commands.size() < INDEX_LIMIT + 1) {
                    commands.stream().filter(cmd -> cmd.isCommandFor(name)).findAny().ifPresent(command -> {

                        if (command.getCategory() == Category.DEVELOPER && event.getAuthor().getIdLong() != Melijn.OWNERID)
                            return;
                        if (noPermission(event, command)) return;
                        if (unFulfilledNeeds(event, command)) return;
                        if (event.getGuild() != null && DisableCommand.disabledGuildCommands.containsKey(event.getGuild().getIdLong()) && DisableCommand.disabledGuildCommands.get(event.getGuild().getIdLong()).contains(commands.indexOf(command)))
                            return;

                        Melijn.mySQL.updateUsage(commands.indexOf(command), System.currentTimeMillis());
                        CommandEvent cevent = new CommandEvent(event, args, this, name);
                        command.run(cevent);
                    });
                } else {
                    int i = commandIndex.containsKey(name.toLowerCase()) ? commandIndex.get(name.toLowerCase()) : -1;
                    if (i != -1) {
                        Command command = commands.get(i);

                        if (command.getCategory() == Category.DEVELOPER && event.getAuthor().getIdLong() != Melijn.OWNERID)
                            return;
                        if (noPermission(event, command)) return;
                        if (unFulfilledNeeds(event, command)) return;

                        Melijn.mySQL.updateUsage(i, System.currentTimeMillis());
                        CommandEvent cevent = new CommandEvent(event, args, this, name);
                        command.run(cevent);
                    }
                }
            }
            if (event.getGuild() != null && serverHasCC.getUnchecked(event.getGuild().getIdLong())) {
                JSONArray ccs = Melijn.mySQL.getCustomCommands(event.getGuild().getIdLong());
                for (int i = 0; i < ccs.length(); i++) {
                    JSONObject command = ccs.getJSONObject(i);
                    if (command.getBoolean("prefix")) {
                        String message = event.getMessage().getContentRaw();
                        if (message.matches("<@" + event.getJDA().getSelfUser().getId() + ">(\\s+)?" + command.getString("name"))) {
                            customCommandSender(command, event.getGuild(), event.getTextChannel());
                        } else if (message.matches(SetPrefixCommand.prefixes.getUnchecked(event.getGuild().getIdLong()) + "(\\s+)?" + command.getString("name"))) {
                            customCommandSender(command, event.getGuild(), event.getTextChannel());
                        }
                    } else if (command.getString("name").equalsIgnoreCase(event.getMessage().getContentStripped())) {
                        customCommandSender(command, event.getGuild(), event.getTextChannel());
                    }
                }
            }
        } catch (Exception e) {
            MessageHelper.printException(Thread.currentThread(), e, event.getGuild(), event.getChannel());
        }
    }

    private void customCommandSender(JSONObject command, Guild guild, TextChannel channel) {
        try {
            String attachment = command.getString("attachment");
            if (isJSONObjectValid(command.getString("message"))) {
                JSONObject content = new JSONObject(command.getString("message"));
                MessageAction action = null;
                if (content.has("content") && !content.getString("content").isBlank()) { //Als er een gewone message bij zit
                    action = channel.sendMessage(content.getString("content"));
                    if (content.has("embed"))
                        action = action.embed(
                                ((JDAImpl) guild.getJDA()).getEntityBuilder().createMessageEmbed(content.getJSONObject("embed").put("type", "link"))
                        );
                    if (attachment.matches("https?://.*")) {
                        action = action.addFile(new URL(attachment).openStream(), "attachment" + attachment.substring(attachment.lastIndexOf(".")));
                    }
                } else { //Als er een geen gewone message bij zit
                    if (content.has("embed") && attachment.matches("https?://.*")) {
                        action = channel.sendMessage(((JDAImpl) guild.getJDA()).getEntityBuilder().createMessageEmbed(content.getJSONObject("embed").put("type", "link")));
                        action = action.addFile(new URL(attachment).openStream(), "attachment" + attachment.substring(attachment.lastIndexOf(".")));
                    } else if (content.has("embed")) {
                        action = channel.sendMessage(((JDAImpl) guild.getJDA()).getEntityBuilder().createMessageEmbed(content.getJSONObject("embed").put("type", "link")));
                    } else if (attachment.matches("https?://.*")) {
                        action = channel.sendFile(new URL(attachment).openStream(), "attachment" + attachment.substring(attachment.lastIndexOf(".")));
                    }
                }
                if (action != null)
                    action.queue();
            } else {
                MessageAction action = channel.sendMessage(command.getString("message"));
                if (attachment.matches("https?://.*")) {
                    action = action.addFile(new URL(attachment).openStream(), "attachment" + attachment.substring(attachment.lastIndexOf(".")));
                }
                action.queue();
            }
        } catch (IOException ignored) {
        }
    }

    private boolean isJSONObjectValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ignored) {
            return false;
        }
        return true;
    }

    private boolean noPermission(MessageReceivedEvent event, Command command) {
        if (event.getGuild() != null && event.getTextChannel() != null)
            for (Permission perm : command.getPermissions()) {
                if (!event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), perm)) {
                    event.getTextChannel().sendMessage("To use `" + command.getCommandName() + "` I need the **" + WordUtils.capitalizeFully(perm.toString().replaceAll("_", " ")) + "** permission").queue();
                    return true;
                }
            }
        return false;
    }

    private boolean unFulfilledNeeds(MessageReceivedEvent event, Command command) {
        for (Need need : command.needs) {
            switch (need) {
                case NSFW:
                    if (event.getGuild() == null) return false;
                    if (!event.getTextChannel().isNSFW()) {
                        event.getTextChannel().sendMessage(Helpers.nsfwOnly).queue();
                        return true;
                    }
                    break;
                case GUILD:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(Helpers.guildOnly).queue();
                        return true;
                    }
                    break;
                case ROLE:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(Helpers.guildOnly).queue();
                        return true;
                    }
                    if (event.getGuild().getSelfMember().getRoles().size() == 0) {
                        event.getTextChannel().sendMessage("I will not be able to do that without a role").queue();
                        return true;
                    }

                    break;
                case VOICECHANNEL:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(Helpers.guildOnly).queue();
                        return true;
                    }
                    if (!event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        event.getTextChannel().sendMessage("I'm not in a voice channel").queue();
                        return true;
                    }
                    break;
                case SAME_VOICECHANNEL:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(Helpers.guildOnly).queue();
                        return true;
                    }
                    if (event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        if ((event.getGuild().getSelfMember().getVoiceState().getChannel() != event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel()) &&
                                !Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), "bypass.sameVoiceChannel", 1)) {
                            event.getTextChannel().sendMessage("You have to be in the same voice channel as me to do this").queue();
                            return true;
                        }
                    } else {
                        event.getTextChannel().sendMessage("I'm not in a voice channel").queue();
                        return true;
                    }
                    break;
                case SAME_VOICECHANNEL_OR_DISCONNECTED:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(Helpers.guildOnly).queue();
                        return true;
                    }
                    if (!event.getGuild().getMember(event.getAuthor()).getVoiceState().inVoiceChannel()) {
                        event.getTextChannel().sendMessage("You have to be in a voice channel to use this command").queue();
                        return true;
                    }
                    if (event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        if ((event.getGuild().getSelfMember().getVoiceState().getChannel() != event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel())) {
                            if (!Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), "bypass.sameVoiceChannel", 1)) {
                                event.getTextChannel().sendMessage("You have to be in the same voice channel as me to do this").queue();
                                return true;
                            } else {
                                if (event.getGuild().getSelfMember().hasPermission(event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel(), Permission.VOICE_CONNECT)) {
                                    return false;
                                } else {
                                    event.getTextChannel().sendMessage("I have no permission to join your voice channel :C").queue();
                                    return true;
                                }
                            }
                        }
                    } else {
                        if (event.getGuild().getSelfMember().hasPermission(event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel(), Permission.VOICE_CONNECT)) {
                            return false;
                        } else {
                            event.getTextChannel().sendMessage("I have no permission to join your voice channel :C").queue();
                            return true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }
}
