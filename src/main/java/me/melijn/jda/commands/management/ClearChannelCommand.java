package me.melijn.jda.commands.management;

import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.blub.Need;
import me.melijn.jda.db.Variables;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.apache.commons.text.WordUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static me.melijn.jda.Melijn.PREFIX;

public class ClearChannelCommand extends Command {

    public ClearChannelCommand() {
        this.commandName = "clearChannel";
        this.description = "Makes a clone of the channel and deletes the original one";
        this.usage = PREFIX + commandName + " [textChannel]";
        this.extra = "The bot can't copy permissions of roles which are higher or equal to the bot's highest role";
        this.category = Category.MANAGEMENT;
        this.permissions = new Permission[]{Permission.MANAGE_CHANNEL, Permission.MANAGE_PERMISSIONS};
        this.needs = new Need[]{Need.GUILD, Need.ROLE};
        this.id = 10;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (event.hasPerm(event.getMember(), commandName, 1)) {
            String[] args = event.getArgs().split("\\s+");
            Variables vars = event.getVariables();
            if (event.getArgs().isEmpty()) {
                if (!vars.possibleDeletes.containsKey(event.getGuild().getIdLong()) || !vars.possibleDeletes.get(event.getGuild().getIdLong()).containsValue(event.getTextChannel().getIdLong())) {
                    event.getTextChannel().sendMessage("Are you sure you want to remove all messages from this channel?").queue(s ->
                            setupQuestion(event.getTextChannel(), vars, s, event.getAuthorId())
                    );
                } else {
                    event.reply("There is still another question in this channel which have to be answered\nThat question will be removed after 60 seconds of it's sent time");
                }
            } else {
                TextChannel channel = event.getGuild().getTextChannelById(event.getHelpers().getTextChannelByArgsN(event, args[0]));
                if (channel == null) {
                    event.reply("Unknown TextChannel");
                    return;
                }

                if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MANAGE_CHANNEL)) {
                   event.reply("To use `" + commandName + "` in " + channel.getAsMention() + ", I need the **" + WordUtils.capitalizeFully(Permission.MANAGE_CHANNEL.toString().replaceAll("_", " ")) + "** permission in that channel");
                   return;
                }

                if (!vars.possibleDeletes.containsKey(event.getGuild().getIdLong()) || !vars.possibleDeletes.get(event.getGuild().getIdLong()).containsValue(channel.getIdLong())) {
                    event.getTextChannel().sendMessage("Are you sure you want to remove all messages from " + channel.getAsMention() + "?").queue(s ->
                            setupQuestion(channel, vars, s, event.getAuthorId())
                    );
                } else {
                    event.reply("There is still another question in that channel which have to be answered\nThat question will be removed after 60 seconds of it's sent time");
                }
            }
        } else {
            event.reply("You need the permission `" + commandName + "` to execute this command.");
        }
    }

    private void setupQuestion(TextChannel channel, Variables vars, Message s, long authorId) {
        Map<Long, Long> messageChannel = new HashMap<>();
        Guild guild = channel.getJDA().asBot().getShardManager().getGuildById(340081887265685504L);
        long guildId = channel.getGuild().getIdLong();

        if (vars.possibleDeletes.containsKey(guildId)) {
            messageChannel.putAll(vars.possibleDeletes.get(channel.getGuild().getIdLong()));
        }
        messageChannel.put(s.getIdLong(), channel.getIdLong());
        vars.possibleDeletes.put(guildId, messageChannel);
        vars.messageUser.put(s.getIdLong(), authorId);

        guild.retrieveEmoteById(463250265026330634L).queue(listedEmote -> s.addReaction(listedEmote).queue());
        guild.retrieveEmoteById(463250264653299713L).queue(listedEmote -> s.addReaction(listedEmote).queue());

        s.delete().queueAfter(60, TimeUnit.SECONDS, (success) -> {
            if (vars.possibleDeletes.containsKey(guildId)) {
                Map<Long, Long> messageChannels = vars.possibleDeletes.get(guildId);

                messageChannels.remove(s.getIdLong());
                if (messageChannels.size() > 0) vars.possibleDeletes.put(guildId, messageChannel);
                else vars.possibleDeletes.remove(guildId);
            }
            vars.messageUser.remove(s.getIdLong());
        }, (failure) -> {
        });
    }
}
