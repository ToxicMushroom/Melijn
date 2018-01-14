package com.pixelatedsource.jda.events;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.PixelSniper;
import com.pixelatedsource.jda.db.MySQL;
import com.pixelatedsource.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.awt.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Chat extends ListenerAdapter {

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private MySQL mySQL = PixelSniper.mySQL;

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        JDA jda = event.getJDA();
        Guild guild = event.getGuild();
        User author = event.getAuthor();
        Member member = event.getMember();
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
        mySQL.update("DELETE FROM history_messages WHERE sentTime < " + (System.currentTimeMillis() - 604_800_000L));
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
                            EmbedBuilder eb = new EmbedBuilder();
                            eb.setTitle("Message deleted in #" + e.getChannel().getName());
                            eb.setThumbnail(user.getAvatarUrl());
                            eb.setFooter(Helpers.getFooterStamp(), Helpers.getFooterIcon());
                            eb.setColor(Color.decode("#000001"));
                            eb.setDescription("```LDIF" +
                                    "\nSender: " + user.getName() + "#" + user.getDiscriminator() +
                                    "\nMessage: " + rs.getString("content").replaceAll("`", "´").replaceAll("\n", " ") +
                                    "\nSender's Id: " + rs.getString("authorId") +
                                    "\nSent Time: " + MessageHelper.millisToDate(rs.getLong("sentTime")) +
                                    "```");
                            if (MessageHelper.filterDeletedMessages.get(e.getMessageId()) != null) {
                                eb.addField("Detected: ", "`" + MessageHelper.filterDeletedMessages.get(e.getMessageId()).replaceAll("`", "´") + "`", false);
                                eb.setColor(Color.ORANGE);
                                MessageHelper.filterDeletedMessages.remove(e.getMessageId());
                            } else if (MessageHelper.deletedByEmote.get(e.getMessageId()) != null) {
                                User staff = MessageHelper.deletedByEmote.get(e.getMessageId());
                                eb.addField("Deleted by: ", staff.getName() + "#" + staff.getDiscriminator(), false);
                                eb.setColor(Color.magenta);
                            }
                            guild.getTextChannelById(mySQL.getLogChannelId(e.getGuild().getId())).sendMessage(eb.build()).queue(v -> {
                                if (eb.build().getColor().equals(Color.decode("#000001"))) mySQL.addUnclaimed(e.getMessageId(), v.getId());
                            });
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
