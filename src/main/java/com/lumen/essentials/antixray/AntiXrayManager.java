package com.lumen.essentials.antixray;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tracks valuable-ore and container discoveries to support anti-xray investigation.
 * It does <em>not</em> punish on its own: by design it produces ore logs, a
 * suspicion score and staff alerts only. The suspicion heuristic rewards finding
 * fully-enclosed ("unexposed") valuables that an honest player could not have seen,
 * while discounting ores exposed to caves/air (which are legitimately visible).
 *
 * <p>Server owners are encouraged to also enable Paper's native engine-level
 * anti-xray (engine-mode obfuscation); this module is the behavioral/forensic layer
 * on top of it. See {@code config.yml -> anti-xray}.
 */
public final class AntiXrayManager {

    private static final String SUSPICION_KEY = "xray";

    private final LumenEssentials plugin;

    private final Deque<OreDiscovery> recentDiscoveries = new ArrayDeque<>();
    private final Map<UUID, Integer> discoveryCounts = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();

    private Set<Material> protectedBlocks = new HashSet<>();
    private Set<String> enabledWorlds = new HashSet<>();
    private boolean enabled;
    private boolean worldWhitelistMode;
    private double unexposedWeight;
    private double exposedWeight;
    private double alertSuspicion;
    private int maxLog;

    public AntiXrayManager(LumenEssentials plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        var cfg = plugin.configManager().config();
        this.enabled = cfg.getBoolean("anti-xray.enabled", true);
        this.protectedBlocks = plugin.versionManager().adapter()
                .resolveMaterials(cfg.getStringList("anti-xray.protected-blocks"));
        this.enabledWorlds = new HashSet<>(cfg.getStringList("anti-xray.worlds"));
        this.worldWhitelistMode = cfg.getBoolean("anti-xray.world-whitelist", false);
        this.unexposedWeight = cfg.getDouble("anti-xray.suspicion.unexposed-weight", 1.0D);
        this.exposedWeight = cfg.getDouble("anti-xray.suspicion.exposed-weight", 0.05D);
        this.alertSuspicion = cfg.getDouble("anti-xray.suspicion.alert-threshold", 12.0D);
        this.maxLog = cfg.getInt("anti-xray.max-log-entries", 200);
    }

    public boolean isEnabledInWorld(String world) {
        if (!enabled) {
            return false;
        }
        if (enabledWorlds.isEmpty()) {
            return true;
        }
        boolean listed = enabledWorlds.contains(world);
        return worldWhitelistMode == listed; // whitelist: only listed; blacklist: all but listed
    }

    /** Called from the world listener whenever a player breaks a block. */
    public void handleBreak(Player player, PlayerData data, Block block) {
        if (block == null || !isEnabledInWorld(block.getWorld().getName())) {
            return;
        }
        Material type = block.getType();
        if (!protectedBlocks.contains(type)) {
            return;
        }

        boolean exposed = isExposed(block);
        OreDiscovery discovery = new OreDiscovery(player.getName(), type,
                block.getWorld().getName(), block.getX(), block.getY(), block.getZ(),
                exposed, System.currentTimeMillis());
        record(player, discovery);

        // Suspicion: unexposed valuables are far more telling than exposed ones.
        double weight = exposed ? exposedWeight : unexposedWeight;
        double score = data.addSuspicion(SUSPICION_KEY, weight);

        plugin.storageManager().logOre(String.format(
                "%s found %s at %s %d,%d,%d exposed=%s suspicion=%.1f",
                player.getName(), type.name(), block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), exposed, score));

        if (score >= alertSuspicion) {
            plugin.alertManager().notifyStaff(String.format(
                    "&eXray suspicion&7: &f%s &7score &c%.1f &7(last: %s unexposed=%s)",
                    player.getName(), score, type.name(), !exposed));
            double xrayFlag = plugin.configManager().config()
                    .getDouble("auto-flag.xray-suspicion-threshold", 20.0D);
            if (xrayFlag > 0 && score >= xrayFlag) {
                plugin.flagManager().autoFlag(player,
                        "Auto: xray suspicion " + String.format("%.1f", score));
            }
            // Soft-reset so we alert again only after further accumulation.
            data.addSuspicion(SUSPICION_KEY, -alertSuspicion / 2.0D);
        }
    }

    /** True if any face of the block is adjacent to air/cave/liquid (legitimately visible). */
    private boolean isExposed(Block block) {
        for (BlockFace face : new BlockFace[]{BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
                BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
            Material rel = block.getRelative(face).getType();
            if (rel == Material.AIR || rel == Material.CAVE_AIR || rel == Material.WATER
                    || rel == Material.LAVA || (rel != null && rel.name().endsWith("_AIR"))) {
                return true;
            }
        }
        return false;
    }

    private void record(Player player, OreDiscovery discovery) {
        synchronized (recentDiscoveries) {
            recentDiscoveries.addLast(discovery);
            while (recentDiscoveries.size() > maxLog) {
                recentDiscoveries.pollFirst();
            }
        }
        names.put(player.getUniqueId(), player.getName());
        discoveryCounts.merge(player.getUniqueId(), 1, Integer::sum);
    }

    /** Recent discoveries (newest last) for {@code /luac orelog}. */
    public List<OreDiscovery> recentDiscoveries() {
        synchronized (recentDiscoveries) {
            return new java.util.ArrayList<>(recentDiscoveries);
        }
    }

    /** Player discovery counts sorted descending for {@code /luac xraytop}. */
    public List<Map.Entry<String, Integer>> topDiscoverers(int limit) {
        Map<String, Integer> byName = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : discoveryCounts.entrySet()) {
            byName.merge(names.getOrDefault(entry.getKey(), entry.getKey().toString()),
                    entry.getValue(), Integer::sum);
        }
        List<Map.Entry<String, Integer>> list = new java.util.ArrayList<>(byName.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return list.size() > limit ? list.subList(0, limit) : list;
    }

    /** Per-player suspicion + recent finds, for {@code /luac inspectmine <player>}. */
    public Map<String, Object> inspect(PlayerData data, UUID uuid) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("suspicion", data == null ? 0.0D : data.suspicion(SUSPICION_KEY));
        out.put("discoveries", discoveryCounts.getOrDefault(uuid, 0));
        return Collections.unmodifiableMap(out);
    }

    public boolean isEnabled() {
        return enabled;
    }
}
