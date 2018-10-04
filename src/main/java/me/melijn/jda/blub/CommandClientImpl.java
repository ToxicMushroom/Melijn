package me.melijn.jda.blub;

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
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CommandClientImpl extends ListenerAdapter implements CommandClient {

    private static final int INDEX_LIMIT = 200;
    private final long ownerId;
    private final TObjectIntMap<String> commandIndex;
    public final List<Command> commands;
    private final int linkedCacheSize;
    private CommandListener listener = null;

    public CommandClientImpl(long ownerId, ArrayList<Command> commands, int linkedCacheSize) {
        if (ownerId == -1)
            throw new IllegalArgumentException("Owner ID was set null or not set! Please provide an User ID to register as the owner!");
        this.ownerId = ownerId;
        this.commandIndex = new TObjectIntHashMap<>();
        this.commands = new ArrayList<>();
        this.linkedCacheSize = linkedCacheSize;
        for (Command command : commands) {
            addCommand(command);
        }
    }

    @Override
    public void setListener(CommandListener listener) {
        this.listener = listener;
    }

    @Override
    public CommandListener getListener() {
        return listener;
    }

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
    public String getPrefix() {
        return Melijn.PREFIX;
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;
            boolean nickname = event.getGuild() != null && event.getGuild().getSelfMember().getNickname() != null;
            if (event.getGuild() != null && EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()) && event.getAuthor().getIdLong() != Melijn.OWNERID)
                return;
            boolean[] isCommand = new boolean[]{false};
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

                            /*Custom merlijn crapcode*/
                            if (command.getCategory() == Category.DEVELOPER && event.getAuthor().getIdLong() != Melijn.OWNERID)
                                return;
                            if (noPermission(event, command)) return;
                            if (unFulfilledNeeds(event, command)) return;
                            if (event.getGuild() != null && DisableCommand.disabledGuildCommands.containsKey(event.getGuild().getIdLong()) && DisableCommand.disabledGuildCommands.get(event.getGuild().getIdLong()).contains(commands.indexOf(command)))
                                return;

                            /*Cool code from jda-utils*/
                            isCommand[0] = true;
                            Melijn.mySQL.updateUsage(commands.indexOf(command), System.currentTimeMillis());
                            CommandEvent cevent = new CommandEvent(event, args, this, name);
                            if (listener != null) listener.onCommand(cevent, command);
                            command.run(cevent);
                        });
                    } else {
                        int i = commandIndex.containsKey(name.toLowerCase()) ? commandIndex.get(name.toLowerCase()) : -1;
                        if (i != -1) {
                            Command command = commands.get(i);

                            /*Custom merlijn crapcode*/
                            if (command.getCategory() == Category.DEVELOPER && event.getAuthor().getIdLong() != Melijn.OWNERID)
                                return;
                            if (noPermission(event, command)) return;
                            if (unFulfilledNeeds(event, command)) return;

                            /*Cool code from jda-utils*/
                            isCommand[0] = true;
                            Melijn.mySQL.updateUsage(i, System.currentTimeMillis());
                            CommandEvent cevent = new CommandEvent(event, args, this, name);
                            if (listener != null) listener.onCommand(cevent, command);
                            command.run(cevent);
                        }
                    }
            }
            if (!isCommand[0] && listener != null) listener.onNonCommandMessage(event);
        } catch (Exception e) {
            MessageHelper.printException(Thread.currentThread(), e, event.getGuild(), event.getChannel());
        }
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
                    if (event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        if ((event.getGuild().getSelfMember().getVoiceState().getChannel() != event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel()) &&
                                !Helpers.hasPerm(event.getGuild().getMember(event.getAuthor()), "bypass.sameVoiceChannel", 1)) {
                            event.getTextChannel().sendMessage("You have to be in the same voice channel as me to do this").queue();
                            return true;
                        }
                    } else {
                        if (event.getGuild().getMember(event.getAuthor()).getVoiceState().inVoiceChannel()) {
                            if (event.getGuild().getSelfMember().hasPermission(event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel(), Permission.VOICE_CONNECT)) {
                                break;
                            } else {
                                event.getTextChannel().sendMessage("I have no permission to join your voice channel :C").queue();
                            }
                        } else {
                            event.getTextChannel().sendMessage("You're not in a voice channel").queue();
                        }
                        return true;
                    }
                    break;
                default:
                    break;
            }
        }
        return false;
    }
}
