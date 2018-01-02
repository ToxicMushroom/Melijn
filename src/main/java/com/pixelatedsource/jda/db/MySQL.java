package com.pixelatedsource.jda.db;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;

import java.sql.*;
import java.util.List;

public class MySQL {

    private String ip;
    private String pass;
    private String user;
    private String dbname;
    public Connection con;

    public MySQL(String ip, String user, String pass, String dbname) {
        this.ip = ip;
        this.user = user;
        this.pass = pass;
        this.dbname = dbname;
        connect();
    }

    public MySQL() {
        if (con == null) connect();
    }

    private void connect() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://" + ip + ":3306/" + dbname + "?autoReconnect=true", user, pass);
            System.out.println("[MySQL] has connected");
        update("CREATE TABLE IF NOT EXISTS perms(guildName varchar(64), guildId varchar(128), roleName varchar(64), roleId varchar(128), permission varchar(256))");
        } catch (SQLException e) {
            System.out.println((char)27 + "[31m" + "did not connect");
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            if (this.con != null) {
                this.con.close();
                System.out.println("[MySQL] has disconnected");
            }
        } catch (SQLException e) {
            System.out.println("[MySQL] did not disconnect proparily error:" + e.getMessage());
        }
    }

    public void update(String qry) {
        try {
            Statement st = this.con.createStatement();
            st.executeUpdate(qry);
            st.close();
        } catch (SQLException e) {
            connect();
            e.printStackTrace();
        }
    }

    public ResultSet query(String qry) {
        ResultSet rs = null;
        try {
            Statement st = this.con.createStatement();
            rs = st.executeQuery(qry);
        } catch (SQLException e) {
            connect();
            e.printStackTrace();
        }
        return rs;
    }

    public void addPermission(Guild guild, Role role, String permission) {
        String id = role == null ? "all" : role.getId();
        String name = id.equals("all") ? "everyone" : role.getName();
        try {
            PreparedStatement adding = con.prepareStatement("INSERT INTO perms (guildName, guildId, roleName, roleId, permission) VALUES (?, ?, ?, ?, ?)");
            adding.setString(1, guild.getName());
            adding.setString(2, guild.getId());
            adding.setString(3, name);
            adding.setString(4, id);
            adding.setString(5, permission);
            adding.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void removePermission(Guild guild, Role role, String permission) {
        String id = role == null ? "all" : role.getId();
        try {
            PreparedStatement removing = con.prepareStatement("DELETE FROM perms WHERE guildId= '?' AND roleId= '?' AND permission= '?'");
            removing.setString(1, guild.getId());
            removing.setString(2, id);
            removing.setString(3, permission);
            removing.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public boolean hasPermission(Guild guild, Role role, String permission) {
        String id = role == null ? "all" : role.getId();
        try {
            PreparedStatement getting = con.prepareStatement("SELECT * FROM perms WHERE guildId= '?' AND roleId= '?' AND permission= '?'");
            getting.setString(1, guild.getId());
            getting.setString(2, id);
            getting.setString(3, permission);
            ResultSet rs = getting.executeQuery();
            if (rs.next()) return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void clearPermissions(Guild guild, Role role) {
        String id = role == null ? "all" : role.getId();
        try {
            PreparedStatement clearing = con.prepareStatement("DELETE FROM perms WHERE guildId= '?' AND roleId= '?'");
            clearing.setString(1, guild.getId());
            clearing.setString(2, id);
            clearing.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<String> getPermissions(Guild guild, Role role) {
        List<String>
    }
}
