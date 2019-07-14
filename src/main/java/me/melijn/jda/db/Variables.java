package me.melijn.jda.db;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.common.collect.Sets;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import me.melijn.jda.Melijn;
import me.melijn.jda.blub.*;
import org.discordbots.api.client.DiscordBotListAPI;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static me.melijn.jda.blub.ChannelType.SELF_ROLE;

public class Variables {

    public final Map<Long, List<Integer>> disabledGuildCommands;
    public final LoadingCache<Long, Boolean> serverHasCC;
    public final Map<Long, Map<Long, Long>> possibleDeletes;
    public final Map<Long, Long> messageUser;

    public final CooldownManager cooldownManager;
    public final LoadingCache<Long, Map<Integer, Integer>> cooldowns;

    public final LoadingCache<Long, Map<Long, String>> selfRoles;

    public final LoadingCache<Long, Integer> embedColorCache;

    public final int embedColor = -16711720;
    public final List<Long> blockedUserIds = new ArrayList<>();
    public final List<Long> blockedGuildIds = new ArrayList<>();

    public final LoadingCache<Long, Long> selfRolesChannels;
    public final LoadingCache<Long, Boolean> streamerModeCache;
    public final LoadingCache<Long, String> verificationCodeCache;
    public final LoadingCache<Long, Long> verificationChannelsCache;
    public final LoadingCache<Long, Long> unverifiedRoleCache;
    public final LoadingCache<Long, Integer> verificationThresholdCache;
    public final LoadingCache<Long, VerificationType> verificationTypes;
    public final LoadingCache<Long, Map<Long, Long>> unVerifiedGuildMembersCache;
    public final Map<Long, String> filteredMessageDeleteCause = new HashMap<>();
    public final Map<Long, Long> purgedMessageDeleter = new HashMap<>();
    public final Set<Long> botDeletedMessages = new HashSet<>();
    public final Set<String> unLoggedThreads = new HashSet<>();
    public final Set<Long> looped = new HashSet<>();
    public final Set<Long> loopedQueues = new HashSet<>();
    public final Map<Long, Long> toLeaveTimeMap = new HashMap<>(); // guild, insert time
    public final Set<String> providers = Sets.newHashSet("yt", "sc", "link", "youtube", "soundcloud");
    public final Map<Long, Long> usersFormToReply = new HashMap<>();
    public final Map<Long, Map<Integer, AudioTrack>> userChoices = new HashMap<>();

    public final Map<Long, List<AudioTrack>> userRequestedSongs = new HashMap<>();
    public final Map<Long, Long> userMessageToAnswer = new HashMap<>();
    public final LoadingCache<Long, Long> joinChannelCache;
    public final LoadingCache<Long, Long> leaveChannelCache;
    public final LoadingCache<Long, Long> muteRoleCache;
    public final LoadingCache<Long, String> prefixes;
    public final LoadingCache<Long, List<String>> privatePrefixes;
    public final LoadingCache<Long, String> joinMessages;
    public final LoadingCache<Long, Long> joinRoleCache;
    public final LoadingCache<Long, String> leaveMessages;

    public final LoadingCache<Long, Long> banLogChannelCache;
    public final LoadingCache<Long, Long> muteLogChannelCache;
    public final LoadingCache<Long, Long> kickLogChannelCache;
    public final LoadingCache<Long, Long> warnLogChannelCache;
    public final LoadingCache<Long, Long> musicLogChannelCache;
    public final LoadingCache<Long, Long> sdmLogChannelCache;
    public final LoadingCache<Long, Long> emLogChannelCache;
    public final LoadingCache<Long, Long> odmLogChannelCache;
    public final LoadingCache<Long, Long> pmLogChannelCache;
    public final LoadingCache<Long, Long> fmLogChannelCache;
    public final LoadingCache<Long, Long> reactionLogChannelCache;
    public final LoadingCache<Long, Long> attachmentLogChannelCache;

    public final LoadingCache<Long, Long> antiRaidThresholdChache;

    public final LoadingCache<Long, Long> musicChannelCache;
    public DiscordBotListAPI dblAPI = null;

    public long startTime = 0;
    public double queryAmount = 0.0;
    public double timerAmount = 0.0;

    public final String devineDBLToken;
    public final String dblDotComToken;
    public final String blDotSpaceToken;
    public final String odDotXYZToken;
    public final String dDotBDotGGToken;
    public final String bfdDotComToken;
    public final String dDotBToken;
    public final String dbDotGToken;


    public Variables(Melijn melijn) {
        devineDBLToken = melijn.getConfig().getValue("devineDBLToken");
        dblDotComToken = melijn.getConfig().getValue("dblDotComToken");
        blDotSpaceToken = melijn.getConfig().getValue("blDotSpaceToken");
        odDotXYZToken = melijn.getConfig().getValue("odDotXYZToken");
        dDotBDotGGToken = melijn.getConfig().getValue("dDotBDotGGToken");
        bfdDotComToken = melijn.getConfig().getValue("bfdDotComToken");
        dDotBToken = melijn.getConfig().getValue("dDotBToken");
        dbDotGToken = melijn.getConfig().getValue("dbDotGToken");

        disabledGuildCommands = melijn.getMySQL().getDisabledCommandsMap();

        blockedGuildIds.addAll(melijn.getMySQL().getBlockedIds("guild"));
        blockedUserIds.addAll(melijn.getMySQL().getBlockedIds("user"));

        int frequentSize = 150;
        int frequentDecayMinutes = 4;
        serverHasCC = Caffeine.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getCustomCommands(key).length() > 0);
        possibleDeletes = new HashMap<>();
        messageUser = new HashMap<>();


        cooldowns = Caffeine.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getCooldowns(key));
        cooldownManager = new CooldownManager(this);

        selfRoles = Caffeine.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getSelfRoles(key));

        selfRolesChannels = Caffeine.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, SELF_ROLE));

        int normalSize = 20;
        int normalDecayMinutes = 2;
        embedColorCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getEmbedColor(key));

        streamerModeCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getStreamerMode(key));

        verificationCodeCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getGuildVerificationCode(key));

        verificationChannelsCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.VERIFICATION));

        unverifiedRoleCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getRoleId(key, RoleType.UNVERIFIED));

        verificationThresholdCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getGuildVerificationThreshold(key));

        verificationTypes = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getVerificationType(key));

        unVerifiedGuildMembersCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getUnverifiedMembers(key));

        joinChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.JOIN));

        leaveChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.LEAVE));


        muteRoleCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getRoleId(key, RoleType.MUTE));

        prefixes = Caffeine.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getPrefix(key));

        privatePrefixes = Caffeine.newBuilder()
                .maximumSize(frequentSize)
                .expireAfterAccess(frequentDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getPrivatePrefixes(key));

        joinMessages = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getMessage(key, MessageType.JOIN));

        joinRoleCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getRoleId(key, RoleType.JOIN));

        leaveMessages = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getMessage(key, MessageType.LEAVE));


        banLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.BAN_LOG));

        muteLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.MUTE_LOG));

        kickLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.KICK_LOG));

        warnLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.WARN_LOG));


        musicLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.MUSIC_LOG));

        sdmLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.SDM_LOG));

        emLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.EM_LOG));

        odmLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.ODM_LOG));

        pmLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.PM_LOG));

        fmLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.FM_LOG));

        reactionLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.REACTION_LOG));

        attachmentLogChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalDecayMinutes, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.ATTACHMENT_LOG));

        musicChannelCache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalSize, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getChannelId(key, ChannelType.MUSIC));

        antiRaidThresholdChache = Caffeine.newBuilder()
                .maximumSize(normalSize)
                .expireAfterAccess(normalSize, TimeUnit.MINUTES)
                .build(key -> melijn.getMySQL().getAntiRaidThreshold(key));

        unLoggedThreads.addAll(melijn.getConfig().getSet("unLoggedThreads"));

    }
}
