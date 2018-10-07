package me.melijn.jda.db;

import com.google.common.collect.Sets;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gnu.trove.list.TIntList;
import gnu.trove.list.TLongList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import groovy.util.MapEntry;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import me.melijn.jda.commands.management.DisableCommand;
import me.melijn.jda.commands.management.SetLogChannelCommand;
import me.melijn.jda.commands.management.SetPrefixCommand;
import me.melijn.jda.commands.management.SetStreamerModeCommand;
import me.melijn.jda.utils.MessageHelper;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.awt.*;
import java.sql.*;
import java.time.LocalTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static me.melijn.jda.utils.MessageHelper.spaces;

public class MySQL {

    private String ip;
    private String pass;
    private String user;
    private String dbname;
    private HikariDataSource ds;
    private Logger logger = LogManager.getLogger(MySQL.class.getName());


    private long currentTime = 0;
    private int currentHour = 25; //Because 25 isn't a valid hour it will be changed on the first updateUsage call

    public MySQL(String ip, String user, String pass, String dbname) {
        this.ip = ip;
        this.user = user;
        this.pass = pass;
        this.dbname = dbname;
        connect();
    }

    private void connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:mysql://" + this.ip + ":3306/" + this.dbname);
            config.setUsername(this.user);
            config.setPassword(this.pass);
            config.setMaximumPoolSize(20);
            config.addDataSourceProperty("autoReconnect", "true");
            config.addDataSourceProperty("useUnicode", "true");
            config.addDataSourceProperty("useSSL", "false");
            config.addDataSourceProperty("useLegacyDatetimeCode", "false");
            config.addDataSourceProperty("serverTimezone", "UTC");
            //https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
            config.addDataSourceProperty("allowMultiQueries", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "350");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("useServerPrepStmts", "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("useLocalTransactionState", "true");

            ds = new HikariDataSource(config);

            Statement statement = ds.getConnection().createStatement();
            statement.executeQuery("SET NAMES 'utf8mb4'");
            statement.close();
            logger.info("[MySQL] has connected & Loading init");
            executeUpdate("CREATE TABLE IF NOT EXISTS commands(commandName varchar(1000), gebruik varchar(1000), description varchar(2000), extra varchar(2000), category varchar(100), aliases varchar(200));");
            executeUpdate("CREATE TABLE IF NOT EXISTS disabled_commands(guildId bigint, command int)");
            executeUpdate("CREATE TABLE IF NOT EXISTS stream_urls(guildId bigint, url varchar(2000))");
            executeUpdate("CREATE TABLE IF NOT EXISTS prefixes(guildId bigint, prefix bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS mute_roles(guildId bigint, roleId bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS join_roles(guildId bigint, roleId bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS unverified_roles(guildId bigint, roleId bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS perms_roles(guildId bigint, roleId bigint, permission varchar(256));");
            executeUpdate("CREATE TABLE IF NOT EXISTS perms_users(guildId bigint, userId bigint, permission varchar(256));");

            //channels
            executeUpdate("CREATE TABLE IF NOT EXISTS ban_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS mute_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS kick_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS warn_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS sdm_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS odm_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS pm_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS fm_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS music_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS welcome_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS music_log_channels(guildId bigint, channelId bigint)");
            executeUpdate("CREATE TABLE IF NOT EXISTS verification_channels(guildId bigint, channelId bigint)");

            executeUpdate("CREATE TABLE IF NOT EXISTS verification_thresholds(guildId bigint, threshold tinyint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS unverified_users(guildId bigint, userId bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS verification_codes(guildId bigint, code varchar(2000));");
            executeUpdate("CREATE TABLE IF NOT EXISTS streamer_modes(guildId bigint, state boolean)");
            executeUpdate("CREATE TABLE IF NOT EXISTS filters(guildId bigint, mode varchar(16), content varchar(2000))");
            executeUpdate("CREATE TABLE IF NOT EXISTS warns(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), moment bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS kicks(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), moment bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS active_bans(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), startTime bigint, endTime bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS history_bans(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), unbanReason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            executeUpdate("CREATE TABLE IF NOT EXISTS active_mutes(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), startTime bigint, endTime bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS history_mutes(guildId bigint, victimId bigint, authorId bigint, reason varchar(2000), unmuteReason varchar(2000), startTime bigint, endTime bigint, active boolean);");
            executeUpdate("CREATE TABLE IF NOT EXISTS history_messages(guildId bigint, authorId bigint, messageId bigint, content varchar(2000), textChannelId bigint, sentTime bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS join_messages(guildId bigint, content varchar(2000))");
            executeUpdate("CREATE TABLE IF NOT EXISTS leave_messages(guildId bigint, content varchar(2000))");
            executeUpdate("CREATE TABLE IF NOT EXISTS votes(userId bigint, votes bigint, streak bigint, lastTime bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS nextvote_notifications(userId bigint, targetId bigint);");
            executeUpdate("CREATE TABLE IF NOT EXISTS command_usage(commandId int, usageCount bigint, time bigint, UNIQUE KEY(commandId, time))");
            logger.info("[MySQL] init loaded");
        } catch (SQLException e) {
            logger.log(Level.ERROR, "[MySQL] did not connect -> ");
            e.printStackTrace();
            System.exit(500);
        }
    }

    public void executeUpdate(final String query, final Object... objects) {
        try (final Connection connection = ds.getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                int current = 1;
                for (final Object object : objects) {
                    preparedStatement.setObject(current, object);
                    current++;
                }
                preparedStatement.executeUpdate();
            }
        } catch (final SQLException e) {
            System.out.println("Something went wrong while executing query method the query: " + query);
            e.printStackTrace();
        }
    }

    public void executeQuery(final String sql, final Consumer<ResultSet> consumer, final Object... objects) {
        try (final Connection connection = ds.getConnection()) {
            try (final PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                int current = 1;
                for (final Object object : objects) {
                    preparedStatement.setObject(current, object);
                    current++;
                }
                final ResultSet resultSet = preparedStatement.executeQuery();
                consumer.accept(resultSet);
                resultSet.close();
            }
        } catch (final SQLException e) {
            System.out.println("Something went wrong while executing query method the query: " + sql);
            e.printStackTrace();
        }
    }

    public long getMessageAuthorId(long messageId) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getAuthor = con.prepareStatement("SELECT * FROM history_messages WHERE messageId= ?");
            getAuthor.setLong(1, messageId);
            ResultSet rs = getAuthor.executeQuery();
            long temp = -1;
            if (rs.next()) {
                temp = rs.getLong("authorId");
            }
            rs.close();
            getAuthor.close();
            return temp;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public JSONObject getMessageObject(long messageId) {
        JSONObject jsonObject = new JSONObject();
        try (Connection con = ds.getConnection()) {
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
            rs.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public void createMessage(long messageId, String content, long authorId, long guildId, long textChannelId) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement createMessage = con.prepareStatement("INSERT INTO history_messages(guildId, authorId, messageId, content, textChannelId, sentTime) VALUES (?, ?, ?, ?, ?, ?)");
            createMessage.setLong(1, guildId);
            createMessage.setLong(2, authorId);
            createMessage.setLong(3, messageId);
            createMessage.setString(4, content);
            createMessage.setLong(5, textChannelId);
            createMessage.setLong(6, System.currentTimeMillis());
            createMessage.executeUpdate();
            createMessage.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //Permissions stuff---------------------------------------------------------
    public void addRolePermission(long guildId, long roleId, String permission) {
        try (Connection con = ds.getConnection()) {
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
        try (Connection con = ds.getConnection()) {
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
        try (Connection con = ds.getConnection()) {
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
        try (Connection con = ds.getConnection()) {
            PreparedStatement removing = con.prepareStatement("DELETE FROM perms_users WHERE guildId= ? AND userId= ? AND permission= ?");
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
        try (Connection con = ds.getConnection()) {
            PreparedStatement getting = con.prepareStatement("SELECT * FROM perms_users WHERE guildId= ? AND userId= ? AND permission= ?");
            getting.setLong(1, guild.getIdLong());
            getting.setLong(2, userId);
            getting.setString(3, permission);
            ResultSet rs = getting.executeQuery();
            boolean temp = false;
            if (rs.next()) {
                temp = true;
            }
            getting.close();
            rs.close();
            if (temp) return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Role> roles = new ArrayList<>(guild.getMemberById(userId).getRoles());
        roles.add(guild.getPublicRole());
        for (Role role : roles) {
            try (Connection con = ds.getConnection()) {
                PreparedStatement getting = con.prepareStatement("SELECT * FROM perms_roles WHERE guildId= ? AND roleId= ? AND permission= ?");
                getting.setLong(1, guild.getIdLong());
                getting.setLong(2, role.getIdLong());
                getting.setString(3, permission);
                ResultSet rs = getting.executeQuery();
                boolean temp = false;
                if (rs.next()) {
                    temp = true;
                }
                getting.close();
                rs.close();
                return temp;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public void clearRolePermissions(long guildId, long roleId) {
        try (Connection con = ds.getConnection()) {
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
        try (Connection con = ds.getConnection()) {
            PreparedStatement clearing = con.prepareStatement("DELETE FROM perms_users WHERE guildId= ? AND userId= ?");
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
        try (Connection con = ds.getConnection()) {
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
        try (Connection con = ds.getConnection()) {
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
        try (Connection con = ds.getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM perms_roles WHERE guildId= ? AND permission= ?");
            statement.setLong(1, guildId);
            statement.setString(2, permission);
            ResultSet rs = statement.executeQuery();
            boolean temp = false;
            if (!rs.next()) {
                temp = true;
            }
            statement.close();
            rs.close();
            if (temp) return true;

            PreparedStatement statement1 = con.prepareStatement("SELECT * FROM perms_users WHERE guildId= ? AND permission= ?");
            statement1.setLong(1, guildId);
            statement1.setString(2, permission);
            ResultSet rs1 = statement1.executeQuery();
            if (!rs.next()) {
                temp = true;
            }
            statement1.close();
            rs1.close();
            if (temp) return true;
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
        executeUpdate("INSERT INTO prefixes (guildId, prefix) VALUES (?, ?) ON DUPLICATE KEY UPDATE prefix= ?",
                guildId, prefix, prefix);
        SetPrefixCommand.prefixes.put(guildId, prefix);
    }

    public String getPrefix(long guildId) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getPrefix = con.prepareStatement("SELECT * FROM prefixes WHERE guildId= ?");
            getPrefix.setLong(1, guildId);
            ResultSet rs = getPrefix.executeQuery();
            if (rs.next()) {
                return rs.getString("prefix");
            }
            rs.close();
            getPrefix.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Melijn.PREFIX;
    }

    //Punishment stuff--------------------------------------------------------------
    public boolean setTempBan(User author, User target, Guild guild, String reasonRaw, long seconds) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        if (seconds > 0) {
            long moment = System.currentTimeMillis();
            long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            String namet = target.getName() + "#" + target.getDiscriminator();
            String name = author.getName() + "#" + author.getDiscriminator();
            EmbedBuilder banned = new EmbedBuilder();
            banned.setColor(Color.RED);
            banned.setDescription("```LDIF\nBanned: " + namet + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + MessageHelper.millisToDate(moment) + "\nUntil: " + MessageHelper.millisToDate(until) + "```");
            banned.setThumbnail(target.getEffectiveAvatarUrl());
            banned.setAuthor("Banned by: " + name + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

            if (!target.isBot())
                target.openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(banned.build()).queue(
                        (success) -> guild.getController().ban(target.getId(), 7, reason).queue(),
                        (failed) -> guild.getController().ban(target.getId(), 7, reason).queue()
                ), (failed) -> guild.getController().ban(target.getId(), 7, reason).queue());
            long logChannelId = SetLogChannelCommand.banLogChannelCache.getUnchecked(guild.getIdLong());
            if (guild.getTextChannelById(logChannelId) != null) {
                if (target.isBot())
                    guild.getTextChannelById(logChannelId).sendMessage(banned.build() + "\nTarget is a bot").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(banned.build()).queue();
            }
            executeUpdate("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                            "INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until, author.getIdLong(), reason, moment, until,
                    guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until, true);
            return true;
        }
        return false;
    }

    public boolean setPermBan(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        String nameTarget = target.getName() + "#" + target.getDiscriminator();
        String name = author.getName() + "#" + author.getDiscriminator();
        EmbedBuilder banned = new EmbedBuilder();
        banned.setColor(Color.RED);
        banned.setDescription("```LDIF\nBanned: " + nameTarget + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + MessageHelper.millisToDate(moment) + "```");
        banned.setThumbnail(target.getEffectiveAvatarUrl());
        banned.setAuthor("Banned by: " + name + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

        if (!target.isBot())
            target.openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(banned.build()).queue(
                    (success) -> guild.getController().ban(target.getId(), 7, reason).queue(),
                    (failed) -> guild.getController().ban(target.getId(), 7, reason).queue()
            ), (failed) -> guild.getController().ban(target.getId(), 7, reason).queue());
        long logChannelId = SetLogChannelCommand.banLogChannelCache.getUnchecked(guild.getIdLong());
        if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
            if (target.isBot())
                guild.getTextChannelById(logChannelId).sendMessage(banned.build() + "\nTarget is a bot").queue();
            else guild.getTextChannelById(logChannelId).sendMessage(banned.build()).queue();
        }
        executeUpdate("INSERT INTO active_bans (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                        "INSERT INTO history_bans (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null, author.getIdLong(), reason, moment, null,
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null, true);
        return true;
    }

    public boolean unban(User toUnban, Guild guild, User author, String reason) {
        if (toUnban != null) {
            executeQuery("SELECT * FROM active_bans WHERE guildId= ? AND victimId= ?", rs -> {
                try {
                    if (rs.next()) {
                        executeUpdate("UPDATE history_bans SET active= ? AND unbanReason= ? WHERE victimId= ? AND guildId= ?; " +
                                        "DELETE FROM active_bans WHERE guildId= ? AND victimId= ?",
                                false, reason, toUnban.getIdLong(), guild.getIdLong(),
                                guild.getIdLong(), toUnban.getIdLong());
                    }
                    rs.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, guild.getIdLong(), toUnban.getIdLong());
            EmbedBuilder eb = new EmbedBuilder();
            eb.setAuthor("Unbanned by: " + author.getName() + "#" + author.getDiscriminator() + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
            eb.setDescription("```LDIF\nUnbanned: " + toUnban.getName() + "#" + toUnban.getDiscriminator() + "\nTargetID: " + toUnban.getId() + "\nReason: " + reason + "\nGuild: " + guild.getName() + "\nMoment: " + MessageHelper.millisToDate(System.currentTimeMillis()) + "```");
            eb.setThumbnail(toUnban.getEffectiveAvatarUrl());
            eb.setColor(Color.green);

            if (!toUnban.isBot() && !toUnban.isFake())
                toUnban.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
            long logChannelId = SetLogChannelCommand.banLogChannelCache.getUnchecked(guild.getIdLong());
            if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                if (toUnban.isBot())
                    guild.getTextChannelById(logChannelId).sendMessage(eb.build() + "\nTarget is a bot").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(eb.build()).queue();
            }


            guild.getController().unban(toUnban.getId()).queue();
            return true;
        }
        return false;
    }

    public boolean addWarn(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        executeUpdate("INSERT INTO warns(guildId, victimId, authorId, reason, moment) VALUES (?, ?, ?, ?, ?);",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, System.currentTimeMillis());
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor("Warned by: " + author.getName() + "#" + author.getDiscriminator() + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
        embedBuilder.setDescription("```LDIF\nWarned: " + target.getName() + "#" + target.getDiscriminator() + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + MessageHelper.millisToDate(System.currentTimeMillis()) + "\n```");
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());
        embedBuilder.setColor(Color.yellow);

        long logChannelId = SetLogChannelCommand.warnLogChannelCache.getUnchecked(guild.getIdLong());
        if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
            if (target.isBot())
                guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build() + "\nTarget is a bot.").queue();
            else guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build()).queue();
        }
        if (!target.isBot()) target.openPrivateChannel().queue((m) -> m.sendMessage(embedBuilder.build()).queue());
        return true;
    }

    public boolean setTempMute(User author, User target, Guild guild, String reasonRaw, long seconds) {
        if (seconds > 0) {
            final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
            long moment = System.currentTimeMillis();
            long until = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
            String nameTarget = target.getName() + "#" + target.getDiscriminator();
            String name = author.getName() + "#" + author.getDiscriminator();
            EmbedBuilder muted = new EmbedBuilder();
            muted.setColor(Color.BLUE);
            muted.setDescription("```LDIF\nMuted: " + nameTarget + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nFrom: " + MessageHelper.millisToDate(moment) + "\nUntil: " + MessageHelper.millisToDate(until) + "```");
            muted.setThumbnail(target.getEffectiveAvatarUrl());
            muted.setAuthor("Muted by: " + name + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

            if (!target.isBot()) target.openPrivateChannel().queue(pc -> pc.sendMessage(muted.build()).queue());
            long logChannelId = SetLogChannelCommand.muteLogChannelCache.getUnchecked(guild.getIdLong());
            if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                if (target.isBot())
                    guild.getTextChannelById(logChannelId).sendMessage(muted.build() + "\nTarget is a bot").queue();
                else guild.getTextChannelById(logChannelId).sendMessage(muted.build()).queue();
            }
            executeUpdate("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                            "INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until,
                    author.getIdLong(), reason, moment, until,
                    guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, until, true);
            return true;
        }
        return false;
    }

    public boolean setPermMute(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        long moment = System.currentTimeMillis();
        String nameTarget = target.getName() + "#" + target.getDiscriminator();
        String name = author.getName() + "#" + author.getDiscriminator();
        EmbedBuilder muted = new EmbedBuilder();
        muted.setColor(Color.BLUE);
        muted.setDescription("```LDIF\nMuted: " + nameTarget + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + MessageHelper.millisToDate(moment) + "```");
        muted.setThumbnail(target.getEffectiveAvatarUrl());
        muted.setAuthor("Muted by: " + name + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());

        if (!target.isBot()) target.openPrivateChannel().queue(pc -> pc.sendMessage(muted.build()).queue());
        long logChannelId = SetLogChannelCommand.muteLogChannelCache.getUnchecked(guild.getIdLong());
        if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
            if (target.isBot())
                guild.getTextChannelById(logChannelId).sendMessage(muted.build() + "\nTarget is a bot").queue();
            else guild.getTextChannelById(logChannelId).sendMessage(muted.build()).queue();
        }
        executeUpdate("INSERT INTO active_mutes (guildId, victimId, authorId, reason, startTime, endTime) VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE authorId= ?, reason= ?, startTime= ?, endTime= ?; " +
                        "INSERT INTO history_mutes (guildId, victimId, authorId, reason, startTime, endTime, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null,
                author.getIdLong(), reason, moment, null,
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, moment, null, true);
        return true;
    }

    public boolean unmute(Guild guild, User toUnmute, User author, String reason) {
        if (toUnmute != null) {
            executeQuery("SELECT * FROM active_mutes WHERE guildId= ? AND victimId= ?", rs -> {
                try {
                    if (rs.next()) {
                        executeUpdate("UPDATE history_mutes SET active= ? AND unmuteReason= ? WHERE victimId= ? AND guildId= ?; " +
                                        "DELETE FROM active_mutes WHERE guildId= ? AND victimId= ?",
                                false, reason, toUnmute.getIdLong(), guild.getIdLong(),
                                guild.getIdLong(), toUnmute.getIdLong());

                        EmbedBuilder eb = new EmbedBuilder();
                        eb.setAuthor("Unmuted by: " + author.getName() + "#" + author.getDiscriminator() + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
                        eb.setDescription("```LDIF" + "\nUnmuted: " + toUnmute.getName() + "#" + toUnmute.getDiscriminator() + "\nTargetID: " + toUnmute.getId() + "\nReason: " + reason + "\nGuild: " + guild.getName() + "\nMoment: " + MessageHelper.millisToDate(System.currentTimeMillis()) + "```");
                        eb.setThumbnail(toUnmute.getEffectiveAvatarUrl());
                        eb.setColor(Color.green);

                        if (!toUnmute.isBot() && !toUnmute.isFake())
                            toUnmute.openPrivateChannel().queue(s -> s.sendMessage(eb.build()).queue());
                        long logChannelId = SetLogChannelCommand.muteLogChannelCache.getUnchecked(guild.getIdLong());
                        if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
                            if (toUnmute.isBot())
                                guild.getTextChannelById(logChannelId).sendMessage(eb.build() + "\nTarget is a bot").queue();
                            else guild.getTextChannelById(logChannelId).sendMessage(eb.build()).queue();
                        }
                        if (guild.getMember(toUnmute) != null)
                            guild.getController().removeSingleRoleFromMember(guild.getMember(toUnmute), guild.getRoleById(getRoleId(guild.getIdLong(), RoleType.MUTE))).queue();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }, guild.getIdLong(), toUnmute.getIdLong());
            return true;
        }
        return false;
    }

    public boolean addKick(User author, User target, Guild guild, String reasonRaw) {
        final String reason = reasonRaw.matches("\\s+|") ? "N/A" : reasonRaw;
        executeUpdate("INSERT INTO kicks(guildId, victimId, authorId, reason, moment) VALUES (?, ?, ?, ?, ?);",
                guild.getIdLong(), target.getIdLong(), author.getIdLong(), reason, System.currentTimeMillis());
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor("Kicked by: " + author.getName() + "#" + author.getDiscriminator() + spaces.substring(0, 45 - author.getName().length()) + "\u200B", null, author.getEffectiveAvatarUrl());
        embedBuilder.setDescription("```LDIF\nKicked: " + target.getName() + "#" + target.getDiscriminator() + "\nTargetID: " + target.getId() + "\nReason: " + reason.replaceAll("`", "´").replaceAll("\n", " ") + "\nGuild: " + guild.getName() + "\nMoment: " + MessageHelper.millisToDate(System.currentTimeMillis()) + "```");
        embedBuilder.setThumbnail(target.getEffectiveAvatarUrl());
        embedBuilder.setColor(Color.ORANGE);

        long logChannelId = SetLogChannelCommand.kickLogChannelCache.getUnchecked(guild.getIdLong());
        if (logChannelId != -1 && guild.getTextChannelById(logChannelId) != null) {
            if (target.isBot())
                guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build() + "\nTarget is a bot.").queue();
            else guild.getTextChannelById(logChannelId).sendMessage(embedBuilder.build()).queue();
        }
        if (!target.isBot()) target.openPrivateChannel().queue((channel) -> {
            channel.sendMessage(embedBuilder.build()).queue();
            guild.getController().kick(guild.getMember(target)).queueAfter(1, TimeUnit.SECONDS);
        });
        return true;
    }

    //Punishment getters
    public void getUserBans(long guildId, long userId, JDA jda, Consumer<String[]> bans) {
        try {
            PreparedStatement getBans = ds.getConnection().prepareStatement("SELECT * FROM history_bans WHERE victimId= ? AND guildId= ?");
            getBans.setLong(1, userId);
            getBans.setLong(2, guildId);
            ResultSet rs = getBans.executeQuery();

            Set<JSONObject> set = Sets.newHashSet();
            while (rs.next()) {
                set.add(new JSONObject()
                        .put("authorId", rs.getLong("authorId"))
                        .put("reason", rs.getString("reason"))
                        .put("unbanReason", rs.getString("unbanReason") == null ? "" : rs.getString("unbanReason"))
                        .put("startTime", rs.getLong("startTime"))
                        .put("endTime", rs.getString("endTime") == null ? "" : rs.getString("endTime"))
                        .put("active", rs.getBoolean("active"))
                );
            }
            rs.close();
            getBans.close();
            if (set.size() == 0) {
                bans.accept(new String[]{"No bans"});
            }
            List<String> toRet = new ArrayList<>();
            AtomicInteger progress = new AtomicInteger();
            for (JSONObject rowObj : set) {
                String endTime = rowObj.getString("endTime").equals("") ? "Infinity" : MessageHelper.millisToDate(Long.valueOf(rowObj.getString("endTime")));
                jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                    if (rowObj.getBoolean("active"))
                        toRet.add("```ini" +
                                "\n[Banned by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                                "\n[Reason]: " + rowObj.getString("reason") +
                                "\n[From]: " + MessageHelper.millisToDate(rowObj.getLong("startTime")) +
                                "\n[Until]: " + endTime +
                                "\n[active]: " + rowObj.getBoolean("active") + "```");
                    else {
                        String toAdd = "";
                        toAdd += "```ini" +
                                "\n[Banned by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                                "\n[Reason]: " + rowObj.getString("reason");
                        if (rowObj.get("unbanReason") != null && !rowObj.getBoolean("active"))
                            toAdd += "\n[UnbanReason]: " + (rowObj.get("unbanReason") == null ? "N/A" : rowObj.getString("unbanReason"));
                        toAdd += "\n[From]: " + MessageHelper.millisToDate(rowObj.getLong("startTime")) +
                                "\n[Until]: " + endTime +
                                "\n[active]: " + rowObj.getBoolean("active") + "```";
                        toRet.add(toAdd);
                    }
                    if (progress.incrementAndGet() == set.size()) {
                        bans.accept(toRet.toArray(new String[0]));
                    }
                }, (failed) -> {
                    toRet.add("```ini" +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[From]: " + MessageHelper.millisToDate(rowObj.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rowObj.getBoolean("active") + "```");
                    if (progress.incrementAndGet() == set.size()) {
                        bans.accept(toRet.toArray(new String[0]));
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            bans.accept(new String[]{"SQL Error :/ Contact support"});
        }
    }

    public void getUserMutes(long guildId, long userId, JDA jda, Consumer<String[]> mutes) {
        try {
            PreparedStatement getMutes = ds.getConnection().prepareStatement("SELECT * FROM history_mutes WHERE victimId= ? AND guildId= ?");
            getMutes.setLong(1, userId);
            getMutes.setLong(2, guildId);
            ResultSet rs = getMutes.executeQuery();

            Set<JSONObject> set = Sets.newHashSet();
            while (rs.next()) {
                set.add(new JSONObject()
                        .put("authorId", rs.getLong("authorId"))
                        .put("reason", rs.getString("reason"))
                        .put("unmuteReason", rs.getString("unmuteReason") == null ? "" : rs.getString("unmuteReason"))
                        .put("startTime", rs.getLong("startTime"))
                        .put("endTime", rs.getString("endTime") == null ? "" : rs.getString("endTime"))
                        .put("active", rs.getBoolean("active"))
                );
            }
            rs.close();
            getMutes.close();
            if (set.size() == 0) {
                mutes.accept(new String[]{"No mutes"});
            }
            List<String> toRet = new ArrayList<>();
            AtomicInteger progress = new AtomicInteger();
            for (JSONObject rowObj : set) {
                String endTime = rowObj.getString("endTime").equals("") ? "Infinity" : MessageHelper.millisToDate(Long.valueOf(rowObj.getString("endTime")));
                jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                    if (rowObj.getBoolean("active"))
                        toRet.add("```ini" +
                                "\n[Muted by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                                "\n[Reason]: " + rowObj.getString("reason") +
                                "\n[From]: " + MessageHelper.millisToDate(rowObj.getLong("startTime")) +
                                "\n[Until]: " + endTime +
                                "\n[active]: " + rowObj.getBoolean("active") + "```");
                    else {
                        String toAdd = "";
                        toAdd += "```ini" +
                                "\n[Muted by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                                "\n[Reason]: " + rowObj.getString("reason");
                        if (!rowObj.get("unmuteReason").equals("") && !rowObj.getBoolean("active"))
                            toAdd += "\n[UnmuteReason]: " + (rowObj.get("unmuteReason") == null ? "N/A" : rowObj.getString("unmuteReason"));
                        toAdd += "\n[From]: " + MessageHelper.millisToDate(rowObj.getLong("startTime")) +
                                "\n[Until]: " + endTime +
                                "\n[active]: " + rowObj.getBoolean("active") + "```";
                        toRet.add(toAdd);
                    }
                    if (progress.incrementAndGet() == set.size()) {
                        mutes.accept(toRet.toArray(new String[0]));
                    }
                }, (failed) -> {
                    toRet.add("```ini" +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[From]: " + MessageHelper.millisToDate(rowObj.getLong("startTime")) +
                            "\n[Until]: " + endTime +
                            "\n[active]: " + rowObj.getBoolean("active") + "```");
                    if (progress.incrementAndGet() == set.size()) {
                        mutes.accept(toRet.toArray(new String[0]));
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            mutes.accept(new String[]{"SQL Error :/ Contact support"});
        }
    }

    public boolean isUserMuted(long guildId, long userId) {
        var ref = new Object() {
            boolean ret = false;
        };
        executeQuery("SELECT * FROM active_mutes WHERE victimId= ? AND guildId= ?", rs -> {
            try {
                ref.ret = rs.next();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }, userId, guildId);
        return ref.ret;
    }

    public void getUserWarns(long guildId, long userId, JDA jda, Consumer<String[]> warns) {
        try {
            PreparedStatement getWarns = ds.getConnection().prepareStatement("SELECT * FROM warns WHERE victimId= ? AND guildId= ?");
            getWarns.setLong(1, userId);
            getWarns.setLong(2, guildId);
            ResultSet rs = getWarns.executeQuery();

            Set<JSONObject> set = Sets.newHashSet();
            while (rs.next()) {
                set.add(new JSONObject()
                        .put("authorId", rs.getLong("authorId"))
                        .put("reason", rs.getString("reason"))
                        .put("moment", rs.getLong("moment"))
                );
            }
            rs.close();
            getWarns.close();
            if (set.size() == 0) {
                warns.accept(new String[]{"No warns"});
            }
            List<String> toRet = new ArrayList<>();
            AtomicInteger progress = new AtomicInteger();
            for (JSONObject rowObj : set) {
                jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                    toRet.add("```ini" +
                            "\n[Warned by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[Moment]: " + MessageHelper.millisToDate(rowObj.getLong("moment")) + "```");
                    if (progress.incrementAndGet() == set.size()) {
                        warns.accept(toRet.toArray(new String[0]));
                    }
                }, (failed) -> {
                    toRet.add("```ini" +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[Moment]: " + MessageHelper.millisToDate(rowObj.getLong("moment")) + "```");
                    if (progress.incrementAndGet() == set.size()) {
                        warns.accept(toRet.toArray(new String[0]));
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            warns.accept(new String[]{"SQL Error :/ Contact support"});
        }
    }


    public void getUserKicks(long guildId, long userId, JDA jda, Consumer<String[]> kicks) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getKicks = con.prepareStatement("SELECT * FROM kicks WHERE victimId= ? AND guildId= ?");
            getKicks.setLong(1, userId);
            getKicks.setLong(2, guildId);
            ResultSet rs = getKicks.executeQuery();

            Set<JSONObject> set = Sets.newHashSet();
            while (rs.next()) {
                set.add(new JSONObject()
                        .put("authorId", rs.getLong("authorId"))
                        .put("reason", rs.getString("reason"))
                        .put("moment", rs.getLong("moment"))
                );
            }
            rs.close();
            getKicks.close();
            if (set.size() == 0) {
                kicks.accept(new String[]{"No kicks"});
            }
            List<String> toRet = new ArrayList<>();
            AtomicInteger progress = new AtomicInteger();
            for (JSONObject rowObj : set) {
                jda.asBot().getShardManager().retrieveUserById(rowObj.getLong("authorId")).queue(staff -> {
                    toRet.add("```ini" +
                            "\n[Kicked by]: " + staff.getName() + "#" + staff.getDiscriminator() +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[Moment]: " + MessageHelper.millisToDate(rowObj.getLong("moment")) + "```");
                    if (progress.incrementAndGet() == set.size()) {
                        kicks.accept(toRet.toArray(new String[0]));
                    }
                }, (failed) -> {
                    toRet.add("```ini" +
                            "\n[Reason]: " + rowObj.getString("reason") +
                            "\n[Moment]: " + MessageHelper.millisToDate(rowObj.getLong("moment")) + "```");
                    if (progress.incrementAndGet() == set.size()) {
                        kicks.accept(toRet.toArray(new String[0]));
                    }
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            kicks.accept(new String[]{"SQL Error :/ Contact support"});
        }
    }


    //log channel stuff----------------------------------------------------------340081887265685504-467770083326951446

    public boolean setChannel(long guildId, long channelId, ChannelType type) {
        executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_channels (guildId, channelId) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE channelId= ?",
                guildId, channelId, channelId);
        return true;
    }

    public long getChannelId(long guildId, ChannelType type) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ?");
            getChannel.setLong(1, guildId);
            ResultSet rs = getChannel.executeQuery();
            long s = -1;
            while (rs.next()) s = rs.getLong("channelId");
            rs.close();
            getChannel.close();
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void removeChannel(long guildId, ChannelType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_channels WHERE guildId= ?",
                guildId);
    }

    //streamer stuff------------------------------------------------
    public void setStreamerMode(long guildId, boolean state) {
        if (state) {
            if (!SetStreamerModeCommand.streamerModeCache.getUnchecked(guildId)) {
                executeUpdate("INSERT INTO streamer_modes (guildId) VALUES (?)", guildId);
            }
        } else {
            executeUpdate("DELETE FROM streamer_modes WHERE guildId= ?", guildId);
        }
    }

    public boolean getStreamerMode(long guildId) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM streamer_modes WHERE guildId= ?");
            getLogChannel.setLong(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            boolean s = rs.next();
            rs.close();
            getLogChannel.close();
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean setStreamUrl(long guildId, String url) {
        executeUpdate("INSERT INTO stream_urls(guildId, url) VALUES (?, ?) ON DUPLICATE KEY UPDATE url= ?",
                guildId, url, url);
        return true;
    }

    public String getStreamUrl(long guildId) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getStreamUrl = con.prepareStatement("SELECT * FROM stream_urls WHERE guildId= ?");
            getStreamUrl.setLong(1, guildId);
            ResultSet rs = getStreamUrl.executeQuery();
            String s = "";
            while (rs.next()) s = rs.getString("url");
            rs.close();
            getStreamUrl.close();
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    //Command stuff---------------------------------------------------
    public void addCommand(Command command) {
        executeUpdate("INSERT INTO commands(commandName, gebruik, description, extra, category, aliases) VALUES (?, ?, ?, ? , ?, ?)",
                command.getCommandName(),
                command.getUsage(),
                command.getDescription(),
                command.getExtra(),
                String.valueOf(command.getCategory()),
                Arrays.toString(command.getAliases()));
    }

    //Mute role stuff--------------------------------------------------
    public long getRoleId(long guildId, RoleType type) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getRoleId = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ?");
            getRoleId.setLong(1, guildId);
            ResultSet rs = getRoleId.executeQuery();
            long roleId = -1;
            if (rs.next()) roleId = rs.getLong("roleId");
            rs.close();
            getRoleId.close();
            return roleId;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean setRole(long guildId, long roleId, RoleType type) {
        if (roleId == -1L) {
            removeRole(guildId, type);
        } else {
            executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_roles (guildId, roleId) VALUES (?, ?) ON DUPLICATE KEY UPDATE roleId= ?", guildId, roleId, roleId);
        }
        return true;
    }

    public void removeRole(long guildId, RoleType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_roles WHERE guildId= ?", guildId);
    }

    //Filter stuff-----------------------------------------
    public void addFilter(long guildId, String mode, String content) {
        executeUpdate("INSERT INTO filters (guildId, mode, content) VALUES (?, ?, ?)", guildId, mode, content);
    }

    public void removeFilter(long guildId, String mode, String content) {
        executeUpdate("DELETE FROM filters WHERE guildId= ? AND mode= ? AND content= ?", guildId, mode, content);
    }

    public List<String> getFilters(long guildId, String mode) {
        List<String> filters = new ArrayList<>();
        try (Connection con = ds.getConnection()) {
            PreparedStatement addFilter = con.prepareStatement("SELECT * FROM filters WHERE guildId= ? AND mode= ?");
            addFilter.setLong(1, guildId);
            addFilter.setString(2, mode);
            ResultSet rs = addFilter.executeQuery();
            while (rs.next()) {
                filters.add(rs.getString("content"));
            }
            rs.close();
            addFilter.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return filters;
    }

    //Message stuff ---------------------------------------------------------
    public String getMessage(long guildId, MessageType type) {
        try (Connection con = ds.getConnection()) {
            PreparedStatement getLogChannel = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_messages WHERE guildId= ?");
            getLogChannel.setLong(1, guildId);
            ResultSet rs = getLogChannel.executeQuery();
            String s = "";
            while (rs.next()) s = rs.getString("content");
            rs.close();
            getLogChannel.close();
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return "";
        }
    }

    public boolean setMessage(long guildId, String content, MessageType type) {
        executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_messages (guildId, content) VALUES (?, ?) ON DUPLICATE KEY UPDATE content= ?",
                guildId, content, content);
        return true;
    }

    public void removeMessage(long guildId, MessageType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_messages WHERE guildId= ?", guildId);
    }

    public JSONObject getVotesObject(long userId) {
        JSONObject toReturn = new JSONObject().put("streak", 0);
        try (Connection con = ds.getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM votes WHERE userId= ?");
            statement.setLong(1, userId);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                toReturn.put("votes", rs.getLong("votes"));
                toReturn.remove("streak");
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

    public TLongObjectMap<TLongList> getNotificationsMap(NotificationType nextvote) {
        //userId -> mensen waarvan notificatie moet krijgen -> aan of uit
        TLongObjectMap<TLongList> mapje = new TLongObjectHashMap<TLongList>();
        try (Connection con = ds.getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM " + nextvote.toString().toLowerCase() + "_notifications");
            ResultSet rs = statement.executeQuery();
            TLongList row = new TLongArrayList();
            while (rs.next()) {
                if (!row.contains(rs.getLong("userId")))
                    row.add(rs.getLong("userId"));
            }
            for (long s : row.toArray()) {
                rs.beforeFirst();
                TLongList lijst = new TLongArrayList();
                while (rs.next()) {
                    if (rs.getLong("userId") == s) {
                        lijst.add(rs.getLong("targetId"));
                    }
                }
                mapje.put(s, lijst);
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return mapje;
    }

    public void putNotification(long userId, long targetId, NotificationType type) {
        executeUpdate("INSERT INTO " + type.toString().toLowerCase() + "_notifications (userId, targetId) VALUES (?, ?)",
                userId, targetId);
    }

    public void removeNotification(long userId, long targetId, NotificationType type) {
        executeUpdate("DELETE FROM " + type.toString().toLowerCase() + "_notifications WHERE userId= ? AND targetId= ?",
                userId, targetId);
    }

    public TLongList getVoteList() {
        TLongList list = new TLongArrayList();
        try (Connection con = ds.getConnection()) {
            long yesterdayandminute = System.currentTimeMillis() - 43_260_000L;
            long yesterday = System.currentTimeMillis() - 43_200_000L;
            PreparedStatement getVoteMap = con.prepareStatement("SELECT * FROM votes WHERE lastTime BETWEEN ? AND ?");
            getVoteMap.setLong(1, yesterdayandminute);
            getVoteMap.setLong(2, yesterday);
            ResultSet rs = getVoteMap.executeQuery();
            while (rs.next()) {
                list.add(rs.getLong("userId"));
            }
            rs.close();
            getVoteMap.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void updateVoteStreak() {
        executeUpdate("UPDATE votes SET streak=? WHERE lastTime<?", 0, System.currentTimeMillis() - 172_800_000);
    }

    public void addUnverifiedUser(long guildId, long userId) {
        executeUpdate("INSERT INTO unverified_users (guildId, userId) VALUES (?, ?)", guildId, userId);
    }

    public void removeUnverifiedUser(long guildId, long userId) {
        executeUpdate("DELETE FROM unverified_users WHERE guildId= ? AND userId= ?", guildId, userId);
    }

    public void setVerificationCode(long guildId, String code) {
        executeUpdate("INSERT INTO verification_codes (guildId, code) VALUES (?, ?) ON DUPLICATE KEY UPDATE code= ?",
                guildId, code, code);
    }

    public void removeVerificationCode(long guildId) {
        executeUpdate("DELETE FROM verification_codes WHERE guildId= ?", guildId);
    }

    public void setVerificationThreshold(long guildId, int threshold) {
        executeUpdate("INSERT INTO verification_thresholds (guildId, threshold) VALUES (?, ?) " +
                        "ON DUPLICATE KEY UPDATE threshold= ?",
                guildId, threshold, threshold);
    }

    public void removeVerificationThreshold(long guildId) {
        executeUpdate("DELETE FROM verification_thresholds WHERE guildId= ?", guildId);
    }

    public TLongObjectMap<TIntList> getDisabledCommandsMap() {
        TLongObjectMap<TIntList> toReturn = new TLongObjectHashMap<>();
        try (Connection con = ds.getConnection()) {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM disabled_commands");
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                if (toReturn.containsKey(rs.getLong("guildId"))) {
                    TIntList buffertje = toReturn.get(rs.getLong("guildId"));
                    buffertje.add(rs.getInt("command"));
                    toReturn.put(rs.getLong("guildId"), buffertje);
                } else {
                    TIntList temp = new TIntArrayList();
                    temp.add(rs.getInt("command"));
                    toReturn.put(rs.getLong("guildId"), temp);
                }
            }
            rs.close();
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    public void removeDisabledCommands(long guildId, TIntList buffer) {
        TIntList toRemove = new TIntArrayList();
        if (DisableCommand.disabledGuildCommands.containsKey(guildId))
            toRemove.addAll(DisableCommand.disabledGuildCommands.get(guildId));
        toRemove.removeAll(buffer);
        try (Connection con = ds.getConnection()) {
            PreparedStatement statement = con.prepareStatement("DELETE FROM disabled_commands WHERE guildId= ? AND command= ?");
            statement.setLong(1, guildId);
            for (int i : toRemove.toArray()) {
                statement.setInt(2, i);
                statement.executeUpdate();
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addDisabledCommands(long guildId, TIntList buffer) {
        TIntList toAdd = new TIntArrayList();
        toAdd.addAll(buffer);
        if (DisableCommand.disabledGuildCommands.containsKey(guildId))
            toAdd.removeAll(DisableCommand.disabledGuildCommands.get(guildId));
        try (Connection con = ds.getConnection()) {
            PreparedStatement statement = con.prepareStatement("INSERT INTO disabled_commands (guildId, command) VALUES (?, ?)");
            statement.setLong(1, guildId);
            for (int i : toAdd.toArray()) {
                statement.setInt(2, i);
                statement.executeUpdate();
            }
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public TLongList getUnverifiedMembers(long guildId) {
        TLongList members = new TLongArrayList();
        try (Connection con = ds.getConnection()) {
            PreparedStatement getUnverifiedMembers = con.prepareStatement("SELECT * FROM unverified_users WHERE guildId= ?");
            getUnverifiedMembers.setLong(1, guildId);
            ResultSet rs = getUnverifiedMembers.executeQuery();
            while (rs.next()) {
                members.add(rs.getLong("userId"));
            }
            rs.close();
            getUnverifiedMembers.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public int getGuildVerificationThreshold(long guildId) {
        int threshold = 0;
        try (Connection con = ds.getConnection()) {
            PreparedStatement getVerificationThreshold = con.prepareStatement("SELECT * FROM verification_thresholds WHERE guildId= ?");
            getVerificationThreshold.setLong(1, guildId);
            ResultSet rs = getVerificationThreshold.executeQuery();
            if (rs.next())
                threshold = rs.getInt("threshold");
            rs.close();
            getVerificationThreshold.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return threshold;
    }

    public String getGuildVerificationCode(long guildId) {
        String code = "";
        try (Connection con = ds.getConnection()) {
            PreparedStatement getVerificationThreshold = con.prepareStatement("SELECT * FROM verification_codes WHERE guildId= ?");
            getVerificationThreshold.setLong(1, guildId);
            ResultSet rs = getVerificationThreshold.executeQuery();
            if (rs.next())
                code = rs.getString("code");
            rs.close();
            getVerificationThreshold.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return code;
    }

    public TLongList getNotifications(long userId, NotificationType type) {
        TLongList lijstje = new TLongArrayList();
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement("SELECT * FROM " + type.toString().toLowerCase() + "_notifications WHERE userId= ?")) {
                statement.setLong(1, userId);
                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        lijstje.add(rs.getLong("targetId"));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return lijstje;
    }

    public void doUnbans(JDA jda) {
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement("SELECT * FROM active_bans WHERE endTime < ?")) {
                statement.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = statement.executeQuery()) {
                    List<JSONObject> blubjes = new ArrayList<>();
                    while (rs.next()) {
                        blubjes.add(new JSONObject()
                                .put("victimId", rs.getLong("victimId"))
                                .put("guildId", rs.getLong("guildId"))
                        );
                    }
                    rs.close();
                    for (JSONObject blubObj : blubjes) {
                        jda.asBot().getShardManager().retrieveUserById(blubObj.getLong("victimId")).queue(user -> {
                            Guild guild = jda.asBot().getShardManager().getGuildById(blubObj.getLong("guildId"));
                            if (guild != null && user != null)
                                Melijn.mySQL.unban(user, guild, jda.getSelfUser(), "Ban expired");
                        });
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void doUnmutes(JDA jda) {
        try (Connection con = ds.getConnection()) {
            try (PreparedStatement statement = con.prepareStatement("SELECT * FROM active_mutes WHERE endTime < ?")) {
                statement.setLong(1, System.currentTimeMillis());
                try (ResultSet rs = statement.executeQuery()) {
                    List<JSONObject> blubjes = new ArrayList<>();
                    while (rs.next()) {
                        blubjes.add(new JSONObject()
                                .put("victimId", rs.getLong("victimId"))
                                .put("guildId", rs.getLong("guildId"))
                        );
                    }
                    rs.close();
                    for (JSONObject blubObj : blubjes) {
                        jda.asBot().getShardManager().retrieveUserById(blubObj.getLong("victimId")).queue(user -> {
                            Guild guild = jda.asBot().getShardManager().getGuildById(blubObj.getLong("guildId"));
                            if (guild != null)
                                Melijn.mySQL.unmute(guild, user, jda.getSelfUser(), "Mute expired");
                        });
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateUsage(int commandId, long currentTimeMillis) {
        if (currentHour != LocalTime.now().getHour()) {
            currentHour = LocalTime.now().getHour();
            currentTime = currentTimeMillis;
        }
        executeUpdate("INSERT INTO command_usage (commandId, time, usageCount) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE usageCount= usageCount + 1 ",
                commandId, currentTime, 1);
    }

    public LinkedHashMap<Integer, Long> getTopUsage(long[] period, int limit) {
        long smallest = period[0] < period[1] ? period[0] : period[1];
        long biggest = period[0] < period[1] ? period[1] : period[0];
        TMap<Integer, Long> commandUsages = new THashMap<>();
        try (PreparedStatement getUsageWithinPeriod = ds.getConnection().prepareStatement("SELECT * FROM command_usage WHERE time < ? AND time > ?")) {
            getUsageWithinPeriod.setLong(1, biggest);
            getUsageWithinPeriod.setLong(2, smallest);
            ResultSet rs = getUsageWithinPeriod.executeQuery();
            while (rs.next()) {
                commandUsages.put(rs.getInt("commandId"), (commandUsages.containsKey(rs.getInt("commandId")) ? commandUsages.get(rs.getInt("commandId")) : 0) + rs.getLong("usageCount"));
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return findGreatest(commandUsages, limit);
    }

    public long getUsage(long[] period, int commandId) {
        long smallest = period[0] < period[1] ? period[0] : period[1];
        long biggest = period[0] < period[1] ? period[1] : period[0];
        long usage = 0;
        try (PreparedStatement getUsageWithinPeriod = ds.getConnection().prepareStatement("SELECT * FROM command_usage WHERE commandId = ? AND time < ? AND time > ?")) {
            getUsageWithinPeriod.setInt(1, commandId);
            getUsageWithinPeriod.setLong(2, biggest);
            getUsageWithinPeriod.setLong(3, smallest);
            try (ResultSet rs = getUsageWithinPeriod.executeQuery()) {
                while (rs.next()) {
                    usage += rs.getLong("usageCount");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usage;
    }

    public TIntLongMap getUsages(long[] period, TIntList commandIds) {
        long smallest = period[0] < period[1] ? period[0] : period[1];
        long biggest = period[0] < period[1] ? period[1] : period[0];
        TIntLongMap sortedCommandUsages = new TIntLongHashMap();
        TIntLongMap commandUsages = new TIntLongHashMap();
        try (PreparedStatement getUsageWithinPeriod = ds.getConnection().prepareStatement("SELECT * FROM command_usage WHERE time < ? AND time > ?")) {
            getUsageWithinPeriod.setLong(1, biggest);
            getUsageWithinPeriod.setLong(2, smallest);
            try (ResultSet rs = getUsageWithinPeriod.executeQuery()) {
                while (rs.next()) {
                    if (commandIds.contains(rs.getInt("commandId")))
                        commandUsages.put(rs.getInt("commandId"),
                                (commandUsages.containsKey(rs.getInt("commandId")) ?
                                        commandUsages.get(rs.getInt("commandId")) :
                                        0
                                ) + rs.getLong("usageCount"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < commandUsages.size(); i++) {
            var ref = new Object() {
                Map.Entry<Integer, Long> maxEntry = null;
            };
            commandUsages.forEachEntry((key, value) -> {
                if (ref.maxEntry == null || value > ref.maxEntry.getValue()) {
                    ref.maxEntry = new MapEntry(key, value);
                }
                return false;
            });
            if (ref.maxEntry != null) {
                commandUsages.remove(ref.maxEntry.getKey());
                sortedCommandUsages.put(ref.maxEntry.getKey(), ref.maxEntry.getValue());
            }
        }
        return sortedCommandUsages;
    }

    private static <K, V extends Comparable<? super V>> LinkedHashMap<K, V> findGreatest(TMap<K, V> map, int n) {
        Comparator<? super Map.Entry<K, V>> comparator = (Comparator<Map.Entry<K, V>>) (e0, e1) -> {
            V v0 = e0.getValue();
            V v1 = e1.getValue();
            return v0.compareTo(v1);
        };
        PriorityQueue<Map.Entry<K, V>> highest = new PriorityQueue<>(n, comparator);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            highest.offer(entry);
            while (highest.size() > n) {
                highest.poll();
            }
        }

        List<Map.Entry<K, V>> result = new ArrayList<>();
        while (highest.size() > 0) {
            result.add(highest.poll());
        }

        LinkedHashMap<K, V> coolMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : result) {
            coolMap.put(entry.getKey(), entry.getValue());
        }

        Comparator<? super Map.Entry<K, V>> comparator2 = (Comparator<Map.Entry<K, V>>) (e0, e1) -> {
            V v0 = e0.getValue();
            V v1 = e1.getValue();
            return v1.compareTo(v0);
        };
        PriorityQueue<Map.Entry<K, V>> highest2 = new PriorityQueue<>(n, comparator2);
        for (Map.Entry<K, V> entry : coolMap.entrySet()) {
            highest2.offer(entry);
            while (highest2.size() > n) {
                highest2.poll();
            }
        }

        List<Map.Entry<K, V>> result2 = new ArrayList<>();
        while (highest2.size() > 0) {
            result2.add(highest2.poll());
        }

        LinkedHashMap<K, V> coolMap2 = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : result2) {
            coolMap2.put(entry.getKey(), entry.getValue());
        }
        return coolMap2;
    }
}
