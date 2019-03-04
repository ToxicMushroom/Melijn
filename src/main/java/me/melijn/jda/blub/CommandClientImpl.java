package me.melijn.jda.blub;

import me.melijn.jda.Melijn;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.restaction.MessageAction;
import org.apache.commons.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CommandClientImpl extends ListenerAdapter implements CommandClient {

    private final Map<String, Integer> commandIndex;
    public final List<Command> commands;
    private final Melijn melijn;
    private final String guildOnly = "This command is to be used in guilds only";
    private final String nsfwOnly = "This command is to be used in (not safe for work) better known as [NSFW] channels only and can contain 18+ content";

    public CommandClientImpl(Melijn melijn, long ownerId, Set<Command> commands) {
        this.melijn = melijn;
        if (ownerId == -1)
            throw new IllegalArgumentException("Owner ID was set null or not set! Please provide an User ID to register as the owner!");
        this.commands = new ArrayList<>();

        this.commandIndex = new HashMap<>();
        commands.forEach(this::addCommand);
        this.commandIndex.clear();
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
        commands.add(command);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return;
            boolean nickname = event.getGuild() != null && event.getGuild().getSelfMember().getNickname() != null;

            if ((event.getGuild() != null && melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()) && event.getAuthor().getIdLong() != Melijn.OWNERID) ||
                    melijn.getVariables().blockedUserIds.contains(event.getAuthor().getIdLong()) ||
                    (event.getGuild() != null && melijn.getVariables().blockedUserIds.contains(event.getGuild().getOwnerIdLong())))
                return;

            String[] parts = null;
            String rawContent = event.getMessage().getContentRaw();
            String prefix = event.getGuild() != null ? melijn.getVariables().prefixes.getUnchecked(event.getGuild().getIdLong()) : Melijn.PREFIX;
            if (rawContent.toLowerCase().startsWith(prefix.toLowerCase()))
                parts = Arrays.copyOf(rawContent.substring(prefix.length()).trim().split("\\s+", 2), 2);
            else if (rawContent.toLowerCase().startsWith(((nickname ? "<@!" : "<@") + event.getJDA().getSelfUser().getId() + ">")))
                parts = Arrays.copyOf(rawContent.substring(((nickname ? "<@!" : "<@") + event.getJDA().getSelfUser().getId() + ">").length()).trim().split("\\s+", 2), 2);
            else {
                for (String s : melijn.getVariables().privatePrefixes.getUnchecked(event.getAuthor().getIdLong())) {
                    if (rawContent.toLowerCase().startsWith(s.toLowerCase()))
                        parts = Arrays.copyOf(rawContent.substring(s.length()).trim().split("\\s+", 2), 2);
                }
            }

            if (parts != null && (event.isFromType(ChannelType.PRIVATE) || event.getTextChannel().canTalk())) {
                String name = parts[0];
                String args = parts[1] == null ? "" : parts[1];
                commands.stream().filter(cmd -> cmd.isCommandFor(name)).findAny().ifPresent(command -> {

                    if (shouldNotRun(event, command)) return;
                    if (event.getGuild() != null && melijn.getVariables().disabledGuildCommands.containsKey(event.getGuild().getIdLong()) && melijn.getVariables().disabledGuildCommands.get(event.getGuild().getIdLong()).contains(command.getId()))
                        return;
                    if (event.getGuild() != null && melijn.getVariables().cooldownManager.isActive(event.getGuild().getIdLong(), event.getAuthor().getIdLong(), command.id)) {
                        event.getTextChannel().sendMessage(String.format("You have to wait **%dms** before using **%s** again",
                                melijn.getVariables().cooldownManager.getTimeLeft(event.getGuild().getIdLong(), event.getAuthor().getIdLong(), command.id),
                                command.getCommandName())).queue();
                        return;
                    }
                    CommandEvent cevent = new CommandEvent(event, args, this, name);
                    command.run(cevent);
                    melijn.getTaskManager().async(() -> {
                        melijn.getMySQL().updateUsage(command.getId(), System.currentTimeMillis());
                        if (event.getGuild() == null || melijn.getHelpers().hasPerm(event.getMember(), "bypass.cooldown", 1))
                            return;
                        melijn.getVariables().cooldownManager.updateCooldown(event.getGuild().getIdLong(), event.getAuthor().getIdLong(), command.id);
                    });
                });
            }
            if (event.getGuild() != null && event.getGuild().getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_WRITE) && melijn.getVariables().serverHasCC.getUnchecked(event.getGuild().getIdLong())) {
                JSONArray ccs = melijn.getMySQL().getCustomCommands(event.getGuild().getIdLong());
                String message = event.getMessage().getContentRaw();
                String regex = "(" +
                        Pattern.quote(event.getJDA().getSelfUser().getAsMention()) + ")|(" +
                        Pattern.quote(melijn.getVariables().prefixes.getUnchecked(event.getGuild().getIdLong())) + ")(\\s+)?";
                String justName = message.replaceFirst(regex, "");
                for (int i = 0; i < ccs.length(); i++) {
                    JSONObject command = ccs.getJSONObject(i);
                    if (command.getBoolean("prefix")) {
                        if (message.equals(justName)) continue;
                        if (justName.toLowerCase().startsWith(command.getString("name").toLowerCase())) {
                            customCommandSender(command, event.getGuild(), event.getAuthor(), event.getTextChannel());
                            return;
                        }
                        for (String alias : command.getString("aliases").split("%split%")) {
                            if (justName.equalsIgnoreCase(alias) && !alias.isEmpty()) {
                                customCommandSender(command, event.getGuild(), event.getAuthor(), event.getTextChannel());
                                return;
                            }
                        }
                    } else {
                        if (message.toLowerCase().startsWith(command.getString("name").toLowerCase())) {
                            customCommandSender(command, event.getGuild(), event.getAuthor(), event.getTextChannel());
                            return;
                        }
                        if (command.getString("aliases").isEmpty()) continue;
                        for (String alias : command.getString("aliases").split("%split%")) {
                            if (message.equalsIgnoreCase(alias) && !alias.isEmpty()) {
                                customCommandSender(command, event.getGuild(), event.getAuthor(), event.getTextChannel());
                                return;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            melijn.getMessageHelper().printException(Thread.currentThread(), e, event.getGuild(), event.getChannel());
        }
    }

    private boolean shouldNotRun(MessageReceivedEvent event, Command command) {
        if (command.getCategory() == Category.DEVELOPER && event.getAuthor().getIdLong() != Melijn.OWNERID)
            return true;
        if (noPermission(event, command)) return true;
        return unFulfilledNeeds(event, command);
    }

    private void customCommandSender(JSONObject command, Guild guild, User author, TextChannel channel) {
        try {
            String attachment = command.getString("attachment");
            if (melijn.getHelpers().isJSONObjectValid(command.getString("message"))) {
                String[] messages = command.getString("message").split("%split%");
                JSONObject content = new JSONObject(messages[melijn.getMessageHelper().randInt(0, messages.length - 1)]);
                MessageAction action = null;
                if (content.has("content") && !content.getString("content").isEmpty()) { //Als er een gewone message bij zit
                    action = channel.sendMessage(melijn.getMessageHelper().variableFormat(content.getString("content"), guild, author));
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
                String[] messages = command.getString("message").split("%split%");
                MessageAction action = channel.sendMessage(
                        melijn.getMessageHelper().variableFormat(messages[melijn.getMessageHelper().randInt(0, messages.length - 1)], guild, author)
                );
                if (attachment.matches("https?://.*")) {
                    action = action.addFile(new URL(attachment).openStream(), "attachment" + attachment.substring(attachment.lastIndexOf(".")));
                }
                action.queue();
            }
        } catch (IOException | IllegalArgumentException ignored) {
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
                        event.getTextChannel().sendMessage(nsfwOnly).queue();
                        return true;
                    }
                    break;
                case GUILD:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(guildOnly).queue();
                        return true;
                    }
                    break;
                case ROLE:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(guildOnly).queue();
                        return true;
                    }
                    if (event.getGuild().getSelfMember().getRoles().size() == 0) {
                        event.getTextChannel().sendMessage("I will not be able to do that without a role").queue();
                        return true;
                    }

                    break;
                case VOICECHANNEL:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(guildOnly).queue();
                        return true;
                    }
                    if (!event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        event.getTextChannel().sendMessage("I'm not in a voice channel").queue();
                        return true;
                    }
                    break;
                case SAME_VOICECHANNEL:
                    if (event.getGuild() == null) {
                        event.getPrivateChannel().sendMessage(guildOnly).queue();
                        return true;
                    }
                    if (event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        if ((event.getGuild().getSelfMember().getVoiceState().getChannel() != event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel()) &&
                                !melijn.getHelpers().hasPerm(event.getGuild().getMember(event.getAuthor()), "bypass.sameVoiceChannel", 1)) {
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
                        event.getPrivateChannel().sendMessage(guildOnly).queue();
                        return true;
                    }
                    if (!event.getGuild().getMember(event.getAuthor()).getVoiceState().inVoiceChannel()) {
                        event.getTextChannel().sendMessage("You have to be in a voice channel to use this command").queue();
                        return true;
                    }
                    if (event.getGuild().getSelfMember().getVoiceState().inVoiceChannel()) {
                        if ((event.getGuild().getSelfMember().getVoiceState().getChannel() != event.getGuild().getMember(event.getAuthor()).getVoiceState().getChannel())) {
                            if (!melijn.getHelpers().hasPerm(event.getGuild().getMember(event.getAuthor()), "bypass.sameVoiceChannel", 1)) {
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

    @Override
    public Melijn getMelijn() {
        return melijn;
    }
}
