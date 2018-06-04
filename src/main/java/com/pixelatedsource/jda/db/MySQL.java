package com.pixelatedsource.jda.db;

import com.pixelatedsource.jda.Helpers;
import com.pixelatedsource.jda.blub.*;
import com.pixelatedsource.jda.commands.management.SetLogChannelCommand;
import com.pixelatedsource.jda.commands.management.SetPrefixCommand;
import com.pixelatedsource.jda.commands.management.SetStreamerModeCommand;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.json.JSONObject;

import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.pixelatedsource.jda.utils.MessageHelper.millisToDate;

public class MySQL {

    private String ip;
    private String pass;
    private String user;
    private String dbname;
    private static Connection con;

    public MySQL(String ip, String user, String pass, String dbname) {
        this.ip = ip;
        this.user = user;
        this.pass = pass;
        this.dbname = dbname;
        connect();
    }

    private void connect() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://" + this.ip + ":3306/" + this.dbname + "?autoReconnect=true&useUnicode=true&useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC", this.user, this.pass);
            Statement statement = con.createStatement();
            statement.executeQuery("SET NAMES 'utf8mb4'");
            statement.close();
            Logger.getLogger(this.getClass().getName()).info("[MySQL] has connected");
            update("CREATE TABLE IF NOT EXISTS commands(commandName varchar(1000), gebruik varchar(1000), description varchar(2000), extra varchar(2000), category varchar(100), aliases varchar(200));");
            update("CREATE TABLE IF NOT EXISTS stream_urls(guildId bigint, url varchar(1500))");
            update("CREATE TABLE IF NOT EXISTS prefixes(guildId bigint, prefix bigint);");
            update("CREATE TABLE IF NOT EXISTS mute_roles(guildId bigint, roleId bigint);");
            update("CREATE TABLE IF NOT EXISTS join_roles(guildId bigint, roleId bigint);");
            update("CREATE TABLE IF NOT EXISTS perms_roles(guildId bigint, roleId bigint, permission varchar(256));");
            update("CREATE TABLE IF NOT EXISTS perms_users(guildId bigint, userId bigint, permission varchar(256));");
            update("CREATE TABLE IF NOT EXISTS log_channels(guildId bigint, channelId bigint)");
            update("CREATE TABLE IF NOT EXISTS music_channels(guildId bigint, channelId bigint)");
            update("CREATE TABLE IF NOT EXISTS welcome_channels(guildId bigint, channelId bigint)");
            update("CREATE TABLE IF NOT EXISTS nowplaying_channels(guildId bigint, channelId bigint)");
            update("CREATE TABLE IF NOT EXISTS streamer_modes(guildId bigint, state boolean)");
            update("CREATE TABLE IF NOT EXISTS filters(guildId bigint, mode varchar(16), content varchar(2000))");
            update("CREATE TABLE IF NOT EXISTS warns(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), moment bigint);");
            update("CREATE TABLE IF NOT EXISTS active_bans(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), startTime bigint, endTime bigint);");
            update("CREATE TABLE IF NOT EXISTS history_bans(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            update("CREATE TABLE IF NOT EXISTS active_mutes(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), startTime bigint, endTime bigint);");
            update("CREATE TABLE IF NOT EXISTS history_mutes(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            update("CREATE TABLE IF NOT EXISTS history_messages(guildId bigint, authorId bigint, messageId bigint, content varchar(2000), textChannelId bigint, sentTime bigint);");
            update("CREATE TABLE IF NOT EXISTS deleted_messages(guildId bigint, authorId bigint, messageId bigint, content varchar(2000), textChannelId bigint, sentTime bigint, delTime bigint);");
            update("CREATE TABLE IF NOT EXISTS join_messages(guildId bigint, content varchar(2000))");
            update("CREATE TABLE IF NOT EXISTS leave_messages(guildId bigint, content varchar(2000))");
            update("CREATE TABLE IF NOT EXISTS unclaimed_messages(deletedMessageId bigint, logMessageId bigint);");
            update("CREATE TABLE IF NOT EXISTS votes(userId bigint, votes bigint, streak bigint, lastTime bigint);");
            update("CREATE TABLE IF NOT EXISTS nextvote_notifications(userId bigint, targetId bigint);");
        } catch (SQLException e) {
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "[MySQL] did not connect");
            e.printStackTrace();
            System.exit(44);
        }
    }

    public void close() {
        try {
            if (con != null) {
                con.close();
                System.out.println("[MySQL] has disconnected");
            }
        } catch (SQLException e) {
            System.out.println("[MySQL] did not disconnect proparily error:" + e.getMessage());
        }
    }

    public void update(String qry) {
        try {
            Statement st = con.createStatement();
            st.executeUpdate(qry);
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ResultSet query(String qry) {
        ResultSet rs = null;
        try {
            Statement st = con.createStatement();
            rs = st.executeQuery(qry);
        } catch (SQLException e) {
            connect();
            e.printStackTrace();
        }
        return rs;
    }

    public long getMessageAuthorId(long messageId) {
        try {
            PreparedStatement getAuthor = con.prepareStatement("SELECT * FROM history_messages WHERE messageId= ?");
            getAuthor.setLong(1, messageId);
            ResultSet rs = getAuthor.executeQuery();
            while (rs.next()) {
                return rs.getLong("authorId");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONObject getMessageObject(long messageId) {
        JSONObject jsonObject = new JSONObject();
        try {
            PreparedStatement preparedStatement = con.prepareStatement("SELECT * FROM history_messages WHERE messageId=?");
            preparedStatement.setLong(1, messageId);
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                jsonObject.put("authorId", rs.getLong("authorId"));
                jsonObject.put("sentTime", rs.getLong("sentTime"));
                jsonObject.put("content", rs.getString("content"));
                jsonObject.put("guildId", rs.getLong("guildId"));
                jsonObject.put("textChannelId", rs.getLong("textChannelId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public void createMessage(long messageId, String content, long authorId, long guildId, long textChannelId) {
        try {
            PreparedStatement createMessage = con.prepareStatement("INSERT INTO history_messages(guildId, authorId, messageId, content, textChannelId, sentTime) VALUES (?, ?, ?, ?, ?, ?)");
            createMessage.setLong(1, guildId);
            createMessage.setLong(2, authorId);
            createMessage.setLong(3, messageId);
            createMessage.setString(4, content);
            createMessage.setLong(5, textChannelId);
            createMessage.setLong(6, System.currentTimeMillis());
            createMessage.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveDeletedMessage(long messageId) {
        ResultSet rs = query("SELECT * FROM history_messages WHERE messageId= '" + messageId + "'");
        try {
            if (rs.next()) {
                long guildId = rs.getLong("guildId");
                long authorId = rs.getLong("authorId");
                String content = rs.getString("content");
                long channelId = rs.getLong("textChannelId");
                long sentTime = rs.getLong("sentTime");
                PreparedStatement createMessage = con.prepareStatement("INSERT INTO deleted_messages (guildId, authorId, messageId, content, textChannelId, sentTime, delTime) VALUES (?, ?, ?, ?, ?, ?, ?)");
                createMessage.setLong(1, guildId);
                createMessage.setLong(2, authorId);
                createMessage.setLong(3, messageId);
                createMessage.setString(4, content);
                createMessage.setLong(5, channelId);
                createMessage.setLong(6, sentTime);
                createMessage.setLong(7, System.currentTimeMillis());
                createMessage.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUnclaimed(long deletedMessageId, long unclaimedId) {
        try {
            PreparedStatement addunclaimed = con.prepareStatement("INSERT INTO unclaimed_messages (deletedMessageId, logMessageId) VALUES (?, ?)");
            addunclaimed.setLong(1, deletedMessageId);
            addunclaimed.setLong(2, unclaimedId);
            addunclaimed.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getMessageIdByUnclaimedId(long unclaimedId) {
        try {
            PreparedStatement getMessageId = con.prepareStatement("SELECT * FROM unclaimed_messages WHERE logMessageId= ?");
            getMessageId.setLong(1, unclaimedId);
            ResultSet rs = getMessageId.executeQuery();
            long s = -1;
            while (rs.next()) {
                s = rs.getLong("deletedMessageId");
            }
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public MessageEmbed unclaimedToClaimed(long messageId, JDA jda, User staff) {
        try {
            PreparedStatement getdeletedmessage = con.prepareStatement("SELECT * FROM deleted_messages WHERE messageId= ?");
            getdeletedmessage.setLong(1, messageId);
            ResultSet rs = getdeletedmessage.executeQuery();
            if (rs.next()) {
                EmbedBuilder eb = new EmbedBuilder();
                eb.setTitle("Message deleted in #" + jda.getTextChannelById(rs.getString("textChannelId")).getName());
                eb.setThumbnail(jda.retrieveUserById(rs.getString("authorId")).complete().getAvatarUrl());
                eb.setFooter(millisToDate(rs.getLong("delTime")), Helpers.getFooterIcon());
                eb.setColor(Color.magenta);
                String authorId = rs.getString("authorId");
                User author = jda.retrieveUserById(authorId).complete();
                eb.setDescription("```LDIF" + "\nSender: " + author.getName() + "#" + author.getDiscriminator() + "\nMessage: " + rs.getString("content").replaceAll("`", "´").replaceAll("\n", " ") + "\nSender's ID: " + rs.getString("authorId") + "\nSent Time: " + millisToDate(rs.getLong("sentTime")) + "```");
                eb.addField("Deleted by: ", staff.getName() + "#" + staff.getDiscriminator(), false);
                return eb.build();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Permissions stuff---------------------------------------------------------
    public void addRolePermission(long guildId, long roleId, String permission) {
        try {
            PreparedStatement adding = con.prepareStatement("INSERT INTO perms_roles(guildId, roleId, permission) VALUES (?, ?, ?)");
            adding.setLong(1, guildId);
            adding.setLong(2, roleId);
            adding.setString(3, permission);
            adding.executeUpdate();
            adding.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUserPermission(long guildId, long userId, String permission) {
        try {
            PreparedStatement adding = con.prepareStatement("INSERT INTO perms_users(guildId, userId, permission) VALUES (?, ?, ?)");
            adding.setLong(1, guildId);
            adding.setLong(2, userId);
            adding.setString(3, permission);
            adding.executeUpdate();
            adding.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeRolePermission(long guildId, long roleId, String permission) {
        try {
            PreparedStatement removing = con.prepareStatement("DELETE FROM perms_roles WHERE guildId= ? AND roleId= ? AND permission= ?");
            removing.setLong(1, guildId);
            removing.setLong(2, roleId);
            removing.setString(3, permission);
            removing.executeUpdate();
            removing.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeUserPermission(long guildId, long userId, String permission) {
        try {
            PreparedStatement removing = con.prepareStatement("DELETE FROM perms_users WHERE guildId= ? AND usersId= ? AND permission= ?");
            removing.setLong(1, guildId);
            removing.setLong(2, userId);
            removing.setString(3, permission);
            removing.executeUpdate();
            removing.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPermission(Guild guild, long userId, String permission) {
        try {
            PreparedStatement getting = con.prepareStatement("SELECT * FROM perms_users WHERE guildId= ? AND userId= ? AND permission= ?");
            getting.setLong(1, guild.getIdLong());
            getting.setLong(2, userId);
            getting.setString(3, permission);
            ResultSet rs = getting.executeQuery();
            if (rs.next()) return true;
            getting.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Role> roles = new ArrayList<>(guild.getMemberById(userId).getRoles());
        roles.add(guild.getPublicRole());
        for (Role role : roles) {
            try {
                PreparedStatement getting = con.prepareStatement("SELECT * FROM perms_roles WHERE guildId= ? AND roleId= ? AND permission= ?");
                getting.setLong(1, guild.getIdLong());
                getting.setLong(2, role.getIdLong());
                getting.setString(3, permission);
                ResultSet rs = getting.executeQuery();
                if (rs.next()) return true;
                getting.close();
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void clearRolePermissions(long guildId, long roleId) {
        try {
            PreparedStatement clearing = con.prepareStatement("DELETE FROM perms_roles WHERE guildId= ? AND roleId= ?");
            clearing.setLong(1, guildId);
            clearing.setLong(2, roleId);
            clearing.executeUpdate();
            clearing.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearUserPermissions(long guildId, long userId) {
        try {
            PreparedStatement clearing = con.prepareStatement("DELETE FROM perm_users WHERE guildId= ? AND userId= ?");
            clearing.setLong(1, guildId);
            clearing.setLong(2, userId);
            clearing.executeUpdate();
            clearing.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getRolePermissions(long guildId, long roleId) {
        List<String> toReturn = new ArrayList<>();
        try {
            PreparedStatement getPerms = con.prepareStatement("SELECT * FROM perms_roles WHERE guildId= ? AND roleId= ?");
            getPerms.setLong(1, guildId);
            getPerms.setLong(2, roleId);
            ResultSet rs = getPerms.executeQuery();
            while (rs.next()) {
                toReturn.add(rs.getString("permission"));
            }
            getPerms.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public List<String> getUserPermissions(long guildId, long userId) {
        List<String> toReturn = new ArrayList<>();
        try {
            PreparedStatement getPerms = con.prepareStatement("SELECT * FROM perms_users WHERE guildId= ? AND userId= ?");
            getPerms.setLong(1, guildId);
            getPerms.setLong(2, userId);
            ResultSet rs = getPerms.executeQuery();
            while (rs.next()) {
                toReturn.add(rs.getString("permission"));
            }
            getPerms.close();
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public boolean noOneHasPermission(long guildId, String permission) {
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM perms_roles WHERE guildId= ? AND permission= ?");
            statement.setLong(1, guildId);
            statement.setString(2, permission);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                return false;
            }
            statement.close();
            rs.close();

            PreparedStatement statement1 = con.prepareStatement("SELECT * FROM perms_users WHERE guildId= ? AND permission= ?");
            statement1.setLong(1, guildId);
            statement1.setString(2, permission);
            ResultSet rs1 = statement1.executeQuery();
            while (rs1.next()) {
                return false;
            }
            statement1.close();
            rs1.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    public void copyRolePermissions(long guildId, long roleId1, long roleId2) {
        List<String> permsRole1 = getRolePermissions(guildId, roleId1);
        List<String> permsRole2 = getRolePermissions(guildId, roleId2);
        for (String permission : permsRole1) {
            if (!permsRole2.contains(permission)) {
                addRolePermission(guildId, roleId2, permission);
            }
        }
    }

    public void copyUserPermissions(long guildId, long userId1, long userId2) {
        List<String> permsUser1 = getUserPermissions(guildId, userId1);
        List<String> permsUser2 = getUserPermissions(guildId, userId2);
        for (String permission : permsUser1) {
            if (!permsUser2.contains(permission)) {
                addUserPermission(guildId, userId2, permission);
            }
        }
    }

    public void copyRoleUserPermissions(long guildId, long roleId, long userId) {
        List<String> permsRole = getRolePermissions(guildId, roleId);
        List<String> permsUser = getUserPermissions(guildId, userId);
        for (String permission : permsRole) {
            if (!permsUser.contains(permission)) {
                addUserPermission(guildId, userId, permission);
            }
        }
    }

    public void copyUserRolePermissions(long guildId, long userId, long roleId) {
        List<String> permsUser = getUserPermissions(guildId, userId);
        List<String> permsRole = getRolePermissions(guildId, roleId);
        for (String permission : permsUser) {
            if (!permsRole.contains(permission)) {
                addRolePermission(guildId, roleId, permission);
            }
        }
    }

    //Prefix stuff---------------------------------------------------------------
    public void setPrefix(long guildId, String prefix) {
        try {
            if (!SetPrefixCommand.prefixes.containsKey(guildId)) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO prefixes (guildId, prefix) VALUES (?, ?)");
                setPrefix.setLong(1, guildId);
                setPrefix.setString(2, prefix);
                setPrefix.executeUpdate();
                setPrefix.close();
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE prefixes SET prefix= ? WHERE guildId= ?");
                updatePrefix.setString(1, prefix);
                updatePrefix.setLong(2, guildId);
                updatePrefix.executeUpdate();
                updatePrefix.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Punishment stuff--------------------------------------------------------------
    public boolean setTempBan(User staff, User victim, Guild guild, String reason, long seconds) {
        if (seconds > 0) {
            Long moment = System.currentTimeMillis();
            Long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            String name = victim.getName() + "#" + victim.getDiscriminator();
            String namep = staff.getName() + "#" + staff.getDiscriminator();
            try {
                EmbedBuilder banned = new EmbedBuilder();
                banned.setColor(Color.RED);
                banned.setDescription("```LDIF" + "\nBanned: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + millisToDate(moment) + "\nUntil: " + millisToDate(until) + "```");
                banned.setThumbnail(victim.getAvatarUrl());
                if (victim.getAvatarUrl() == null) banned.setThumbnail(victim.getDefaultAvatarUrl());
                banned.setAuthor("Banned by: " + namep, null, staff.getAvatarUrl());
                if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(banned.build()).queue();
                long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
                if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                    if (victim.isFake()) guild.getTextChannelById(logChannelId).sendMessage(banned.build() + "\nTarget has private messages disabled").queue();
                    else guild.getTextChannelById(logChannelId).sendMessage(banned.build()).queue();
                }
                ResultSet rs = query("SELECT * FROM active_bans WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
                if (rs.next()) {//Player was banned so just update the times
                    PreparedStatement banupdate = con.prepareStatement("UPDATE active_bans SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                    banupdate.setLong(1, victim.getIdLong());
                    banupdate.setLong(2, guild.getIdLong());
                    banupdate.setString(3, reason);
                    banupdate.setLong(4, moment);
                    banupdate.setLong(5, until);
                    banupdate.setLong(6, staff.getIdLong());
                    banupdate.setLong(7, victim.getIdLong());
                    banupdate.setLong(8, guild.getIdLong());
                    banupdate.executeUpdate();
                } else {//nieuwe ban
                    PreparedStatement ban = con.prepareStatement("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                    ban.setLong(1, guild.getIdLong());
                    ban.setLong(2, victim.getIdLong());
                    ban.setLong(3, staff.getIdLong());
                    ban.setString(4, reason);
                    ban.setLong(5, moment);
                    ban.setLong(6, until);
                    ban.executeUpdate();
                }
                //add to history as active
                PreparedStatement banhistoire = con.prepareStatement("INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
                banhistoire.setLong(1, guild.getIdLong());
                banhistoire.setLong(2, victim.getIdLong());
                banhistoire.setLong(3, staff.getIdLong());
                banhistoire.setString(4, reason);
                banhistoire.setLong(5, moment);
                banhistoire.setLong(6, until);
                banhistoire.setBoolean(7, true);
                banhistoire.executeUpdate();
                guild.getController().ban(victim.getId(), 7, reason).queue();
                return true;
            } catch (SQLException | IllegalStateException e) {
                return false;
            }
        }
        return false;
    }

    public boolean setPermBan(User staff, User victim, Guild guild, String reason) {
        Long moment = System.currentTimeMillis();
        String name = victim.getName() + "#" + victim.getDiscriminator();
        String namep = staff.getName() + "#" + staff.getDiscriminator();
        try {
            EmbedBuilder banned = new EmbedBuilder();
            banned.setColor(Color.RED);
            banned.setDescription("```LDIF" + "\nBanned: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(moment) + "```");
            banned.setThumbnail(victim.getAvatarUrl());
            if (victim.getAvatarUrl() == null) banned.setThumbnail(victim.getDefaultAvatarUrl());
            banned.setAuthor("Permanently banned by: " + namep, null, staff.getAvatarUrl());
            if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(banned.build()).queue();
            long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
            if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null){
                if (victim.isFake()) guild.getTextChannelById(logChannelId).sendMessage(banned.build() + "\nTarget has private messages disabled").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(banned.build()).queue();
            }
            ResultSet rs = query("SELECT * FROM active_bans WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
            if (rs.next()) {//Player was banned so just update the times
                PreparedStatement banupdate = con.prepareStatement("UPDATE active_bans SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                banupdate.setLong(1, victim.getIdLong());
                banupdate.setLong(2, guild.getIdLong());
                banupdate.setString(3, reason);
                banupdate.setLong(4, moment);
                banupdate.setBigDecimal(5, null);
                banupdate.setLong(6, staff.getIdLong());
                banupdate.setLong(7, victim.getIdLong());
                banupdate.setLong(8, guild.getIdLong());
                banupdate.executeUpdate();
            } else {//nieuwe ban
                PreparedStatement ban = con.prepareStatement("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                ban.setLong(1, guild.getIdLong());
                ban.setLong(2, victim.getIdLong());
                ban.setLong(3, staff.getIdLong());
                ban.setString(4, reason);
                ban.setLong(5, moment);
                ban.setBigDecimal(6, null);
                ban.executeUpdate();
            }
            //add to history as active
            PreparedStatement banhistoire = con.prepareStatement("INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
            banhistoire.setLong(1, guild.getIdLong());
            banhistoire.setLong(2, victim.getIdLong());
            banhistoire.setLong(3, staff.getIdLong());
            banhistoire.setString(4, reason);
            banhistoire.setLong(5, moment);
            banhistoire.setBigDecimal(6, null);
            banhistoire.setBoolean(7, true);
            banhistoire.executeUpdate();
            guild.getController().ban(victim.getId(), 7, reason).queue();
            return true;
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }

    public boolean unban(User toUnban, Guild guild, JDA jda, boolean auto) {
        if (toUnban != null) {
            try {
                PreparedStatement getBans = con.prepareStatement("SELECT * FROM active_bans WHERE guildId= ? AND victimId= ?");
                getBans.setLong(1, guild.getIdLong());
                getBans.setLong(2, toUnban.getIdLong());
                ResultSet rs = getBans.executeQuery();
                while (rs.next()) {
                    User author = jda.retrieveUserById(rs.getString("authorId")).complete();
                    guild.getController().unban(toUnban.getId()).queue();
                    PreparedStatement unban = con.prepareStatement("UPDATE history_bans SET ACTIVE= ? WHERE victimId= ? AND guildId= ?");
                    unban.setBoolean(1, false);
                    unban.setString(2, toUnban.getId());
                    unban.setString(3, guild.getId());
                    unban.executeUpdate();
                    unban.close();
                    PreparedStatement deleteBans = con.prepareStatement("DELETE FROM active_bans WHERE guildId= ? AND victimId= ?");
                    deleteBans.setLong(1, guild.getIdLong());
                    deleteBans.setLong(2, toUnban.getIdLong());
                    deleteBans.executeUpdate();
                    deleteBans.close();

                    EmbedBuilder eb = new EmbedBuilder();
                    if (auto) author = jda.getSelfUser();
                    eb.setAuthor("Unbanned by: " + author.getName() + "#" + author.getDiscriminator(), null, author.getAvatarUrl());
                    eb.setDescription("```LDIF" + "\nUnbanned: " + toUnban.getName() + "#" + toUnban.getDiscriminator() + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(System.currentTimeMillis()) + "```");
                    eb.setThumbnail(toUnban.getAvatarUrl());
                    eb.setColor(Helpers.EmbedColor);
                    eb.setColor(Color.green);

                    toUnban.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
                    long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
                    if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                        if (toUnban.isFake()) guild.getTextChannelById(logChannelId).sendMessage(eb.build() + "\nTarget has private messages disabled").queue();
                        else guild.getTextChannelById(logChannelId).sendMessage(eb.build()).queue();
                    }
                   return true;
                }
                return false;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public boolean addWarn(User staff, User victim, Guild guild, String reason) {
        try {
            reason = reason.replaceFirst(" ", "");
            PreparedStatement newWarn = con.prepareStatement("INSERT INTO warns(guildId, victimId, authorId, reason, moment) VALUES (?, ?, ?, ?, ?);");
            newWarn.setLong(1, guild.getIdLong());
            newWarn.setLong(2, victim.getIdLong());
            newWarn.setLong(3, staff.getIdLong());
            newWarn.setString(4, reason);
            newWarn.setLong(5, System.currentTimeMillis());
            newWarn.executeUpdate();
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setAuthor("Warned by: " + staff.getName() + "#" + staff.getDiscriminator(), null, staff.getAvatarUrl());
            embedBuilder.setDescription("```LDIF\n" + "Warned: " + victim.getName() + "#" + victim.getDiscriminator() + "\n" + "Reason: " + reason + "\n" + "Guild: " + guild.getName() + "\n" + "Moment: " + millisToDate(System.currentTimeMillis()) + "\n" + "```");
            embedBuilder.setThumbnail(victim.getAvatarUrl());
            embedBuilder.setColor(Color.yellow);
            long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
            if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                if (victim.isFake()) guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build() + "\nTarget has private messages disabled.").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build()).queue();
            }
            if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(embedBuilder.build()).queue();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean setTempMute(User staff, User victim, Guild guild, String reason, long seconds) {
        if (seconds > 0) {
            Long moment = System.currentTimeMillis();
            Long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            String name = victim.getName() + "#" + victim.getDiscriminator();
            String namep = staff.getName() + "#" + staff.getDiscriminator();
            try {
                EmbedBuilder muted = new EmbedBuilder();
                muted.setColor(Color.BLUE);
                muted.setDescription("```LDIF" + "\nMuted: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + millisToDate(moment) + "\nUntil: " + millisToDate(until) + "```");
                muted.setThumbnail(victim.getAvatarUrl());
                if (victim.getAvatarUrl() == null) muted.setThumbnail(victim.getDefaultAvatarUrl());
                muted.setAuthor("Muted by: " + namep, null, staff.getAvatarUrl());
                if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(muted.build()).queue();
                long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
                if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                    if (victim.isFake()) guild.getTextChannelById(logChannelId).sendMessage(muted.build() + "\nTarget has private messages disabled").queue();
                    else guild.getTextChannelById(logChannelId).sendMessage(muted.build()).queue();
                }
                ResultSet rs = query("SELECT * FROM active_mutes WHERE victimId= '" + victim.getIdLong() + "' AND guildId= '" + guild.getIdLong() + "'");
                if (rs.next()) {//Player was banned so just update the times
                    PreparedStatement muteupdate = con.prepareStatement("UPDATE active_mutes SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                    muteupdate.setLong(1, victim.getIdLong());
                    muteupdate.setLong(2, guild.getIdLong());
                    muteupdate.setString(3, reason);
                    muteupdate.setLong(4, moment);
                    muteupdate.setLong(5, until);
                    muteupdate.setLong(6, staff.getIdLong());
                    muteupdate.setLong(7, victim.getIdLong());
                    muteupdate.setLong(8, guild.getIdLong());
                    muteupdate.executeUpdate();
                } else {//nieuwe mute
                    PreparedStatement mute = con.prepareStatement("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                    mute.setLong(1, guild.getIdLong());
                    mute.setLong(2, victim.getIdLong());
                    mute.setLong(3, staff.getIdLong());
                    mute.setString(4, reason);
                    mute.setLong(5, moment);
                    mute.setLong(6, until);
                    mute.executeUpdate();
                }
                //add to history as active
                PreparedStatement mutehistoire = con.prepareStatement("INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
                mutehistoire.setLong(1, guild.getIdLong());
                mutehistoire.setLong(2, victim.getIdLong());
                mutehistoire.setLong(3, staff.getIdLong());
                mutehistoire.setString(4, reason);
                mutehistoire.setLong(5, moment);
                mutehistoire.setLong(6, until);
                mutehistoire.setBoolean(7, true);
                mutehistoire.executeUpdate();
                return true;
            } catch (SQLException | IllegalStateException e) {
                return false;
            }
        }
        return false;
    }

    public boolean setPermMute(User staff, User victim, Guild guild, String reason) {
        Long moment = System.currentTimeMillis();
        String name = victim.getName() + "#" + victim.getDiscriminator();
        String namep = staff.getName() + "#" + staff.getDiscriminator();
        try {
            EmbedBuilder muted = new EmbedBuilder();
            muted.setColor(Color.BLUE);
            muted.setDescription("```LDIF" + "\nMuted: " + name + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(moment) + "```");
            muted.setThumbnail(victim.getAvatarUrl());
            if (victim.getAvatarUrl() == null) muted.setThumbnail(victim.getDefaultAvatarUrl());
            muted.setAuthor("Permanently muted by: " + namep, null, staff.getAvatarUrl());
            if (!victim.isFake()) victim.openPrivateChannel().complete().sendMessage(muted.build()).queue();
            long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
            if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                if (victim.isFake()) guild.getTextChannelById(logChannelId).sendMessage(muted.build() + "\nTarget has private messages disabled").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(muted.build()).queue();
            }
            ResultSet rs = query("SELECT * FROM active_mutes WHERE victimId= '" + victim.getId() + "' AND guildId= '" + guild.getId() + "'");
            if (rs.next()) {
                PreparedStatement muteupdate = con.prepareStatement("UPDATE active_mutes SET victimId= ?, guildId= ?, reason= ?, startTime= ?, endTime= ?, authorId= ? WHERE victimId= ? AND guildId= ?");
                muteupdate.setLong(1, victim.getIdLong());
                muteupdate.setLong(2, guild.getIdLong());
                muteupdate.setString(3, reason);
                muteupdate.setLong(4, moment);
                muteupdate.setBigDecimal(5, null);
                muteupdate.setLong(6, staff.getIdLong());
                muteupdate.setLong(7, victim.getIdLong());
                muteupdate.setLong(8, guild.getIdLong());
                muteupdate.executeUpdate();
            } else {//nieuwe mute
                PreparedStatement mute = con.prepareStatement("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?)");
                mute.setLong(1, guild.getIdLong());
                mute.setLong(2, victim.getIdLong());
                mute.setLong(3, staff.getIdLong());
                mute.setString(4, reason);
                mute.setLong(5, moment);
                mute.setBigDecimal(6, null);
                mute.executeUpdate();
            }
            //add to history as active
            PreparedStatement mutehistoire = con.prepareStatement("INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)");
            mutehistoire.setLong(1, guild.getIdLong());
            mutehistoire.setLong(2, victim.getIdLong());
            mutehistoire.setLong(3, staff.getIdLong());
            mutehistoire.setString(4, reason);
            mutehistoire.setLong(5, moment);
            mutehistoire.setBigDecimal(6, null);
            mutehistoire.setBoolean(7, true);
            mutehistoire.executeUpdate();
            return true;
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }

    public boolean unmute(Guild guild, User toUnmute, JDA jda, boolean auto) {
        if (toUnmute != null) {
            try {
                PreparedStatement getMutes = con.prepareStatement("SELECT * FROM active_mutes WHERE guildId= ? AND victimId= ?");
                getMutes.setLong(1, guild.getIdLong());
                getMutes.setLong(2, toUnmute.getIdLong());
                ResultSet rs = getMutes.executeQuery();
                boolean t = false;
                while (rs.next()) {
                    User author = jda.retrieveUserById(rs.getString("authorId")).complete();
                    PreparedStatement unmute = con.prepareStatement("UPDATE history_mutes SET ACTIVE= ? WHERE victimId= ? AND guildId= ?");
                    unmute.setBoolean(1, false);
                    unmute.setLong(2, toUnmute.getIdLong());
                    unmute.setLong(3, guild.getIdLong());
                    unmute.executeUpdate();
                    unmute.close();
                    PreparedStatement deleteMutes = con.prepareStatement("DELETE FROM active_mutes WHERE guildId= ? AND victimId= ?");
                    deleteMutes.setLong(1, guild.getIdLong());
                    deleteMutes.setLong(2, toUnmute.getIdLong());
                    deleteMutes.executeUpdate();
                    deleteMutes.close();
                    EmbedBuilder eb = new EmbedBuilder();
                    if (auto) author = jda.getSelfUser();
                    eb.setAuthor("Unmuted by: " + author.getName() + "#" + author.getDiscriminator(), null, author.getAvatarUrl());
                    eb.setDescription("```LDIF" + "\nUnmuted: " + toUnmute.getName() + "#" + toUnmute.getDiscriminator() + "\nGuild: " + guild.getName() + "\nMoment: " + millisToDate(System.currentTimeMillis()) + "```");
                    eb.setThumbnail(toUnmute.getAvatarUrl());
                    eb.setColor(Helpers.EmbedColor);
                    eb.setColor(Color.green);
                    guild.getController().removeSingleRoleFromMember(guild.getMember(toUnmute), guild.getRoleById(getRoleId(guild.getIdLong(), RoleType.MUTE))).queue();
                    toUnmute.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
                    long logChannelId = SetLogChannelCommand.guildLogChannelMap.getOrDefault(guild.getIdLong(), -1L);
                    if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                        if (toUnmute.isFake()) guild.getTextChannelById(logChannelId).sendMessage(eb.build() + "\nTarget has private messages disabled").queue();
                        else guild.getTextChannelById(logChannelId).sendMessage(eb.build()).queue();
                    }
                    t = true;
                }
                return t;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    //Punishment getters
    public String[] getUserBans(long guildId, long userId, JDA jda) {
        try {
            PreparedStatement getbans = con.prepareStatement("SELECT * FROM history_bans WHERE victimId= ? AND guildId= ?");
            getbans.setLong(1, userId);
            getbans.setLong(2, guildId);
            ResultSet rs = getbans.executeQuery();
            int amount = 0;
            while (rs.next()) amount++;
            String[] bans = new String[amount];
            ResultSet rs2 = getbans.executeQuery();
            if (amount == 0) return new String[]{"no bans"};
            int progress = 0;
            while (rs2.next()) {
                String endTime = rs2.getString("endTime").equalsIgnoreCase("NULL") ? "Infinity" : millisToDate(rs2.getLong("endTime"));
                User staff = jda.retrieveUserById(rs2.getString("authorId")).complete();
                bans[progress] = String.valueOf("```ini\n" + "[Banned by]: " + staff.getName() + "#" + staff.getDiscriminator() + "\n[Reason]: " + rs2.getString("reason") + "\n[From]: " + millisToDate(rs2.getLong("startTime")) + "\n[Until]: " + endTime + "\n[active]: " + rs2.getString("active") + "```");
                progress++;
            }
            return bans;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{"no bans"};
    }

    public String[] getUserMutes(long guildId, long userId, JDA jda) {
        try {
            PreparedStatement getMutes = con.prepareStatement("SELECT * FROM history_mutes WHERE victimId= ? AND guildId= ?");
            getMutes.setLong(1, userId);
            getMutes.setLong(2, guildId);
            ResultSet rs = getMutes.executeQuery();
            int amount = 0;
            while (rs.next()) amount++;
            String[] bans = new String[amount];
            ResultSet rs2 = getMutes.executeQuery();
            if (amount == 0) return new String[]{"no mutes"};
            int progress = 0;
            while (rs2.next()) {
                User staff = jda.retrieveUserById(rs2.getString("authorId")).complete();
                String endTime = rs2.getString("endTime").equalsIgnoreCase("NULL") ? "Infinity" : millisToDate(rs2.getLong("endTime"));
                bans[progress] = String.valueOf("```ini\n" + "[Muted by]: " + staff.getName() + "#" + staff.getDiscriminator() + "\n[Reason]: " + rs2.getString("reason") + "\n[From]: " + millisToDate(rs2.getLong("startTime")) + "\n[Until]: " + endTime + "\n[active]: " + rs2.getString("active") + "```");
                progress++;
            }
            return bans;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{"no mutes"};
    }

    public boolean isUserMuted(long guildId, long userId) {
        try {
            PreparedStatement getMutes = con.prepareStatement("SELECT * FROM active_mutes WHERE victimId= ? AND guildId= ?");
            getMutes.setLong(1, userId);
            getMutes.setLong(2, guildId);
            ResultSet rs = getMutes.executeQuery();
            while (rs.next()) {
                return true;
            }
            return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String[] getUserWarns(long guildId, long userId, JDA jda) {
        try {
            PreparedStatement getbans = con.prepareStatement("SELECT * FROM warns WHERE victimId= ? AND guildId= ?");
            getbans.setLong(1, userId);
            getbans.setLong(2, guildId);
            ResultSet rs = getbans.executeQuery();
            int amount = 0;
            while (rs.next()) amount++;
            String[] bans = new String[amount];
            ResultSet rs2 = getbans.executeQuery();
            if (amount == 0) return new String[]{"no warns"};
            int progress = 0;
            while (rs2.next()) {
                User staff = jda.retrieveUserById(rs2.getString("authorId")).complete();
                bans[progress] = String.valueOf("```ini\n" + "[Warned by]: " + staff.getName() + "#" + staff.getDiscriminator() + "\n[Reason]: " + rs2.getString("reason") + "\n[Moment]: " + millisToDate(rs2.getLong("moment")) + "```");
                progress++;
            }
            return bans;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[]{"no warns"};
    }


    //log channel stuff----------------------------------------------------------

    public boolean setChannel(long guildId, long channelId, ChannelType type) {
        try {
            if (getChannelId(guildId, type) == -1) {
                PreparedStatement setChannel = con.prepareStatement("INSERT INTO " + type.toString().toLowerCase() + "_channels (guildId, channelId) VALUES (?, ?)");
                setChannel.setLong(1, guildId);
                setChannel.setLong(2, channelId);
                setChannel.executeUpdate();
                setChannel.close();
                return true;
            } else {
                PreparedStatement updateChannel = con.prepareStatement("UPDATE " + type.toString().toLowerCase() + "_channels SET channelId= ? WHERE guildId= ?");
                updateChannel.setLong(1, channelId);
                updateChannel.setLong(2, guildId);
                updateChannel.executeUpdate();
                updateChannel.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public long getChannelId(long guildId, ChannelType type) {
        try {
            PreparedStatement getChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ?");
            getChannel.setLong(1, guildId);
            ResultSet rs = getChannel.executeQuery();
            long s = -1;
            while (rs.next()) s = rs.getLong("channelId");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void removeChannel(long guildId, ChannelType type) {
        try {
            PreparedStatement removeChannel = con.prepareStatement("DELETE FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ?");
            removeChannel.setLong(1, guildId);
             removeChannel.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //streamer stuff------------------------------------------------
    public void setStreamerMode(long guildId, boolean state) {
        try {
            if (state) {
                if (!SetStreamerModeCommand.streamerModes.contains(guildId)) {
                    PreparedStatement setStreamerMode = con.prepareStatement("INSERT INTO streamer_modes (guildId) VALUES (?)");
                    setStreamerMode.setLong(1, guildId);
                    setStreamerMode.executeUpdate();
                    setStreamerMode.close();
                }
            } else {
                PreparedStatement deleteStreamerMode = con.prepareStatement("DELETE FROM streamer_modes WHERE guildId= ?");
                deleteStreamerMode.setLong(1, guildId);
                deleteStreamerMode.executeUpdate();
                deleteStreamerMode.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean getStreamerMode(long guildId) {
        try {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM streamer_modes WHERE guildId= ?");
            getLogChannel.setLong(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            boolean s = false;
            while (rs.next()) s = rs.getBoolean("state");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setStreamUrl(long guildId, String url) {
        try {
            if (getStreamUrl(guildId) == null) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO stream_urls(guildId, url) VALUES (?, ?)");
                setPrefix.setLong(1, guildId);
                setPrefix.setString(2, url);
                setPrefix.executeUpdate();
                setPrefix.close();
                return true;
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE stream_urls SET url= ? WHERE guildId= ?");
                updatePrefix.setString(1, url);
                updatePrefix.setLong(2, guildId);
                updatePrefix.executeUpdate();
                updatePrefix.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getStreamUrl(long guildId) {
        try {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM stream_urls WHERE guildId= ?");
            getLogChannel.setLong(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            String s = null;
            while (rs.next()) s = rs.getString("url");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Command stuff---------------------------------------------------
    public void addCommand(Command command) {
        try {
            PreparedStatement newCommand = con.prepareStatement("INSERT INTO commands(commandName, gebruik, description, extra, category, aliases) VALUES (?, ?, ?, ? , ?, ?)");
            newCommand.setString(1, command.getCommandName());
            newCommand.setString(2, command.getUsage());
            newCommand.setString(3, command.getDescription());
            newCommand.setString(4, command.getExtra());
            newCommand.setString(5, String.valueOf(command.getCategory()));
            newCommand.setString(6, Arrays.toString(command.getAliases()));
            newCommand.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Mute role stuff--------------------------------------------------
    public long getRoleId(long guildId, RoleType type) {
        try {
            PreparedStatement getRoleId = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ?");
            getRoleId.setLong(1, guildId);
            ResultSet rs = getRoleId.executeQuery();
            while (rs.next()) {
                return rs.getLong("roleId");
            }
            return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean setRole(long guildId, long roleId, RoleType type) {
        try {
            if (roleId == -1L) {
                PreparedStatement setRole = con.prepareStatement("DELETE FROM " + type.toString().toLowerCase() + "_roles guildId= ?");
                setRole.setLong(1, guildId);
                setRole.executeUpdate();
                setRole.close();
                return true;
            }
            if (getRoleId(guildId, type) == -1) {
                PreparedStatement setRole = con.prepareStatement("INSERT INTO " + type.toString().toLowerCase() + "_roles (guildId, roleId) VALUES (?, ?)");
                setRole.setLong(1, guildId);
                setRole.setLong(2, roleId);
                setRole.executeUpdate();
                setRole.close();
                return true;
            } else {
                PreparedStatement updateRole = con.prepareStatement("UPDATE " + type.toString().toLowerCase() + "_roles SET roleId= ? WHERE guildId= ?");
                updateRole.setLong(1, roleId);
                updateRole.setLong(2, guildId);
                updateRole.executeUpdate();
                updateRole.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void removeRole(long guildId, RoleType type) {
        try {
            PreparedStatement remove = con.prepareStatement("DELETE * FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ?");
            remove.setLong(1, guildId);
            remove.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Filter stuff-----------------------------------------
    public void addFilter(long guildId, String mode, String content) {
        try {
            PreparedStatement addFilter = con.prepareStatement("INSERT INTO filters (guildId, mode, content) VALUES (?, ?, ?)");
            addFilter.setLong(1, guildId);
            addFilter.setString(2, mode);
            addFilter.setString(3, content);
            addFilter.executeUpdate();
            addFilter.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeFilter(long guildId, String mode, String content) {
        try {
            PreparedStatement addFilter = con.prepareStatement("DELETE FROM filters WHERE guildId= ? AND mode= ? AND content= ?");
            addFilter.setLong(1, guildId);
            addFilter.setString(2, mode);
            addFilter.setString(3, content);
            addFilter.executeUpdate();
            addFilter.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getFilters(long guildId, String mode) {
        List<String> filters = new ArrayList<>();
        try {
            PreparedStatement addFilter = con.prepareStatement("SELECT * FROM filters WHERE guildId= ? AND mode= ?");
            addFilter.setLong(1, guildId);
            addFilter.setString(2, mode);
            ResultSet rs = addFilter.executeQuery();
            while (rs.next()) {
                filters.add(rs.getString("content"));
            }
            addFilter.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return filters;
    }

    //Message stuff ---------------------------------------------------------
    public String getMessage(long guildId, MessageType type) {
        try {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_messages WHERE guildId= ?");
            getLogChannel.setLong(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            String s = null;
            while (rs.next()) s = rs.getString("content");
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean setMessage(long guildId, String content, MessageType type) {
        try {
            if (getMessage(guildId, type) == null) {
                PreparedStatement setPrefix = con.prepareStatement("INSERT INTO " + type.toString().toLowerCase() + "_messages (guildId, content) VALUES (?, ?)");
                setPrefix.setLong(1, guildId);
                setPrefix.setString(2, content);
                setPrefix.executeUpdate();
                setPrefix.close();
                return true;
            } else {
                PreparedStatement updatePrefix = con.prepareStatement("UPDATE " + type.toString().toLowerCase() + "_messages SET content= ? WHERE guildId= ?");
                updatePrefix.setString(1, content);
                updatePrefix.setLong(2, guildId);
                updatePrefix.executeUpdate();
                updatePrefix.close();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public HashMap<Long, String> getPrefixMap() {
        HashMap<Long, String> mapje = new HashMap<>();
        try {
            PreparedStatement gertjeuh = con.prepareStatement("SELECT * FROM prefixes");
            ResultSet rs = gertjeuh.executeQuery();
            while (rs.next()) {
                mapje.put(rs.getLong("guildId"), rs.getString("prefix"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public HashMap<Long, Long> getChannelMap(ChannelType type) {
        HashMap<Long, Long> mapje = new HashMap<>();
        try {
            PreparedStatement GERTJEUH = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_channels");
            ResultSet rs = GERTJEUH.executeQuery();
            while (rs.next()) {
                mapje.put(rs.getLong("guildId"), rs.getLong("channelId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public HashMap<Long, String> getMessageMap(MessageType leave) {
        HashMap<Long, String> mapje = new HashMap<>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM " + leave.toString().toLowerCase() + "_messages");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                mapje.put(rs.getLong("guildId"), rs.getString("content"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public HashMap<Long, String> getStreamUrlMap() {
        HashMap<Long, String> mapje = new HashMap<>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM stream_urls");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                mapje.put(rs.getLong("guildId"), rs.getString("url"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public HashMap<Long, Long> getRoleMap(RoleType type) {
        HashMap<Long, Long> mapje = new HashMap<>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_roles");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                mapje.put(rs.getLong("guildId"), rs.getLong("roleId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public List<Long> getStreamerModeList() {
        List<Long> lijstje = new ArrayList<>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM streamer_modes");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                if (!lijstje.contains(rs.getLong("guildId")))
                    lijstje.add(rs.getLong("guildId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lijstje;
    }

    public JSONObject getVotesObject(long userId) {
        JSONObject toReturn = new JSONObject();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM votes WHERE userId= ?");
            statement.setLong(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                toReturn.put("votes", rs.getLong("votes"));
                toReturn.put("streak", rs.getLong("streak"));
                toReturn.put("lastTime", rs.getLong("lastTime"));
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        return toReturn;
    }

    public HashMap<Long, ArrayList<Long>> getNotificationsMap(NotificationType nextvote) {
        //userId -> mensen waarvan notificatie moet krijgen -> aan of uit
        HashMap<Long, ArrayList<Long>> mapje = new HashMap<>();
        try {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM " + nextvote.toString().toLowerCase() + "_notifications");
            ResultSet rs = statement.executeQuery();
            List<Long> row = new ArrayList<>();
            while (rs.next()) {
                if (!row.contains(rs.getLong("userId")))
                    row.add(rs.getLong("userId"));
            }
            for (long s : row) {
                rs.beforeFirst();
                ArrayList<Long> lijst = new ArrayList<>();
                while (rs.next()) {
                    if (rs.getLong("userId") == s) {
                        lijst.add(rs.getLong("targetId"));
                    }
                }
                mapje.put(s, lijst);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public void putNotifcation(long userId, long targetId, NotificationType type) {
        try {
            PreparedStatement insert = con.prepareStatement("INSERT INTO " + type.toString().toLowerCase() + "_notifications (userId, targetId) VALUES (?, ?)");
            insert.setLong(1, userId);
            insert.setLong(2, targetId);
            insert.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removeNotification(long userId, long targetId, NotificationType type) {
        try {
            PreparedStatement remove = con.prepareStatement("DELETE FROM " + type.toString().toLowerCase() + "_notifications WHERE userId= ? AND targetId= ?");
            remove.setLong(1, userId);
            remove.setLong(2, targetId);
            remove.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Long> getVoteList() {
        ArrayList<Long> list = new ArrayList<>();
        try {
            long currentTime = System.currentTimeMillis();
            long yesterdayandminute = currentTime - 86_460_000L;
            long yesterday = currentTime - 86_400_000L;
            PreparedStatement getVoteMap = con.prepareStatement("SELECT * FROM votes WHERE lastTime BETWEEN ? AND ?");
            getVoteMap.setLong(1, yesterdayandminute);
            getVoteMap.setLong(2, yesterday);
            ResultSet rs = getVoteMap.executeQuery();
            while (rs.next()) {
                list.add(rs.getLong("userId"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public HashMap<Long,List<Integer>> getPermissionsMap(int i) {
        HashMap<Long, List<Integer>> permissions = new HashMap<>();
        try {
            String type = i == 0 ? "user" : "role";
            PreparedStatement statement = con.prepareStatement("SELECT * FROM perms_" + type + "s");
            ResultSet rs = statement.executeQuery();
            List<Long> row = new ArrayList<>();
            while (rs.next()) {
                if (!row.contains(rs.getLong(type + "Id")))
                    row.add(rs.getLong(type + "Id"));
            }
            for (long s : row) {
                rs.beforeFirst();
                ArrayList<Integer> lijst = new ArrayList<>();
                while (rs.next()) {
                    if (rs.getLong(type + "Id") == s) {
                        lijst.add(Helpers.perms.indexOf(rs.getString("permission")));
                    }
                }
                permissions.put(s, lijst);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return permissions;
    }
}
