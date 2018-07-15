package com.pixelatedsource.jda.commands.management;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.Category;
import com.pixelatedsource.jda.blub.Command;
import com.pixelatedsource.jda.blub.CommandEvent;
import com.pixelatedsource.jda.blub.Need;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static com.pixelatedsource.jda.PixelSniper.PREFIX;

public class ClearChannelCommand extends Command {

    public ClearChannelCommand() {
        this.commandName = "clearChannel";
        this.description = "makes a clone of the current channel (permissions ect included) and deletes the original one";
        this.usage = PREFIX + commandName + " [textChannel]";
        this.extra = "The bot can't copy permissions of role which are higher or equal to the bot's highest role";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS};
        this.needs = new Need[]{Need.GUILD, Need.ROLE};
    }

    public static HashMap<Long, HashMap<Long, Long>> possibleDeletes = new HashMap<>();
    public static HashMap<Long, Long> messageUser = new HashMap<>();

    @Override
    protected void execute(CommandEvent event) {
        if (Helpers.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            if (args.length > 0 && !args[0].equalsIgnoreCase("")) {
                TextChannel channel = event.getGuild().getTextChannelById(Helpers.getTextChannelByArgsN(event, args[0]));
                if (channel != null) {
                    if (!possibleDeletes.containsKey(event.getGuild().getIdLong()) || !possibleDeletes.get(event.getGuild().getIdLong()).containsValue(channel.getIdLong())) {
                        channel.sendMessage("Are you sure you want to remove all messages from " + channel.getAsMention() + "?").queue(s -> setupQuestion(channel, s, event.getAuthorId()));
                    } else {
                        event.reply("There is still another question in that channel which has to be answered\nThat question will be removed after 60 seconds of it's sent time");
                    }
                } else {
                    event.reply("Unknown TextChannel");
                }
            } else {
                if (!possibleDeletes.containsKey(event.getGuild().getIdLong()) || !possibleDeletes.get(event.getGuild().getIdLong()).containsValue(event.getTextChannel().getIdLong())) {
                    event.getTextChannel().sendMessage("Are you sure you want to remove all messages from this channel?").queue(s -> setupQuestion(event.getTextChannel(), s, event.getAuthorId()));
                } else {
                    event.reply("There is still another question in this channel which has to be answered\nThat question will be removed after 60 seconds of it's sent time");
                }
            }
        }
    }

    private void setupQuestion(TextChannel channel, Message s, long authorId) {
        HashMap<Long, Long> messageChannel = new HashMap<>();
        messageChannel.put(s.getIdLong(), channel.getIdLong());
        possibleDeletes.put(channel.getGuild().getIdLong(), messageChannel);
        messageUser.put(s.getIdLong(), authorId);
        s.addReaction(channel.getJDA().getEmoteById(463250265026330634L)).queue();
        s.addReaction(channel.getJDA().getEmoteById(463250264653299713L)).queue();
        s.delete().queueAfter(60, TimeUnit.SECONDS, null,
                (failure) -> {});
    }
}
