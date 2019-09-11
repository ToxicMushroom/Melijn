package me.melijn.jda;

import com.neovisionaries.ws.client.WebSocketFactory;
import lavalink.client.io.jda.JdaLavalink;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandClientBuilder;
import me.melijn.jda.db.MySQL;
import me.melijn.jda.db.Variables;
import me.melijn.jda.events.AddReaction;
import me.melijn.jda.events.Channels;
import me.melijn.jda.events.Chat;
import me.melijn.jda.events.JoinLeave;
import me.melijn.jda.rest.Application;
import me.melijn.jda.utils.*;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.jooby.Jooby;
import org.reflections.Reflections;

import javax.security.auth.login.LoginException;
import java.net.URI;
import java.util.Base64;
import java.util.EnumSet;
import java.util.Set;

public class Melijn {

    private final Config config;
    private final WebUtils webUtils;
    private final ImageUtils imageUtils;
    private final TaskManager taskManager;
    private final MySQL mySQL;
    private final Helpers helpers;
    private final MessageHelper messageHelper;
    private final Variables variables;
    private final Private _private; //private is a preserved keyword :/
    private final Lava lava;
    public static long OWNERID;
    public static String PREFIX;
    private final ShardManager shardManager;

    private Melijn() throws LoginException {
        config = new Config();
        mySQL = new MySQL(
            this,
            config.getValue("ipaddress"),
            config.getValue("username"),
            config.getValue("password"),
            config.getValue("database")
        );
        variables = new Variables(this);

        PREFIX = config.getValue("prefix");
        OWNERID = Long.parseLong(config.getValue("ownerid"));

        webUtils = new WebUtils(this);
        imageUtils = new ImageUtils();

        helpers = new Helpers(this);
        lava = new Lava(this);
        messageHelper = new MessageHelper(this);

        _private = new Private();
        taskManager = new TaskManager(messageHelper);
        shardManager = initJDA();
    }

    private ShardManager initJDA() throws LoginException {
        CommandClientBuilder commandClientBuilder = new CommandClientBuilder(this, OWNERID);

        loadCommands(commandClientBuilder);

        JdaLavalink lavalink = new JdaLavalink(
            getIdFromToken(config.getValue("token")),
            Integer.parseInt(config.getValue("shardCount")),
            shardId -> getShardManager().getShardById(shardId)
        );
        lavalink.addNode(URI.create("ws://" + config.getValue("lavalink-host")), config.getValue("lavalink-pwd"));
        lavalink.setAutoReconnect(true);
        lava.init(lavalink);

        RestAction.setPassContext(true);

        taskManager.async(() -> Jooby.run(() -> new Application(this), "application.port=" + config.getValue("restPort")));
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> messageHelper.printException(thread, exception, null, null));

        return new DefaultShardManagerBuilder()
            .setShardsTotal(Integer.parseInt(config.getValue("shardCount")))
            .setToken(config.getValue("token"))
            .setGame(Game.playing(PREFIX + "help | melijn.com"))
            .setAutoReconnect(true)
            .addEventListeners(commandClientBuilder.build(), lavalink,
                new JoinLeave(this),
                new AddReaction(this),
                new Channels(this),
                new Chat(this))
            .setDisabledCacheFlags(EnumSet.of(CacheFlag.GAME))
            .setWebsocketFactory(new WebSocketFactory().setVerifyHostname(false))
            .setHttpClient(new OkHttpClient.Builder().hostnameVerifier(new NoopHostnameVerifier()).build())
            .setCallbackPool(taskManager.getExecutorService())
            .build();
    }

    public static void main(String[] args) {
        try {
            new Melijn();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public ShardManager getShardManager() {
        return shardManager;
    }

    private String getIdFromToken(String token) {
        return new String(
            Base64.getDecoder().decode(
                token.split("\\.")[0]
            )
        );
    }

    private void loadCommands(CommandClientBuilder client) {
        Reflections reflections = new Reflections("me.melijn.jda.commands");

        Set<Class<? extends Command>> commands = reflections.getSubTypesOf(Command.class);
        commands.forEach(
            (command) -> {
                try {
                    Command cmd = command.getDeclaredConstructor().newInstance();
                    client.addCommand(cmd);
                } catch (Exception ignored) {
                }
            }
        );
    }

    public WebUtils getWebUtils() {
        return webUtils;
    }

    public MySQL getMySQL() {
        return mySQL;
    }

    public Helpers getHelpers() {
        return helpers;
    }

    public MessageHelper getMessageHelper() {
        return messageHelper;
    }

    public Config getConfig() {
        return config;
    }

    public Variables getVariables() {
        return variables;
    }

    public Private getPrivate() {
        return _private;
    }

    public ImageUtils getImageUtils() {
        return imageUtils;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public Lava getLava() {
        return lava;
    }
}
