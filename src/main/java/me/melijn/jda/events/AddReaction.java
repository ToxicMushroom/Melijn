package me.melijn.jda.events;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import gnu.trove.map.TLongLongMap;
import gnu.trove.map.TLongObjectMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.audio.AudioLoader;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.ClearChannelCommand;
import me.melijn.jda.commands.management.SetSelfRoleChannelCommand;
import me.melijn.jda.commands.music.SPlayCommand;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.utils.Embedder;
import me.melijn.jda.utils.TaskScheduler;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AddReaction extends ListenerAdapter {

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        Guild guild = event.getGuild();
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        if (AudioLoader.userMessageToAnswer.containsKey(event.getUser().getIdLong()) && AudioLoader.userMessageToAnswer.get(event.getUser().getIdLong()) == event.getMessageIdLong()) {
            MusicPlayer player = AudioLoader.getManagerInstance().getPlayer(event.getGuild());
            if (event.getReactionEmote().getName().equalsIgnoreCase("✅")) {
                List<AudioTrack> tracks = AudioLoader.userRequestedSongs.get(event.getUser().getIdLong());
                StringBuilder songs = new StringBuilder();
                int i = player.getTrackManager().getTrackSize();
                int t = 0;
                for (AudioTrack track : tracks) {
                    i++;
                    player.queue(track);
                    songs.append("[#").append(i).append("](").append(track.getInfo().uri).append(") - ").append(track.getInfo().title).append("\n");
                    if (songs.length() > 1700) {
                        t++;
                        EmbedBuilder eb = new Embedder(event.getGuild());
                        eb.setTitle("Added part **#" + t + "**");
                        eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                        eb.setDescription(songs);
                        event.getChannel().sendMessage(eb.build()).queue();
                        songs = new StringBuilder();
                    }
                }
                if (t == 0) {
                    EmbedBuilder eb = new Embedder(event.getGuild());
                    eb.setTitle("Added");
                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                    eb.setDescription(songs);
                    event.getChannel().sendMessage(eb.build()).queue();
                } else {
                    t++;
                    EmbedBuilder eb = new Embedder(event.getGuild());
                    eb.setTitle("Added part **#" + t + "**");
                    eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                    eb.setDescription(songs);
                    event.getChannel().sendMessage(eb.build()).queue();
                }
                event.getChannel().getMessageById(AudioLoader.userMessageToAnswer.get(event.getUser().getIdLong())).queue(message -> message.delete().queue(), f -> LoggerFactory.getLogger(this.getClass()).info("72"));
                AudioLoader.userMessageToAnswer.remove(event.getUser().getIdLong());
                AudioLoader.userRequestedSongs.remove(event.getUser().getIdLong());
            } else if (event.getReactionEmote().getName().equalsIgnoreCase("❎")) {
                event.getChannel().getMessageById(AudioLoader.userMessageToAnswer.get(event.getUser().getIdLong())).queue(message -> message.delete().queue(), f -> LoggerFactory.getLogger(this.getClass()).info("76"));
                AudioLoader.userMessageToAnswer.remove(event.getUser().getIdLong());
                AudioLoader.userRequestedSongs.remove(event.getUser().getIdLong());
            }
        }
        if (SPlayCommand.usersFormToReply.containsKey(event.getUser().getIdLong()) && SPlayCommand.usersFormToReply.get(event.getUser().getIdLong()).getIdLong() == event.getMessageIdLong()) {
            MusicPlayer player = AudioLoader.getManagerInstance().getPlayer(event.getGuild());
            AudioTrack track;
            EmbedBuilder eb = new Embedder(event.getGuild());
            eb.setTitle("Added");
            eb.setFooter(Helpers.getFooterStamp(), null);
            boolean wrongemote = false;
            switch (event.getReactionEmote().getName()) {
                case "\u0031\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(0);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue(), f -> LoggerFactory.getLogger(this.getClass()).error("93"));
                    break;
                case "\u0032\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(1);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue(), f -> LoggerFactory.getLogger(this.getClass()).error("99"));
                    break;
                case "\u0033\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(2);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue(), f -> LoggerFactory.getLogger(this.getClass()).error("105"));
                    break;
                case "\u0034\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(3);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue(), f -> LoggerFactory.getLogger(this.getClass()).error("111"));
                    break;
                case "\u0035\u20E3":
                    track = SPlayCommand.userChoices.get(event.getUser().getIdLong()).get(4);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.editMessage(eb.build()).queue(), f -> LoggerFactory.getLogger(this.getClass()).error("117"));
                    break;
                case "\u274E":
                    SPlayCommand.usersFormToReply.remove(event.getUser().getIdLong());
                    SPlayCommand.userChoices.remove(event.getUser().getIdLong());
                    event.getChannel().getMessageById(event.getMessageId()).queue(s -> s.delete().queue(), f -> LoggerFactory.getLogger(this.getClass()).error("122"));
                    wrongemote = true;
                    break;
                default:
                    wrongemote = true;
                    break;

            }
            if (!wrongemote) {
                event.getChannel().getMessageById(event.getMessageId()).queue((s) -> {
                    if (s.getGuild() != null && s.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE))
                        s.clearReactions().queue();
                }, f -> LoggerFactory.getLogger(this.getClass()).error("133"));
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
        if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) &&
                SetSelfRoleChannelCommand.selfRolesChannel.getUnchecked(guild.getIdLong()) == event.getChannel().getIdLong())
            TaskScheduler.async(() -> {
                TLongObjectMap<String> roles = Melijn.mySQL.getSelfRoles(guild.getIdLong());
                roles.forEachEntry((key, value) -> {
                    if (!value.equals(event.getReactionEmote().getId()) && !value.equals(event.getReactionEmote().getName()))
                        return true;
                    Role role = guild.getRoleCache().getElementById(key);
                    if (role == null) return true;

                    guild.getController().addSingleRoleToMember(event.getMember(), role).reason("SelfRole added").queue();
                    return false;
                });
            });
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        Guild guild = event.getGuild();
        if (guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES) &&
                SetSelfRoleChannelCommand.selfRolesChannel.getUnchecked(guild.getIdLong()) == event.getChannel().getIdLong())
            TaskScheduler.async(() -> {
                TLongObjectMap<String> roles = Melijn.mySQL.getSelfRoles(guild.getIdLong());
                roles.forEachEntry((key, value) -> {
                    if (!value.equals(event.getReactionEmote().getId()) && !value.equals(event.getReactionEmote().getName()))
                        return true;
                    Role role = guild.getRoleCache().getElementById(key);
                    if (role == null) return true;

                    guild.getController().removeSingleRoleFromMember(event.getMember(), role).reason("SelfRole added").queue();
                    return false;
                });
            });
    }

    private void removeMenu(GuildMessageReactionAddEvent event, Guild guild, TLongLongMap messageChannel) {
        event.getChannel().getMessageById(event.getMessageId()).queue(s -> {
            s.delete().queue();
            messageChannel.remove(event.getMessageIdLong());
            ClearChannelCommand.possibleDeletes.put(guild.getIdLong(), messageChannel);
            ClearChannelCommand.messageUser.remove(event.getMessageIdLong());
        }, f -> LoggerFactory.getLogger(this.getClass()).error("203"));
    }
}
