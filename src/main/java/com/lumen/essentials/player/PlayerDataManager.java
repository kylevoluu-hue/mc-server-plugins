package com.lumen.essentials.player;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of {@link PlayerData}. Data is created on join and removed on
 * quit to keep memory bounded and avoid leaking state for offline players.
 */
public final class PlayerDataManager {

    private final ConcurrentHashMap<UUID, PlayerData> data = new ConcurrentHashMap<>();

    public PlayerData getOrCreate(Player player) {
        return data.computeIfAbsent(player.getUniqueId(),
                id -> new PlayerData(id, player.getName()));
    }

    public PlayerData getIfPresent(UUID uuid) {
        return data.get(uuid);
    }

    public void remove(UUID uuid) {
        data.remove(uuid);
    }

    public void clear() {
        data.clear();
    }

    public Collection<PlayerData> all() {
        return data.values();
    }

    public int size() {
        return data.size();
    }
}
