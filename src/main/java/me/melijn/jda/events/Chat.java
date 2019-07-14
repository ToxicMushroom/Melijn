package me.melijn.jda.events;


import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.Melijn;
import me.melijn.jda.audio.MusicPlayer;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.utils.Embedder;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.audit.AuditLogOption;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.entities.Guild.Ban;
import net.dv8tion.jda.core.events.message.MessageUpdateEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.MiscUtil;
import org.json.JSONObject;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.blub.VerificationType.CODE;

public class Chat extends ListenerAdapter {

    private Set<Long> black = new HashSet<>();
    private long latestId = 0;
    private int latestChanges = 0;
    private Map<Long, Map<Long, Integer>> guildUserVerifyTries = new HashMap<>();
    private final Melijn melijn;


    public Chat(Melijn melijn) {
        this.melijn = melijn;
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()))
            if (event.getGuild() == null &&
                    (event.getMessage().getContentRaw().equalsIgnoreCase(PREFIX) || event.getMessage().getContentRaw().equalsIgnoreCase(event.getJDA().getSelfUser().getAsMention())) &&
                    !event.getAuthor().isBot()) {
                event.getChannel().sendMessage(String.format("Hello there my default prefix is %s and you can view all commands using **%shelp**", PREFIX, PREFIX)).queue();
                return;
            } else return;
        Guild guild = event.getGuild();
        long guildId = guild.getIdLong();
        User author = event.getAuthor();

        if (event.getMember() == null) return;
        if (melijn.getVariables().blockedUserIds.contains(guild.getOwnerIdLong())) return;
        melijn.getHelpers().guildCount = event.getJDA().asBot().getShardManager().getGuildCache().size();

        StringBuilder content = new StringBuilder(event.getMessage().getContentRaw());
        for (Message.Attachment a : event.getMessage().getAttachments()) {
            content.append("\n").append(a.getUrl());
        }
        String finalContent = content.toString();

        melijn.getTaskManager().async(() -> {
            long attachmentsId = melijn.getVariables().attachmentLogChannelCache.get(guildId);
            if (attachmentsId != -1)
                postAttachmentLog(guild, author, event.getChannel(), attachmentsId, event.getMessage().getAttachments());
            if (melijn.getVariables().sdmLogChannelCache.get(guildId) != -1 ||
                    melijn.getVariables().odmLogChannelCache.get(guildId) != -1 ||
                    melijn.getVariables().pmLogChannelCache.get(guildId) != -1 ||
                    melijn.getVariables().fmLogChannelCache.get(guildId) != -1)
                melijn.getMySQL().createMessage(event.getMessageIdLong(), finalContent, author.getIdLong(), guildId, event.getChannel().getIdLong());
        });
        if (event.getMessage().getContentRaw().equalsIgnoreCase(guild.getSelfMember().getAsMention()) &&
                !event.getAuthor().isBot() &&
                guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            String prefix = melijn.getVariables().prefixes.get(guildId);
            event.getChannel().sendMessage(String.format(("Hello there my default prefix is %s " + (prefix.equals(PREFIX) ? "" : String.format("\nThis server has configured %s as the prefix\n", prefix)) + "and you can view all commands using **%shelp**"), PREFIX, prefix)).queue();
        }
        if (!event.getAuthor().isBot() && guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE) &&
                !event.getMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
            filter(event.getMessage());
        }

        if (melijn.getVariables().userMessageToAnswer.containsKey(author.getIdLong())) {
            if (event.getMessage().getContentRaw().equalsIgnoreCase("agree")) {
                MusicPlayer player = melijn.getLava().getAudioLoader().getPlayer(event.getGuild());
                List<AudioTrack> tracks = melijn.getVariables().userRequestedSongs.get(author.getIdLong());
                StringBuilder songs = new StringBuilder();
                int i = player.getTrackManager().getTrackSize();
                int t = 0;
                for (AudioTrack track : tracks) {
                    i++;
                    player.queue(track);
                    songs.append("[#").append(i).append("](").append(track.getInfo().uri).append(") - ").append(track.getInfo().title).append("\n");
                    if (songs.length() > 1700) {
                        t++;
                        EmbedBuilder eb = new Embedder(melijn.getVariables(), event.getGuild());
                        eb.setTitle("Added part **#" + t + "**");
                        eb.setFooter(melijn.getHelpers().getFooterStamp(), melijn.getHelpers().getFooterIcon());
                        eb.setDescription(songs);
                        event.getChannel().sendMessage(eb.build()).queue();
                        songs = new StringBuilder();
                    }
                }
                if (t == 0) {
                    EmbedBuilder eb = new Embedder(melijn.getVariables(), event.getGuild());
                    eb.setTitle("Added");
                    eb.setFooter(melijn.getHelpers().getFooterStamp(), melijn.getHelpers().getFooterIcon());
                    eb.setDescription(songs);
                    event.getChannel().sendMessage(eb.build()).queue();
                } else {
                    t++;
                    EmbedBuilder eb = new Embedder(melijn.getVariables(), event.getGuild());
                    eb.setTitle("Added part **#" + t + "**");
                    eb.setFooter(melijn.getHelpers().getFooterStamp(), melijn.getHelpers().getFooterIcon());
                    eb.setDescription(songs);
                    event.getChannel().sendMessage(eb.build()).queue();
                }
                event.getChannel().getMessageById(melijn.getVariables().userMessageToAnswer.get(author.getIdLong()))
                        .queue(message -> message.delete().queue());
                melijn.getVariables().userMessageToAnswer.remove(author.getIdLong());
                melijn.getVariables().userRequestedSongs.remove(author.getIdLong());

            } else if (event.getMessage().getContentRaw().equalsIgnoreCase("deny")) {
                event.getChannel().getMessageById(melijn.getVariables().userMessageToAnswer.get(author.getIdLong()))
                        .queue(message -> message.delete().queue());
                melijn.getVariables().userMessageToAnswer.remove(author.getIdLong());
                melijn.getVariables().userRequestedSongs.remove(author.getIdLong());
            }
        }

        if (melijn.getVariables().usersFormToReply.containsKey(author.getIdLong())) {
            MusicPlayer player = melijn.getLava().getAudioLoader().getPlayer(event.getGuild());
            AudioTrack track;

            EmbedBuilder eb = new Embedder(melijn.getVariables(), event.getGuild());
            eb.setTitle("Added");
            eb.setFooter(melijn.getHelpers().getFooterStamp(), null);
            switch (event.getMessage().getContentRaw()) {
                case "1":
                    track = melijn.getVariables().userChoices.get(author.getIdLong()).get(0);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");

                    event.getChannel().getMessageById(melijn.getVariables().usersFormToReply.get(author.getIdLong())).queue(s ->
                            s.editMessage(eb.build()).queue()
                    );
                    break;
                case "2":
                    track = melijn.getVariables().userChoices.get(author.getIdLong()).get(1);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(melijn.getVariables().usersFormToReply.get(author.getIdLong())).queue(s ->
                            s.editMessage(eb.build()).queue()
                    );
                    break;
                case "3":
                    track = melijn.getVariables().userChoices.get(author.getIdLong()).get(2);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(melijn.getVariables().usersFormToReply.get(author.getIdLong())).queue(s ->
                            s.editMessage(eb.build()).queue());
                    break;
                case "4":
                    track = melijn.getVariables().userChoices.get(author.getIdLong()).get(3);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(melijn.getVariables().usersFormToReply.get(author.getIdLong())).queue(s ->
                            s.editMessage(eb.build()).queue()
                    );
                    break;
                case "5":
                    track = melijn.getVariables().userChoices.get(author.getIdLong()).get(4);
                    player.queue(track);
                    eb.setDescription("**[" + track.getInfo().title + "](" + track.getInfo().uri + ")** is queued at position **#" + player.getTrackManager().getTrackSize() + "**");
                    event.getChannel().getMessageById(melijn.getVariables().usersFormToReply.get(author.getIdLong())).queue(s ->
                            s.editMessage(eb.build()).queue()
                    );
                    break;
                case "cancel":
                    melijn.getVariables().usersFormToReply.remove(author.getIdLong());
                    melijn.getVariables().userChoices.remove(author.getIdLong());
                    event.getChannel().getMessageById(melijn.getVariables().usersFormToReply.get(author.getIdLong())).queue(s ->
                            s.delete().queue()
                    );
                    break;
                default:
                    break;

            }
        }

        if (melijn.getVariables().verificationChannelsCache.get(guildId) == event.getChannel().getIdLong()) {
            if (!event.getMember().hasPermission(event.getChannel(), Permission.MANAGE_CHANNEL))
                event.getMessage().delete().reason("Verification Channel").queue(
                        s -> melijn.getVariables().botDeletedMessages.add(event.getMessageIdLong()),
                        failed -> {
                        }
                );
            if (melijn.getVariables().verificationCodeCache.get(guildId) == null &&
                    melijn.getVariables().verificationTypes.get(guildId) == CODE) {
                return;
            }
            String code = melijn.getVariables().verificationTypes.get(guildId) == CODE ?
                    melijn.getVariables().verificationCodeCache.get(guildId) :
                    String.valueOf(melijn.getVariables().unVerifiedGuildMembersCache.get(guildId).get(event.getAuthor().getIdLong()));

            if (event.getMessage().getContentRaw().equalsIgnoreCase(code)) {
                melijn.getHelpers().verify(guild, event.getAuthor());
                removeMemberFromTriesCache(event);
            } else if (melijn.getVariables().verificationThresholdCache.get(guildId) != 0) {
                if (guildUserVerifyTries.containsKey(guildId)) {
                    if (guildUserVerifyTries.get(guildId).containsKey(guildId)) {
                        Map<Long, Integer> userTriesBuffer = guildUserVerifyTries.get(guildId);
                        userTriesBuffer.put(event.getAuthor().getIdLong(), userTriesBuffer.get(guildId) + 1);
                        guildUserVerifyTries.put(guildId, userTriesBuffer);
                    } else {
                        Map<Long, Integer> userTriesBuffer = guildUserVerifyTries.get(guildId);
                        userTriesBuffer.put(event.getAuthor().getIdLong(), 1);
                        guildUserVerifyTries.put(guildId, userTriesBuffer);
                    }
                } else {
                    Map<Long, Integer> userTriesBuffer = new HashMap<>();
                    userTriesBuffer.put(event.getAuthor().getIdLong(), 1);
                    guildUserVerifyTries.put(guildId, userTriesBuffer);
                }
                if (guildUserVerifyTries.get(guildId).get(event.getAuthor().getIdLong()).equals(melijn.getVariables().verificationThresholdCache.get(guildId))) {
                    if (event.getGuild().getSelfMember().canInteract(event.getMember()))
                        event.getGuild().getController().kick(event.getMember()).reason("Failed verification").queue();
                    removeMemberFromTriesCache(event);
                }
            } else {
                event.getMessage().delete().reason("Verification Channel").queue(
                        s -> melijn.getVariables().botDeletedMessages.add(event.getMessageIdLong()),
                        failed -> {
                        }
                );
            }
        }
    }

    private void postAttachmentLog(Guild guild, User author, TextChannel origin, long attachmentsId, List<Message.Attachment> attachments) {
        TextChannel textChannel = guild.getTextChannelById(attachmentsId);
        if (author.isBot() || textChannel == null || !guild.getSelfMember().hasPermission(textChannel, Permission.MESSAGE_WRITE))
            return;

        for (Message.Attachment attachment : attachments) {
            textChannel.sendMessage(new EmbedBuilder()
                    .setTitle("Attachment sent in #" + origin.getName())
                    .setColor(Color.decode("#DC143C"))
                    .setDescription("```LDIF" +
                            "\nSenderID: " + author.getId() +
                            "\nMessageID: " + attachment.getId() +
                            "\nURL: " + attachment.getUrl() +
                            "```\n" +
                            "**Attachment**: [Click to view](" + attachment.getUrl() + (attachment.isImage() ? "?size=2048" : "") + ")")
                    .setImage(attachment.isImage() ? attachment.getUrl() + "?size=2048" : null)
                    .setFooter("Sent by " + author.getName() + "#" + author.getDiscriminator(), author.getEffectiveAvatarUrl())
                    .build()
            ).queue();
        }
    }


    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getMember() == null) return;
        long messageId = event.getMessageIdLong();

        Guild guild = event.getGuild();
        User author = event.getAuthor();
        JSONObject oMessage = melijn.getMySQL().getMessageObject(messageId);

        //Redo filter
        if (guild.getSelfMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE) &&
                !event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE)) {
            filter(event.getMessage());
        }
        melijn.getMySQL().updateMessage(event.getMessage());

        //Logging part
        TextChannel emChannel = event.getGuild().getTextChannelById(melijn.getVariables().emLogChannelCache.get(guild.getIdLong()));
        if (emChannel == null || oMessage.length() == 0 || !guild.getSelfMember().hasPermission(emChannel, Permission.MESSAGE_WRITE))
            return;

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Message edited in #" + event.getTextChannel().getName() + " ".repeat(100).substring(0, 45 + event.getAuthor().getName().length()) + "\u200B");
        eb.setThumbnail(event.getAuthor().getEffectiveAvatarUrl());
        eb.setColor(Color.decode("#A1DAC3"));

        int nLength = event.getMessage().getContentRaw().length();
        int oLength = oMessage.getString("content").length();
        if (oLength + nLength < 1800) {
            eb.setDescription("```LDIF" +
                    "\nSender: " + author.getName() + "#" + author.getDiscriminator() +
                    "\nMessage before: " + oMessage.getString("content").replaceAll("`", "´").replaceAll("\n", " ") +
                    "\nMessage after: " + event.getMessage().getContentRaw().replaceAll("`", "´").replaceAll("\n", " ") +
                    "\nSenderID: " + author.getId() +
                    "\nSent Time: " + melijn.getMessageHelper().millisToDate(oMessage.getLong("sentTime")) +
                    "\nEdited Time: " + melijn.getMessageHelper().millisToDate(System.currentTimeMillis()) +
                    "```");
            emChannel.sendMessage(eb.build()).queue();
        } else if (oLength < 1900 && nLength < 1800) {
            eb.setDescription("```LDIF" +
                    "\nSender: " + author.getName() + "#" + author.getDiscriminator() +
                    "\nMessage before: " + oMessage.getString("content").replaceAll("`", "´").replaceAll("\n", " ") +
                    "```");
            emChannel.sendMessage(eb.build()).queue();
            eb.setTitle(null);
            eb.setThumbnail(null);
            eb.setDescription("```LDIF" +
                    "\nMessage after: " + event.getMessage().getContentRaw().replaceAll("`", "´").replaceAll("\n", " ") +
                    "\nSenderID: " + author.getId() +
                    "\nSent Time: " + melijn.getMessageHelper().millisToDate(oMessage.getLong("sentTime")) +
                    "\nEdited Time: " + melijn.getMessageHelper().millisToDate(System.currentTimeMillis()) +
                    "```");
            emChannel.sendMessage(eb.build()).queue();
        } else {
            eb.setDescription("```LDIF" +
                    "\nSender: " + author.getName() + "#" + author.getDiscriminator() +
                    "\nSenderID: " + author.getId() +
                    "\nSent Time: " + melijn.getMessageHelper().millisToDate(oMessage.getLong("sentTime")) +
                    "\nEdited Time: " + melijn.getMessageHelper().millisToDate(System.currentTimeMillis()) +
                    "```");
            emChannel.sendMessage(eb.build()).queue();
            eb.setThumbnail(null);
            eb.setTitle(null);
            eb.setDescription("```LDIF\nMessage before: " + oMessage.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "```");
            emChannel.sendMessage(eb.build()).queue();
            eb.setDescription("```LDIF\nMessage after: " + event.getMessage().getContentRaw().replaceAll("`", "´").replaceAll("\n", " ") + "```");
            emChannel.sendMessage(eb.build()).queue();
        }
    }

    private void addPositions(String message, Map<Integer, Integer> deniedPositions, List<String> deniedList) {
        for (String toFind : deniedList) {
            Pattern word = Pattern.compile(Pattern.quote(toFind.toLowerCase()));
            Matcher match = word.matcher(message.toLowerCase());
            while (match.find()) {
                deniedPositions.put(match.start(), match.end());
            }
        }
    }

    private void removeMemberFromTriesCache(GuildMessageReceivedEvent event) {
        if (guildUserVerifyTries.containsKey(event.getGuild().getIdLong())) {
            Map<Long, Integer> memberTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
            memberTriesBuffer.remove(event.getAuthor().getIdLong());
            if (memberTriesBuffer.size() > 0) guildUserVerifyTries.put(event.getGuild().getIdLong(), memberTriesBuffer);
            else guildUserVerifyTries.remove(event.getGuild().getIdLong());
        }
    }


    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (event.getGuild() == null || melijn.getVariables().blockedGuildIds.contains(event.getGuild().getIdLong()))
            return;
        if (melijn.getVariables().blockedUserIds.contains(event.getGuild().getOwnerIdLong())) return;
        if (melijn.getHelpers().lastRunTimer1 < (System.currentTimeMillis() - 10_000 * 2) && melijn.getHelpers().lastRunTimer1 != -1)
            melijn.getHelpers().startTimer(event.getJDA(), 1);
        if (melijn.getHelpers().lastRunTimer2 < (System.currentTimeMillis() - 120_000 * 2) && melijn.getHelpers().lastRunTimer2 != -1)
            melijn.getHelpers().startTimer(event.getJDA(), 2);
        if (melijn.getHelpers().lastRunTimer3 < (System.currentTimeMillis() - 1_800_000 * 2) && melijn.getHelpers().lastRunTimer3 != -1)
            melijn.getHelpers().startTimer(event.getJDA(), 3);
        Guild guild = event.getGuild();
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS) &&
                (melijn.getVariables().sdmLogChannelCache.get(guild.getIdLong()) != -1 ||
                        melijn.getVariables().odmLogChannelCache.get(guild.getIdLong()) != -1 ||
                        melijn.getVariables().pmLogChannelCache.get(guild.getIdLong()) != -1 ||
                        melijn.getVariables().fmLogChannelCache.get(guild.getIdLong()) != -1)) {
            JSONObject message = melijn.getMySQL().getMessageObject(event.getMessageIdLong());
            if (!message.keySet().contains("authorId"))
                return;
            event.getJDA().retrieveUserById(message.getLong("authorId")).queue(author -> {
                if (author == null || black.contains(author.getIdLong()))
                    return;
                if (!event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS))
                    return;
                guild.getBanList().queue(bans -> {
                    if (bans.stream().map(Ban::getUser).anyMatch(author::equals)) {
                        black.add(author.getIdLong());
                        return;
                    }
                    doMessageDeleteChecks(event, guild, message, author);
                });
                melijn.getMySQL().executeUpdate("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
            });
        }
    }

    private void doMessageDeleteChecks(GuildMessageDeleteEvent event, Guild guild, JSONObject message, User author) {
        guild.getAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).queue(auditLogEntries -> {
            if (auditLogEntries.size() == 0) return;
            AuditLogEntry auditLogEntry = auditLogEntries.get(0);
            String t = auditLogEntry.getOption(AuditLogOption.COUNT);
            if (t == null) {
                return;
            }
            boolean sameAsLast = latestId == auditLogEntry.getIdLong() && latestChanges != Integer.valueOf(t);
            latestId = auditLogEntry.getIdLong();
            latestChanges = Integer.valueOf(t);
            ZonedDateTime deletionTime = MiscUtil.getCreationTime(auditLogEntry.getIdLong()).toZonedDateTime();
            ZonedDateTime now = OffsetDateTime.now().atZoneSameInstant(deletionTime.getOffset());
            deletionTime = deletionTime.plusSeconds(1).plusNanos((event.getJDA().getPing() * 1_000_000));

            boolean split = false;
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("Message deleted in #" + event.getChannel().getName() + " ".repeat(100).substring(0, 45 + author.getName().length()) + "\u200B");
            eb.setThumbnail(author.getEffectiveAvatarUrl());
            eb.setColor(Color.decode("#000001"));
            if (message.getString("content").length() > 1850) {
                eb.setDescription("```LDIF" +
                        "\nSender: " + author.getName() + "#" + author.getDiscriminator() +
                        "\nMessage part 1: " + message.getString("content").substring(0, 1500) +
                        "```");
                split = true;
            } else {
                eb.setDescription("```LDIF" +
                        "\nSender: " + author.getName() + "#" + author.getDiscriminator() +
                        "\nMessage: " + message.getString("content").replaceAll("`", "´").replaceAll("\n", " ") +
                        "\nSenderID: " + author.getId() +
                        "\nSent Time: " + melijn.getMessageHelper().millisToDate(message.getLong("sentTime")) +
                        "```");
            }

            if (melijn.getVariables().filteredMessageDeleteCause.keySet().contains(event.getMessageIdLong()) && guild.getTextChannelById(melijn.getVariables().fmLogChannelCache.get(guild.getIdLong())) != null) {
                // FILTERED
                eb.setColor(Color.ORANGE);
                TextChannel fmLogChannel = guild.getTextChannelById(melijn.getVariables().fmLogChannelCache.get(guild.getIdLong()));
                if (!event.getGuild().getSelfMember().hasPermission(fmLogChannel, Permission.MESSAGE_WRITE)) {
                    melijn.getMySQL().removeChannel(guild.getIdLong(), ChannelType.FM_LOG);
                    return;
                }
                sendSplitIfNeeded(message, author, split, eb, fmLogChannel);
                eb.addField("Detected: ", "`" + melijn.getVariables().filteredMessageDeleteCause.get(event.getMessageIdLong()).replaceAll("`", "´") + "`", false);
                User bot = event.getJDA().getSelfUser();
                eb.setFooter("Deleted by: " + bot.getName() + "#" + bot.getDiscriminator(), bot.getEffectiveAvatarUrl());
                fmLogChannel.sendMessage(eb.build()).queue();
                melijn.getVariables().filteredMessageDeleteCause.remove(event.getMessageIdLong());
            } else if (melijn.getVariables().purgedMessageDeleter.containsKey(event.getMessageIdLong()) && guild.getTextChannelById(melijn.getVariables().pmLogChannelCache.get(guild.getIdLong())) != null) {
                // PURGED
                eb.setColor(Color.decode("#551A8B"));
                User purger = event.getJDA().asBot().getShardManager().getUserById(melijn.getVariables().purgedMessageDeleter.get(event.getMessageIdLong()));
                TextChannel pmLogChannel = guild.getTextChannelById(melijn.getVariables().pmLogChannelCache.get(guild.getIdLong()));
                if (!event.getGuild().getSelfMember().hasPermission(pmLogChannel, Permission.MESSAGE_WRITE)) {
                    melijn.getMySQL().removeChannel(guild.getIdLong(), ChannelType.PM_LOG);
                    return;
                }
                sendSplitIfNeeded(message, author, split, eb, pmLogChannel);
                if (purger != null)
                    eb.setFooter("Purged by: " + purger.getName() + "#" + purger.getDiscriminator(), purger.getEffectiveAvatarUrl());
                pmLogChannel.sendMessage(eb.build()).queue();
                melijn.getVariables().purgedMessageDeleter.remove(event.getMessageIdLong());
            } else if (melijn.getVariables().botDeletedMessages.remove(event.getMessageIdLong())) {
                // DELETED BY BOT
                User deleter = event.getJDA().getSelfUser();
                log(guild, author, eb, deleter, message, split);
            } else if (now.toInstant().toEpochMilli() - deletionTime.toInstant().toEpochMilli() < 1000) {
                // ODM
                User deleter = auditLogEntry.getUser();
                log(guild, author, eb, deleter, message, split);
            } else {
                // SDM
                User deleter = sameAsLast ? auditLogEntry.getUser() : null;
                if (deleter == null) {
                    Member member = guild.getMemberById(melijn.getMySQL().getMessageAuthorId(event.getMessageIdLong()));
                    if (member == null) return;
                    deleter = member.getUser();
                }
                log(guild, author, eb, deleter, message, split);
            }

        });
    }

    private void sendSplitIfNeeded(JSONObject message, User author, boolean split, EmbedBuilder eb, TextChannel channel) {
        if (split) {
            eb.setTitle(eb.build().getTitle() + " part 1");
            channel.sendMessage(eb.build()).queue();
            eb.setDescription("```LDIF" +
                    "\npart 2: " + message.getString("content").substring(1850) +
                    "\nSenderID: " + author.getId() +
                    "\nSent Time: " + melijn.getMessageHelper().millisToDate(message.getLong("sentTime")) +
                    "```"
            );
            eb.setTitle("part 2");
            eb.setThumbnail("https://melijn.com/files/u/03-09-2018--09.16-29s.png");
        }
    }

    private void log(Guild guild, User author, EmbedBuilder eb, User deleter, JSONObject message, boolean split) {
        if (deleter == null) return;
        TextChannel sdmChannel = guild.getTextChannelById(melijn.getVariables().sdmLogChannelCache.get(guild.getIdLong()));
        TextChannel odmChannel = guild.getTextChannelById(melijn.getVariables().odmLogChannelCache.get(guild.getIdLong()));
        if (sdmChannel == null || !guild.getSelfMember().hasPermission(sdmChannel, Permission.MESSAGE_WRITE)) {
            melijn.getMySQL().removeChannel(guild.getIdLong(), ChannelType.SDM_LOG);
            melijn.getVariables().sdmLogChannelCache.invalidate(guild.getIdLong());
        }
        if (odmChannel == null || !guild.getSelfMember().hasPermission(odmChannel, Permission.MESSAGE_WRITE)) {
            melijn.getMySQL().removeChannel(guild.getIdLong(), ChannelType.ODM_LOG);
            melijn.getVariables().odmLogChannelCache.invalidate(guild.getIdLong());
        }
        if (split) {
            eb.setTitle(eb.build().getTitle() + " part 1");
            if (author.equals(deleter) && sdmChannel != null && guild.getSelfMember().hasPermission(sdmChannel, Permission.MESSAGE_WRITE)) {
                sdmChannel.sendMessage(eb.build()).queue();
            } else if (odmChannel != null && guild.getSelfMember().hasPermission(sdmChannel, Permission.MESSAGE_WRITE)) {
                odmChannel.sendMessage(eb.build()).queue();
            }
            eb.setTitle("part 2");
            eb.setThumbnail("https://melijn.com/files/u/03-09-2018--09.16-29s.png");
            eb.setDescription("```LDIF" +
                    "\npart 2: " + message.getString("content").substring(1500) +
                    "\nSenderID: " + author.getId() +
                    "\nSent Time: " + melijn.getMessageHelper().millisToDate(message.getLong("sentTime")) +
                    "```"
            );
        }
        eb.setFooter("Deleted by: " + deleter.getName() + "#" + deleter.getDiscriminator(), deleter.getEffectiveAvatarUrl());
        if (author.equals(deleter) && sdmChannel != null && guild.getSelfMember().hasPermission(sdmChannel, Permission.MESSAGE_WRITE)) {
            sdmChannel.sendMessage(eb.build()).queue();
        } else if (odmChannel != null && guild.getSelfMember().hasPermission(odmChannel, Permission.MESSAGE_WRITE)) {
            odmChannel.sendMessage(eb.build()).queue();
        }
    }

    private void filter(Message msg) {
        melijn.getTaskManager().async(() -> {
            String message = msg.getContentRaw();
            StringBuilder detectedWord = new StringBuilder();

            List<String> deniedList = melijn.getMySQL().getFilters(msg.getGuild().getIdLong(), "denied");
            if (deniedList.size() == 0) return;
            List<String> allowedList = melijn.getMySQL().getFilters(msg.getGuild().getIdLong(), "allowed");

            AtomicBoolean ranOnce = new AtomicBoolean(false);

            if (allowedList.size() == 0) {
                deniedList.forEach(deniedWord -> {
                    if (message.toLowerCase().contains(deniedWord.toLowerCase())) {
                        detectedWord.append(ranOnce.get() ? ", " : "").append(deniedWord);
                        ranOnce.set(true);
                    }
                });

            } else {
                Map<Integer, Integer> deniedPositions = new HashMap<>();
                Map<Integer, Integer> allowedPositions = new HashMap<>();
                addPositions(message, deniedPositions, deniedList);
                addPositions(message, allowedPositions, allowedList);

                if (allowedPositions.size() > 0 && deniedPositions.size() > 0) {
                    for (int beginDenied : deniedPositions.keySet()) {
                        int endDenied = deniedPositions.get(beginDenied);
                        for (int beginAllowed : allowedPositions.keySet()) {
                            int endAllowed = allowedPositions.get(beginAllowed);

                            if (beginDenied > beginAllowed && endDenied < endAllowed) continue;
                            detectedWord.append(message, beginDenied, endDenied);
                        }
                    }
                } else if (deniedPositions.size() > 0) {
                    for (int beginDenied : deniedPositions.keySet()) {
                        int endDenied = deniedPositions.get(beginDenied);
                        detectedWord.append(ranOnce.get() ? ", " : "").append(message, beginDenied, endDenied);
                        ranOnce.set(true);
                    }
                }
            }
            if (detectedWord.length() > 0) {
                melijn.getVariables().filteredMessageDeleteCause.put(msg.getIdLong(), detectedWord.substring(0, detectedWord.length()));
                msg.delete().reason("Use of prohibited words").queue(
                        success -> {
                        },
                        failure -> {
                        }
                );
            }
        });
    }
}
