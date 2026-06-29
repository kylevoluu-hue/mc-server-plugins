package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Matchmaking queue for premade kits. Players queue for a (mode, kit) pair; once enough
 * are waiting they are pulled, split into two teams, shown "Match Found", and handed to
 * {@link DuelManager#startMatch}.
 */
public final class QueueManager {

    private final LumenEssentials plugin;
    private final DuelManager duelManager;
    // key = "MODE:kit" -> ordered list of waiting players
    private final Map<String, List<UUID>> queues = new LinkedHashMap<>();
    private final Map<UUID, String> playerQueue = new java.util.concurrent.ConcurrentHashMap<>();

    public QueueManager(LumenEssentials plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    private String key(DuelMode mode, String kit) {
        return mode.name() + ":" + kit.toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isQueued(UUID uuid) {
        return playerQueue.containsKey(uuid);
    }

    /** Adds a player to a (mode, kit) queue, starting a match once it fills. */
    public synchronized void join(Player player, DuelMode mode, String kit) {
        if (duelManager.isBusy(player.getUniqueId())) {
            MessageUtil.send(player, "&cYou are already in a duel or queue. Use /leave first.");
            return;
        }
        String key = key(mode, kit);
        List<UUID> queue = queues.computeIfAbsent(key, k -> new ArrayList<>());
        queue.add(player.getUniqueId());
        playerQueue.put(player.getUniqueId(), key);
        MessageUtil.send(player, "&aQueued for &f" + mode.label() + " " + kit
                + " &7(" + queue.size() + "/" + mode.total() + ")");

        if (queue.size() >= mode.total()) {
            List<UUID> picked = new ArrayList<>(queue.subList(0, mode.total()));
            for (UUID uuid : picked) {
                queue.remove(uuid);
                playerQueue.remove(uuid);
            }
            launch(mode, kit, picked);
        }
    }

    private void launch(DuelMode mode, String kit, List<UUID> players) {
        List<UUID> teamA = new ArrayList<>(players.subList(0, mode.teamSize()));
        List<UUID> teamB = new ArrayList<>(players.subList(mode.teamSize(), mode.total()));
        for (UUID uuid : players) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                DuelUtil.title(p, "&aMatch Found!", "&7" + mode.label() + " " + kit);
                MessageUtil.send(p, "&aMatch Found! &7" + mode.label() + " " + kit);
            }
        }
        DuelSettings settings = new DuelSettings();
        settings.setKit(kit);
        duelManager.startMatch(mode, settings, teamA, teamB);
    }

    /** Removes a player from any queue. Returns true if they were queued. */
    public boolean leave(UUID uuid) {
        String key = playerQueue.remove(uuid);
        if (key == null) {
            return false;
        }
        List<UUID> queue = queues.get(key);
        if (queue != null) {
            queue.remove(uuid);
        }
        return true;
    }
}
