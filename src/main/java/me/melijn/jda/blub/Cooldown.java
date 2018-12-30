package me.melijn.jda.blub;

import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TLongObjectHashMap;
import me.melijn.jda.commands.management.CooldownCommand;

public class Cooldown {

    private final TLongObjectMap<TLongObjectMap<TIntLongMap>> cooldowns = new TLongObjectHashMap<>();// Guild -> User -> command -> time used

    private void checkOldEntries() {
        long currentTime = System.currentTimeMillis();
        new TLongObjectHashMap<>(cooldowns).forEachEntry((guildId, users) -> users.forEachEntry((userId, commands) -> commands.forEachEntry((commandId, time) -> {
            if (time < currentTime) {
                TIntLongMap freshCommands = cooldowns.get(guildId).get(userId);
                freshCommands.remove(commandId);
                if (freshCommands.isEmpty()) {
                    TLongObjectMap<TIntLongMap> freshUsers = cooldowns.get(guildId);
                    freshUsers.remove(userId);
                    if (freshUsers.isEmpty()) {
                        cooldowns.remove(guildId);
                    } else {
                        cooldowns.put(guildId, freshUsers);
                    }
                } else {
                    TLongObjectMap<TIntLongMap> freshUsers = cooldowns.get(guildId);
                    freshUsers.put(userId, freshCommands);
                    cooldowns.put(guildId, freshUsers);
                }
            }
            return true;
        })));
    }

    public void updateCooldown(long guildId, long userId, int commandId) {
        checkOldEntries();
        if (!CooldownCommand.cooldowns.getUnchecked(guildId).containsKey(commandId)) return;
        TLongObjectMap<TIntLongMap> users = cooldowns.containsKey(guildId) ? cooldowns.get(guildId) : new TLongObjectHashMap<>();
        if (!users.containsKey(userId)) users.put(userId, new TIntLongHashMap());
        TIntLongMap commands = users.get(userId);
        commands.put(commandId, System.currentTimeMillis() + CooldownCommand.cooldowns.getUnchecked(guildId).get(commandId));
        users.put(userId, commands);
        cooldowns.put(guildId, users);
    }

    public boolean isActive(long guildId, long userId, int commandId) {
        long currentTime = System.currentTimeMillis();
        return (cooldowns.containsKey(guildId) &&
                cooldowns.get(guildId).containsKey(userId) &&
                cooldowns.get(guildId).get(userId).containsKey(commandId) &&
                cooldowns.get(guildId).get(userId).get(commandId) > currentTime);
    }

    public long getTimeLeft(long guildId, long userId, int commandId) {
        return (cooldowns.containsKey(guildId) &&
                cooldowns.get(guildId).containsKey(userId) &&
                cooldowns.get(guildId).get(userId).containsKey(commandId)) ?
                cooldowns.get(guildId).get(userId).get(commandId) - System.currentTimeMillis() :
                0;
    }
}
