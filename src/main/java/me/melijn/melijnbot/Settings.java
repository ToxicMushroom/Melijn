package me.melijn.melijnbot;

import me.melijn.melijnbot.enums.Environment;

public class Settings {

    public String prefix;
    public long id;
    public String name;
    public String version;
    public long[] developerIds;
    public Environment environment;
    public int shardCount;
    public int restPort;
    public int embedColor;
    public long exceptionChannel;

    public Spotify spotify;
    public Lavalink lavalink;
    public Tokens tokens;
    public Database database;
    public String[] unLoggedThreads;

    public static class Spotify {
        public String clientId;
        public String password;
    }

    public static class Lavalink {
        public Node[] http_nodes;
        public Node[] verified_nodes;
        public boolean enabled;

        public static class Node {
            public String host;
            public String password;
        }
    }

    public static class Tokens {
        public String discord;
        public String topDotGG;
        public String weebSh;
        public String melijnRest;
        public String botsOnDiscordXYZ;
        public String botlistSpace;
        public String discordBotListCom;
        public String discordBotsGG;
        public String botsForDiscordCom;
        public String discordBoats;
        public String randomCatApi;
        public String kSoftApi;
    }

    public static class Database {
        public String database;
        public String password;
        public String user;
        public String host;
        public int port;
    }
}
