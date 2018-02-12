package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.blub.ChannelType;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.audit.ActionType;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.audit.AuditLogOption;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.MiscUtil;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Chat extends ListenerAdapter {

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private MySQL mySQL = PixelSniper.mySQL;
    private String latestId = "";
    private int latestChanges = 0;

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        Guild guild = event.getGuild();
        User author = event.getAuthor();
        try {
            mySQL.createMessage(event.getMessageId(), event.getMessage().getContentRaw(), author.getId(), guild.getId(), event.getChannel().getId());
            ResultSet rs = PixelSniper.mySQL.query("SELECT * FROM active_bans WHERE guildId='" + guild.getId() + "' AND endTime < " + System.currentTimeMillis());
            while (rs.next()) {
                event.getGuild().getController().unban(rs.getString("victimId")).queue();
                PixelSniper.mySQL.update("DELETE FROM active_bans WHERE guildId='" + guild.getId() + "' AND victimId='" + rs.getString("victimId") + "' AND endTime < " + System.currentTimeMillis());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent e) {
        Guild guild = e.getGuild();
        ResultSet rs = PixelSniper.mySQL.query("SELECT * FROM history_messages WHERE messageId= '" + e.getMessageId() + "' ");
        try {
            if (rs.next()) {
                executorService.execute(() -> {
                    try {
                        User user = e.getJDA().retrieveUserById(rs.getString("authorId")).complete();
                        if (user != null) {
                            for (Guild.Ban ban : guild.getBanList().complete()) {
                                if (ban.getUser() == user) return;
                            }

                            AuditLogEntry auditLogEntry = guild.getAuditLogs().type(ActionType.MESSAGE_DELETE).limit(1).complete().get(0);
                            boolean sameAsLast = latestId.equals(auditLogEntry.getId()) && latestChanges != Integer.valueOf(auditLogEntry.getOption(AuditLogOption.COUNT));
                            latestId = auditLogEntry.getId();
                            latestChanges = Integer.valueOf(auditLogEntry.getOption(AuditLogOption.COUNT));
                            ZonedDateTime deletionTime = MiscUtil.getCreationTime(auditLogEntry.getIdLong()).toZonedDateTime();
                            ZonedDateTime now = OffsetDateTime.now().atZoneSameInstant(deletionTime.getOffset());
                            deletionTime = deletionTime.plusSeconds(1).plusNanos((e.getJDA().getPing() * 1_000_000));

                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Message deleted in #" + e.getChannel().getName());
                            eb.setThumbnail(user.getAvatarUrl());
                            eb.setColor(Color.decode("#000001"));
                            eb.setDescription("```LDIF" + "\nSender: " + user.getName() + "#" + user.getDiscriminator() + "\nMessage: " + rs.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "\nSender's Id: " + rs.getString("authorId") + "\nSent Time: " + MessageHelper.millisToDate(rs.getLong("sentTime")) + "```");
                            if (MessageHelper.filterDeletedMessages.get(e.getMessageId()) != null) {
                                eb.addField("Detected: ", "`" + MessageHelper.filterDeletedMessages.get(e.getMessageId()).replaceAll("`", "´") + "`", false);
                                eb.setColor(Color.ORANGE);
                                User bot = guild.getSelfMember().getUser();
                                eb.setFooter("Deleted by: " + bot.getName() + "#" + bot.getDiscriminator(), bot.getAvatarUrl());
                                MessageHelper.filterDeletedMessages.remove(e.getMessageId());
                            } else if (now.toInstant().toEpochMilli() - deletionTime.toInstant().toEpochMilli() < 1000) {
                                User deletor = auditLogEntry.getUser();
                                if (deletor != null) eb.setFooter("Deleted by: " + deletor.getName() + "#" + deletor.getDiscriminator(), deletor.getAvatarUrl());
                            } else {
                                User deletor = sameAsLast ? auditLogEntry.getUser() : PixelSniper.mySQL.getMessageAuthor(e.getMessageId(), e.getJDA());
                                if (deletor != null) eb.setFooter("Deleted by: " + deletor.getName() + "#" + deletor.getDiscriminator(), deletor.getAvatarUrl());
                            }

                            guild.getTextChannelById(mySQL.getChannelId(e.getGuild().getId(), ChannelType.LOG)).sendMessage(eb.build()).queue(v -> {
                                if (eb.build().getColor().equals(Color.decode("#000001"))) mySQL.addUnclaimed(e.getMessageId(), v.getId());
                            });
                            mySQL.update("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
                            mySQL.saveDeletedMessage(e.getMessageId());

                        }
                    } catch (SQLException e1) {
                        e1.printStackTrace();
                    }
                });
            }
        } catch (SQLException e1) {
            e1.printStackTrace();
        }

    }
}
