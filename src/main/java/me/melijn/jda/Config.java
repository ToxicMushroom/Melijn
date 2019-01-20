package me.melijn.jda;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Config {

    private static Config config = new Config();
    private JSONObject configObject;
    private final File configFile = new File("config.json");

    public static Config getConfigInstance() {
        return config;
    }

    private Config() {
        if (!configFile.exists()) {
            create();
            System.out.println("The config file is created. Fill in all the values. If you don't know how to get a value then check out my wiki");
            System.exit(0);
        }

        JSONObject obj = read(configFile);
        if (obj.has("token") &&
                obj.has("prefix") &&
                obj.has("ownerid") && obj.has("username") && obj.has("password") && obj.has("ipaddress") && obj.has("database")) {
            configObject = obj;
        } else {
            create();
            System.err.println("You didn't fill in all the values correct.");
            System.exit(1);
        }
    }

    private void create() {
        try {
            Files.write(Paths.get(configFile.getPath()),
                new JSONObject()
                    .put("dbltoken", "")
                    .put("ownerid", "231459866630291459")
                    .put("prefix", ">")
                    .put("token", "")
                    .put("username", "")
                    .put("password", "")
                    .put("ipaddress", "")
                    .put("database", "")
                    .put("spotify", "")
                    .put("spotifyClientId", "")
                    .put("environment", "testing")
                    .put("lavalink-host", "")
                    .put("lavalink-pwd", "")
                    .put("shardCount", 1)
                    .put("restPort", 8080)
                    .toString(4)
                    .getBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject read(File file) {
        JSONObject obj = null;
        try {
            obj = new JSONObject(new String(Files.readAllBytes(Paths.get(file.getPath()))));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return obj;
    }

    public String getValue(String key) {
        return configObject == null ? null : configObject.get(key).toString();
    }
}
