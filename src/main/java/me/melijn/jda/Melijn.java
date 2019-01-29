package me.melijn.jda;

import com.neovisionaries.ws.client.WebSocketFactory;
import lavalink.client.io.jda.JdaLavalink;
import me.melijn.jda.audio.Lava;
import me.melijn.jda.blub.CommandClient;
import me.melijn.jda.blub.CommandClientBuilder;
import me.melijn.jda.commands.DonateCommand;
import me.melijn.jda.commands.HelpCommand;
import me.melijn.jda.commands.InviteCommand;
import me.melijn.jda.commands.VoteCommand;
import me.melijn.jda.commands.developer.EvalCommand;
import me.melijn.jda.commands.developer.ShutdownCommand;
import me.melijn.jda.commands.developer.TestCommand;
import me.melijn.jda.commands.developer.WeebshCommand;
import me.melijn.jda.commands.fun.*;
import me.melijn.jda.commands.management.*;
import me.melijn.jda.commands.music.*;
import me.melijn.jda.commands.util.*;
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
import org.jooby.Jooby;

import javax.security.auth.login.LoginException;
import java.net.URI;
import java.util.Base64;
import java.util.EnumSet;

public class Melijn {

    private final Melijn melijn;
    private final Config config;
    private final WebUtils webUtils;
    private final ImageUtils imageUtils;
    private final TaskManager taskManager;
    private final YTSearch ytSearch;
    private final MySQL mySQL;
    private final Helpers helpers;
    private final MessageHelper messageHelper;
    private final Variables variables;
    private final Private aPrivate;
    private final Lava lava;
    public static long OWNERID;
    public static String PREFIX;
    private ShardManager shardManager;

    private Melijn() {
        melijn = this;
        config = new Config();
        lava = new Lava(this);

        PREFIX = config.getValue("prefix");
        OWNERID = Long.parseLong(config.getValue("ownerid"));

        webUtils = new WebUtils(this);
        imageUtils = new ImageUtils();
        ytSearch = new YTSearch();
        mySQL = new MySQL(
                melijn,
                config.getValue("ipaddress"),
                config.getValue("username"),
                config.getValue("password"),
                config.getValue("database")
        );
        variables = new Variables(this);
        helpers = new Helpers(this);
        messageHelper = new MessageHelper(this);

        aPrivate = new Private(webUtils);
        taskManager = new TaskManager(messageHelper);


        try {
            init();
        } catch (LoginException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new Melijn();
    }

    private void init() throws LoginException {
        CommandClient commandClient = new CommandClientBuilder(melijn, OWNERID)
                .addCommands(
                        new BirdCommand(), //Only add commands at the end of the list for because of commandIndexes
                        new UrbanCommand(),
                        new BlurpleCommand(),
                        new InvertCommand(),
                        new SetVerificationThresholdCommand(),
                        new SetUnverifiedRoleCommand(),
                        new SetVerificationCodeCommand(),
                        new SetVerificationChannelCommand(),
                        new ShardsCommand(),
                        new ClearChannelCommand(),
                        new NyanCatCommand(),
                        new SummonCommand(),
                        new ForwardCommand(),
                        new RewindCommand(),
                        new CryCommand(),
                        new ShrugCommand(),
                        new DabCommand(),
                        new HighfiveCommand(),
                        new WastedCommand(),
                        new LewdCommand(),
                        new PunchCommand(),
                        new ShuffleCommand(),
                        new EvalCommand(),
                        new WeebshCommand(),
                        new SayCommand(),
                        new DiscordMemeCommand(),
                        new LoopQueueCommand(),
                        new SetNotifications(),
                        new VoteCommand(),
                        new InviteCommand(),
                        new SetJoinLeaveChannelCommand(),
                        new SetJoinRoleCommand(),
                        new SetJoinMessageCommand(),
                        new SetLeaveMessageCommand(),
                        new TriggeredCommand(),
                        new SlapCommand(),
                        new PatCommand(),
                        new FilterCommand(),
                        new PotatoCommand(),
                        new PauseCommand(),
                        new SPlayCommand(),
                        new BanCommand(),
                        new HistoryCommand(),
                        new MuteCommand(),
                        new SetMuteRoleCommand(),
                        new TempMuteCommand(),
                        new UnmuteCommand(),
                        new KickCommand(),
                        new AvatarCommand(),
                        new WarnCommand(),
                        new PurgeCommand(),
                        new HelpCommand(),
                        new PingCommand(),
                        new PlayCommand(),
                        new QueueCommand(),
                        new CatCommand(),
                        new SkipCommand(),
                        new ClearCommand(),
                        new StopCommand(),
                        new ResumeCommand(),
                        new VolumeCommand(),
                        new InfoCommand(),
                        new UserInfoCommand(),
                        new LoopCommand(),
                        new TextToEmojiCommand(),
                        new SeekCommand(),
                        new PermCommand(),
                        new NowPlayingCommand(),
                        new RemoveCommand(),
                        new GuildInfoCommand(),
                        new RolesCommand(),
                        new RoleCommand(),
                        new DogCommand(),
                        new SetPrefixCommand(),
                        new SetMusicChannelCommand(),
                        new SetLogChannelCommand(),
                        new TempBanCommand(),
                        new UnbanCommand(),
                        new SetStreamerModeCommand(),
                        new SetStreamUrlCommand(),
                        new VerifyCommand(),
                        new EnableCommand(),
                        new DisableCommand(),
                        new MetricsCommand(),
                        new SettingsCommand(),
                        new DonateCommand(),
                        new SlowModeCommand(),
                        new UnicodeCommand(),
                        new AlpacaCommand(),
                        new StatsCommand(),
                        new KissCommand(),
                        new HugCommand(),
                        new SpookifyCommand(),
                        new SelfRoleCommand(),
                        new SetSelfRoleChannelCommand(),
                        new CustomCommandCommand(),
                        new ShutdownCommand(),
                        new PollCommand(),
                        new DiceCommand(),
                        new TestCommand(),
                        new SetEmbedColorCommand(),
                        new EmotesCommand(),
                        new PrivatePrefixCommand(),
                        new CooldownCommand(),
                        new SetVerificationTypeCommand()
                ).build();

        JdaLavalink lavalink = new JdaLavalink(
                getIdFromToken(config.getValue("token")),
                Integer.valueOf(config.getValue("shardCount")),
                shardId -> getShardManager().getShardById(shardId)
        );
        lavalink.addNode(URI.create("ws://" + config.getValue("lavalink-host")), config.getValue("lavalink-pwd"));
        lavalink.setAutoReconnect(true);
        lava.init(lavalink);

        shardManager = new DefaultShardManagerBuilder()
                .setShardsTotal(Integer.parseInt(config.getValue("shardCount")))
                .setToken(config.getValue("token"))
                .setGame(Game.playing(PREFIX + "help | melijn.com"))
                .setAutoReconnect(true)
                .addEventListeners(commandClient, lavalink,
                        new JoinLeave(melijn),
                        new AddReaction(melijn),
                        new Channels(melijn),
                        new Chat(melijn))
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.GAME))
                .setWebsocketFactory(new WebSocketFactory().setVerifyHostname(false))
                .build();
        RestAction.setPassContext(true);

        taskManager.async(() -> Jooby.run(() -> new Application(this), "application.port=" + config.getValue("restPort")));
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> messageHelper.printException(thread, exception, null, null));
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
        return aPrivate;
    }

    public ImageUtils getImageUtils() {
        return imageUtils;
    }

    public YTSearch getYtSearch() {
        return ytSearch;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public Lava getLava() {
        return lava;
    }
}
