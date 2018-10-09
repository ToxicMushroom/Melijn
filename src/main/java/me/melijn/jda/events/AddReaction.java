package me.melijn.jda.events;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.map.TLongLongMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.ClearChannelCommand;
import me.melijn.jda.commands.music.SPlayCommand;
import me.melijn.jda.music.MusicManager;
import me.melijn.jda.music.MusicPlayer;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddReaction extends ListenerAdapter {

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        if (MusicManager.userMessageToAnswer.containsKey(event.getUser().getIdLong()) && MusicManager.userMessageToAnswer.get(event.getUser().getIdLong()) == event.getMessageIdLong()) {
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
        if (SPlayCommand.usersFormToReply.containsKey(event.getUser().getIdLong()) && SPlayCommand.usersFormToReply.get(event.getUser().getIdLong()).getIdLong() == event.getMessageIdLong()) {
            MusicPlayer player = MusicManager.getManagerInstance().getPlayer(event.getGuild());
            AudioTrack track;
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Added");
            eb.setFooter(Helpers.getFooterStamp(), null);
            eb.setColor(Helpers.EmbedColor);
            boolean wrongemote = false;
            switch (event.getReactionEmote().getName()) {
                case "\u0031\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(0);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0032\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(1);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0033\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(2);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0034\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(3);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u0035\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(4);
                    player.playTrack(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getListener().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue());
                    break;
                case "\u274E":
                    SPlayCommand.usersFormToReply.remove(event.getUser().getIdLong());
                    SPlayCommand.userChoices.remove(event.getUser().getIdLong());
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
            TLongLongMap messageChannel = ClearChannelCommand.possibleDeletes.get(guild.getIdLong());
            if (ClearChannelCommand.messageUser.keySet().contains(event.getMessageIdLong())
                    && ClearChannelCommand.messageUser.get(event.getMessageIdLong()) == event.getUser().getIdLong()
                    && Helpers.hasPerm(event.getGuild().getMember(event.getUser()), "clearChannel", 1)
                    && event.getReactionEmote().getEmote() != null)
                switch (event.getReactionEmote().getEmote().getId()) {
                    case "463250265026330634"://yes
                        TextChannel toDelete = event.getGuild().getTextChannelById(ClearChannelCommand.possibleDeletes.get(event.getGuild().getIdLong()).get(event.getMessageIdLong()));
                        toDelete.createCopy().queue(s -> guild.getController().modifyTextChannelPositions().selectPosition((TextChannel) s).moveTo(toDelete.getPosition()).queue(done -> {
                            toDelete.delete().queue();
                            ((TextChannel) s).sendMessage("**#" + toDelete.getName() + "** has been cleared")
                                    .queue(message ->
                                            message.delete().queueAfter(3, TimeUnit.SECONDS, null, (failure) -> {
                                            }));
                        }));
                        removeMenu(event, guild, messageChannel);
                        break;
                    case "463250264653299713"://no
                        removeMenu(event, guild, messageChannel);
                        break;
                    default:
                        break;
                }
        }
    }

    private void removeMenu(GuildMessageReactionAddEvent event, Guild guild, TLongLongMap messageChannel) {
        event.getChannel().getMessageById(event.getMessageId()).queue(s -> {
            s.delete().queue();
            messageChannel.remove(event.getMessageIdLong());
            ClearChannelCommand.possibleDeletes.put(guild.getIdLong(), messageChannel);
            ClearChannelCommand.messageUser.remove(event.getMessageIdLong());
        });
    }
}
