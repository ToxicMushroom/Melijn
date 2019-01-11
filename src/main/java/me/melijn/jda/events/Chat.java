package me.melijn.jda.events;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TLongIntMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TLongIntHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import me.melijn.jda.Helpers;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.ChannelType;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
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
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.melijn.jda.Melijn.PREFIX;
import static me.melijn.jda.blub.VerificationType.CODE;

public class Chat extends ListenerAdapter {

    private List<Long> black = new ArrayList<>();
    private MySQL mySQL = Melijn.mySQL;
    private long latestId = 0;
    private int latestChanges = 0;
    private TLongObjectMap<TLongIntMap> guildUserVerifyTries = new TLongObjectHashMap<>();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
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
        if (EvalCommand.userBlackList.contains(guild.getOwnerIdLong())) return;
        Helpers.guildCount = event.getJDA().asBot().getShardManager().getGuildCache().size();

        StringBuilder content = new StringBuilder(event.getMessage().getContentRaw());
        for (Message.Attachment a : event.getMessage().getAttachments()) {
            content.append("\n").append(a.getUrl());
        }
        String finalContent = content.toString();

        TaskScheduler.async(() -> {
            if (SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guildId) != -1 ||
                    SetLogChannelCommand.odmLogChannelCache.getUnchecked(guildId) != -1 ||
                    SetLogChannelCommand.pmLogChannelCache.getUnchecked(guildId) != -1 ||
                    SetLogChannelCommand.fmLogChannelCache.getUnchecked(guildId) != -1)
                mySQL.createMessage(event.getMessageIdLong(), finalContent, author.getIdLong(), guildId, event.getChannel().getIdLong());
        });
        if (event.getMessage().getContentRaw().equalsIgnoreCase(guild.getSelfMember().getAsMention()) && guild.getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_WRITE)) {
            String prefix = SetPrefixCommand.prefixes.getUnchecked(guildId);
            event.getChannel().sendMessage(String.format(("Hello there my default prefix is %s " + (prefix.equals(PREFIX) ? "" : String.format("\nThis server has configured %s as the prefix\n", prefix)) + "and you can view all commands using **%shelp**"), PREFIX, prefix)).queue();
        }
        if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            filter(event.getMessage());
        }

        if (SetVerificationChannelCommand.verificationChannelsCache.getUnchecked(guildId) == event.getChannel().getIdLong()) {
            if (!event.getMember().hasPermission(event.getChannel(), Permission.MANAGE_CHANNEL))
                event.getMessage().delete().reason("Verification Channel").queue(
                        s -> MessageHelper.botDeletedMessages.add(event.getMessageIdLong()),
                        failed -> {}
                );
            if (SetVerificationCodeCommand.verificationCodeCache.getUnchecked(guildId) == null &&
                    SetVerificationTypeCommand.verificationTypes.getUnchecked(guildId) == CODE) {
                return;
            }
            String code = SetVerificationTypeCommand.verificationTypes.getUnchecked(guildId) == CODE ?
                    SetVerificationCodeCommand.verificationCodeCache.getUnchecked(guildId) :
                    String.valueOf(JoinLeave.unVerifiedGuildMembersCache.getUnchecked(guildId).get(event.getAuthor().getIdLong()));

            if (event.getMessage().getContentRaw().equalsIgnoreCase(code)) {
                JoinLeave.verify(guild, event.getAuthor());
                removeMemberFromTriesCache(event);
            } else if (SetVerificationThresholdCommand.verificationThresholdCache.getUnchecked(guildId) != 0) {
                if (guildUserVerifyTries.containsKey(guildId)) {
                    if (guildUserVerifyTries.get(guildId).containsKey(guildId)) {
                        TLongIntMap userTriesBuffer = guildUserVerifyTries.get(guildId);
                        userTriesBuffer.put(event.getAuthor().getIdLong(), userTriesBuffer.get(guildId) + 1);
                        guildUserVerifyTries.put(guildId, userTriesBuffer);
                    } else {
                        TLongIntMap userTriesBuffer = guildUserVerifyTries.get(guildId);
                        userTriesBuffer.put(event.getAuthor().getIdLong(), 1);
                        guildUserVerifyTries.put(guildId, userTriesBuffer);
                    }
                } else {
                    TLongIntMap userTriesBuffer = new TLongIntHashMap();
                    userTriesBuffer.put(event.getAuthor().getIdLong(), 1);
                    guildUserVerifyTries.put(guildId, userTriesBuffer);
                }
                if (guildUserVerifyTries.get(guildId).get(event.getAuthor().getIdLong()) == SetVerificationThresholdCommand.verificationThresholdCache.getUnchecked(guildId)) {
                    if (event.getGuild().getSelfMember().canInteract(event.getMember()))
                        event.getGuild().getController().kick(event.getMember()).reason("Failed verification").queue();
                    removeMemberFromTriesCache(event);
                }
            } else {
                event.getMessage().delete().reason("Verification Channel").queue(
                        s -> MessageHelper.botDeletedMessages.add(event.getMessageIdLong()),
                        failed -> {}
                );
            }
        }
    }


    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        long messageId = event.getMessageIdLong();
        Guild guild = event.getGuild();
        User author = event.getAuthor();
        JSONObject oMessage = Melijn.mySQL.getMessageObject(messageId);
        if (guild.getSelfMember().hasPermission(Permission.MESSAGE_MANAGE) && !event.getMember().hasPermission(Permission.MESSAGE_MANAGE)) {
            LoggerFactory.getLogger(this.getClass().getName());
            filter(event.getMessage());
        }
        Melijn.mySQL.updateMessage(event.getMessage());
        TextChannel emChannel = event.getGuild().getTextChannelById(SetLogChannelCommand.emLogChannelCache.getUnchecked(guild.getIdLong()));
        if (emChannel == null || oMessage.isEmpty() || !emChannel.getGuild().getSelfMember().hasPermission(emChannel, Permission.MESSAGE_WRITE))
            return;


        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Message edited in #" + event.getTextChannel().getName() + MessageHelper.spaces.substring(0, 45 + event.getAuthor().getName().length()) + "\u200B");
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
                    "\nSent Time: " + MessageHelper.millisToDate(oMessage.getLong("sentTime")) +
                    "\nEdited Time: " + MessageHelper.millisToDate(System.currentTimeMillis()) +
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
                    "\nSent Time: " + MessageHelper.millisToDate(oMessage.getLong("sentTime")) +
                    "\nEdited Time: " + MessageHelper.millisToDate(System.currentTimeMillis()) +
                    "```");
            emChannel.sendMessage(eb.build()).queue();
        } else {
            eb.setDescription("```LDIF" +
                    "\nSender: " + author.getName() + "#" + author.getDiscriminator() +
                    "\nSenderID: " + author.getId() +
                    "\nSent Time: " + MessageHelper.millisToDate(oMessage.getLong("sentTime")) +
                    "\nEdited Time: " + MessageHelper.millisToDate(System.currentTimeMillis()) +
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

    private void addPositions(String message, TIntIntMap deniedPositions, List<String> deniedList) {
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
            TLongIntMap memberTriesBuffer = guildUserVerifyTries.get(event.getGuild().getIdLong());
            memberTriesBuffer.remove(event.getAuthor().getIdLong());
            if (memberTriesBuffer.size() > 0) guildUserVerifyTries.put(event.getGuild().getIdLong(), memberTriesBuffer);
            else guildUserVerifyTries.remove(event.getGuild().getIdLong());
        }
    }


    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (event.getGuild() == null || EvalCommand.serverBlackList.contains(event.getGuild().getIdLong()))
            return;
        if (EvalCommand.userBlackList.contains(event.getGuild().getOwnerIdLong())) return;
        if (Helpers.lastRunTimer1 < (System.currentTimeMillis() - 10_000 * 2) && Helpers.lastRunTimer1 != -1)
            Helpers.startTimer(event.getJDA(), 1);
        if (Helpers.lastRunTimer2 < (System.currentTimeMillis() - 120_000 * 2) && Helpers.lastRunTimer2 != -1)
            Helpers.startTimer(event.getJDA(), 2);
        if (Helpers.lastRunTimer3 < (System.currentTimeMillis() - 1_800_000 * 2) && Helpers.lastRunTimer3 != -1)
            Helpers.startTimer(event.getJDA(), 3);
        Guild guild = event.getGuild();
        if (event.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS) &&
                (SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ||
                        SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ||
                        SetLogChannelCommand.pmLogChannelCache.getUnchecked(guild.getIdLong()) != -1 ||
                        SetLogChannelCommand.fmLogChannelCache.getUnchecked(guild.getIdLong()) != -1)) {
            JSONObject message = mySQL.getMessageObject(event.getMessageIdLong());
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
                mySQL.executeUpdate("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
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
            eb.setTitle("Message deleted in #" + event.getChannel().getName() + MessageHelper.spaces.substring(0, 45 + author.getName().length()) + "\u200B");
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
                        "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) +
                        "```");
            }

            if (MessageHelper.filteredMessageDeleteCause.keySet().contains(event.getMessageIdLong()) && guild.getTextChannelById(SetLogChannelCommand.fmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                // FILTERED
                eb.setColor(Color.ORANGE);
                TextChannel fmLogChannel = guild.getTextChannelById(SetLogChannelCommand.fmLogChannelCache.getUnchecked(guild.getIdLong()));
                if (!event.getGuild().getSelfMember().hasPermission(fmLogChannel, Permission.MESSAGE_WRITE)) {
                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.FM_LOG);
                    return;
                }
                sendSplitIfNeeded(message, author, split, eb, fmLogChannel);
                eb.addField("Detected: ", "`" + MessageHelper.filteredMessageDeleteCause.get(event.getMessageIdLong()).replaceAll("`", "´") + "`", false);
                User bot = event.getJDA().getSelfUser();
                eb.setFooter("Deleted by: " + bot.getName() + "#" + bot.getDiscriminator(), bot.getEffectiveAvatarUrl());
                fmLogChannel.sendMessage(eb.build()).queue();
                MessageHelper.filteredMessageDeleteCause.remove(event.getMessageIdLong());
            } else if (MessageHelper.purgedMessageDeleter.containsKey(event.getMessageIdLong()) && guild.getTextChannelById(SetLogChannelCommand.pmLogChannelCache.getUnchecked(guild.getIdLong())) != null) {
                // PURGED
                eb.setColor(Color.decode("#551A8B"));
                User purger = event.getJDA().asBot().getShardManager().getUserById(MessageHelper.purgedMessageDeleter.get(event.getMessageIdLong()));
                TextChannel pmLogChannel = guild.getTextChannelById(SetLogChannelCommand.pmLogChannelCache.getUnchecked(guild.getIdLong()));
                if (!event.getGuild().getSelfMember().hasPermission(pmLogChannel, Permission.MESSAGE_WRITE)) {
                    Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.PM_LOG);
                    return;
                }
                sendSplitIfNeeded(message, author, split, eb, pmLogChannel);
                if (purger != null)
                    eb.setFooter("Purged by: " + purger.getName() + "#" + purger.getDiscriminator(), purger.getEffectiveAvatarUrl());
                pmLogChannel.sendMessage(eb.build()).queue();
                MessageHelper.purgedMessageDeleter.remove(event.getMessageIdLong());
            } else if (MessageHelper.botDeletedMessages.remove(event.getMessageIdLong())) {
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
                    Member member = guild.getMemberById(Melijn.mySQL.getMessageAuthorId(event.getMessageIdLong()));
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
            eb.setDescription("```LDIF\npart 2: " + message.getString("content").substring(1850) + "\nSenderID: " + author.getId() + "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) + "```");
            eb.setTitle("part 2");
            eb.setThumbnail("https://melijn.com/files/u/03-09-2018--09.16-29s.png");
        }
    }

    private void log(Guild guild, User author, EmbedBuilder eb, User deleter, JSONObject message, boolean split) {
        if (deleter == null) return;
        TextChannel sdmChannel = guild.getTextChannelById(SetLogChannelCommand.sdmLogChannelCache.getUnchecked(guild.getIdLong()));
        TextChannel odmChannel = guild.getTextChannelById(SetLogChannelCommand.odmLogChannelCache.getUnchecked(guild.getIdLong()));
        if (sdmChannel != null && !guild.getSelfMember().hasPermission(sdmChannel, Permission.MESSAGE_WRITE)) {
            Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.SDM_LOG);
        }
        if (odmChannel != null && !guild.getSelfMember().hasPermission(odmChannel, Permission.MESSAGE_WRITE)) {
            Melijn.mySQL.removeChannel(guild.getIdLong(), ChannelType.ODM_LOG);
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
            eb.setDescription("```LDIF\npart 2: " + message.getString("content").substring(1500) + "\nSenderID: " + author.getId() + "\nSent Time: " + MessageHelper.millisToDate(message.getLong("sentTime")) + "```");
        }
        eb.setFooter("Deleted by: " + deleter.getName() + "#" + deleter.getDiscriminator(), deleter.getEffectiveAvatarUrl());
        if (author.equals(deleter) && sdmChannel != null && guild.getSelfMember().hasPermission(sdmChannel, Permission.MESSAGE_WRITE)) {
            sdmChannel.sendMessage(eb.build()).queue();
        } else if (odmChannel != null && guild.getSelfMember().hasPermission(odmChannel, Permission.MESSAGE_WRITE)) {
            odmChannel.sendMessage(eb.build()).queue();
        }
    }

    private void filter(Message msg) {
        TaskScheduler.async(() -> {
            String message = msg.getContentRaw();
            StringBuilder detectedWord = null;
            TIntIntMap deniedPositions = new TIntIntHashMap();
            TIntIntMap allowedPositions = new TIntIntHashMap();
            List<String> deniedList = mySQL.getFilters(msg.getGuild().getIdLong(), "denied");
            List<String> allowedList = mySQL.getFilters(msg.getGuild().getIdLong(), "allowed");
            addPositions(message, deniedPositions, deniedList);
            addPositions(message, allowedPositions, allowedList);

            if (allowedPositions.size() > 0 && deniedPositions.size() > 0) {
                for (int beginDenied : deniedPositions.keys()) {
                    int endDenied = deniedPositions.get(beginDenied);
                    for (int beginAllowed : allowedPositions.keys()) {
                        int endAllowed = allowedPositions.get(beginAllowed);
                        if (beginDenied < beginAllowed || endDenied > endAllowed) {
                            detectedWord = new StringBuilder(message.substring(beginDenied, endDenied));
                        }
                    }
                }
            } else if (deniedPositions.size() > 0) {
                detectedWord = new StringBuilder();
                for (int beginDenied : deniedPositions.keys()) {
                    int endDenied = deniedPositions.get(beginDenied);
                    detectedWord.append(message, beginDenied, endDenied).append(", ");
                }
            }
            if (detectedWord != null) {
                MessageHelper.filteredMessageDeleteCause.put(msg.getIdLong(), detectedWord.substring(0, detectedWord.length() - 2));
                msg.delete().reason("Use of prohibited words").queue(
                        success -> {
                        }, failure -> {
                        }
                );
            }
        });
    }
}
