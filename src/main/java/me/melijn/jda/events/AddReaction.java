package me.melijn.jda.events;

import me.melijn.jda.Helpers;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.ClearChannelCommand;
import me.melijn.jda.commands.music.SPlayCommand;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddReaction extends ListenerAdapter {

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getGuild() == null || EvalCommand.INSTANCE.getBlackList().contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        if (MusicManager.userMessageToAnswer.get(event.getUser().getIdLong()) != null && MusicManager.userMessageToAnswer.get(event.getUser().getIdLong()) == event.getMessageIdLong()) {
            MusicPlayer player = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            if (event.getReactionEmote().getName().equalsIgnoreCase("✅")) {
                List<AudioTrack> tracks = MusicManager.userRequestedSongs.get(event.getUser().getIdLong());
                StringBuilder songs = new StringBuilder();
                int i = player.getListener().getTrackSize();
                int t = 0;
                for (AudioTrack track : tracks) {
                    i++;
                    player.playTrack(track);
                    songs.append("[#").append(i).append("](").append(track.getInfo().uri).append(") - ").append(track.getInfo().title).append("\n");
                    if (songs.length() > 1700) {
                        t++;
                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setTitle("Added part **#" + t + "**");
                        eb.setColor(Helpers.EmbedColor);
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        eb.setDescription(songs);
                        event.getChannel().sendMessage(eb.build()).queue();
                        songs = new StringBuilder();
                    }
                }
                if (t == 0) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Added");
                    eb.setColor(Helpers.EmbedColor);
                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                    eb.setDescription(songs);
                    event.getChannel().sendMessage(eb.build()).queue();
                } else {
                    t++;
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Added part **#" + t + "**");
                    eb.setColor(Helpers.EmbedColor);
                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                    eb.setDescription(songs);
                    event.getChannel().sendMessage(eb.build()).queue();
                }
                event.getChannel().getMessageById(MusicManager.userMessageToAnswer.get(event.getUser().getIdLong())).queue(message -> message.delete().queue());
                MusicManager.userMessageToAnswer.remove(event.getUser().getIdLong());
                MusicManager.userRequestedSongs.remove(event.getUser().getIdLong());
            } else if (event.getReactionEmote().getName().equalsIgnoreCase("❎")) {
                event.getChannel().getMessageById(MusicManager.userMessageToAnswer.get(event.getUser().getIdLong())).queue(message -> message.delete().queue());
                MusicManager.userMessageToAnswer.remove(event.getUser().getIdLong());
                MusicManager.userRequestedSongs.remove(event.getUser().getIdLong());
            }
        }
        if (SPlayCommand.usersFormToReply.get(event.getUser()) != null && SPlayCommand.usersFormToReply.get(event.getUser()).getId().equalsIgnoreCase(event.getMessageId())) {
            MusicPlayer player = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            AudioTrack track;
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Added");
            eb.setFooter(Helpers.getFooterStamp(), null);
            eb.setColor(Helpers.EmbedColor);
            boolean wrongemote = false;
            switch (event.getReactionEmote().getName()) {
                case "\u0031\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(0);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0032\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(1);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0033\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(2);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0034\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(3);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0035\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser()).get(4);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u274E":
                    SPlayCommand.usersFormToReply.remove(event.getUser());
                    SPlayCommand.userChoices.remove(event.getUser());
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.delete().queue());
                    wrongemote = true;
                    break;
                default:
                    wrongemote = true;
                    break;

            }
            if (!wrongemote) {
                event.getChannel().getMessageById(event.getMessageId()).queue((s) -> {
                    if (s.getGuild() != null && s.getGuild().getSelfMember().hasPermission(Permission.MESSAGE_MANAGE))
                        s.clearReactions().queue();
                });
            }
        }
        if (ClearChannelCommand.possibleDeletes.containsKey(event.getGuild().getIdLong())) {
            HashMap<Long, Long> messageChannel = new HashMap<>();
            messageChannel.put(event.getMessageIdLong(), event.getChannel().getIdLong());
            if (ClearChannelCommand.possibleDeletes.values().contains(messageChannel)
                    && ClearChannelCommand.messageUser.get(event.getMessageIdLong()) == event.getUser().getIdLong()
                    && Helpers.hasPerm(event.getGuild().getMember(event.getUser()), "clearChannel", 1)) {
                if (event.getReactionEmote().getEmote() != null)
                    switch (event.getReactionEmote().getEmote().getId()) {
                        case "463250265026330634"://yes
                            TextChannel toDelete = event.getGuild().getTextChannelById(ClearChannelCommand.possibleDeletes.get(event.getGuild().getIdLong()).get(event.getMessageIdLong()));
                            toDelete.createCopy().queue(s -> {
                                guild.getController().modifyTextChannelPositions().selectPosition((TextChannel) s).moveTo(toDelete.getPosition()).queue(done -> {
                                    toDelete.delete().queue();
                                    ((TextChannel) s).sendMessage("**#" + toDelete.getName() + "** has been cleared")
                                            .queue(message ->
                                                    message.delete().queueAfter(5, TimeUnit.SECONDS, null, (failure) -> {
                                                    }));
                                });
                            });

                            ClearChannelCommand.possibleDeletes.get(guild.getIdLong()).remove(messageChannel);
                            HashMap<Long, Long> messageUser = new HashMap<>();
                            messageUser.put(event.getMessageIdLong(), event.getUser().getIdLong());
                            ClearChannelCommand.messageUser.remove(messageUser);
                            break;
                        case "463250264653299713"://no
                            event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.delete().queue());
                            ClearChannelCommand.possibleDeletes.get(guild.getIdLong()).remove(messageChannel);
                            HashMap<Long, Long> messageUser2 = new HashMap<>();
                            messageUser2.put(event.getMessageIdLong(), event.getUser().getIdLong());
                            ClearChannelCommand.messageUser.remove(messageUser2);
                            break;
                        default:
                            break;
                    }
            }
        }
    }
}
