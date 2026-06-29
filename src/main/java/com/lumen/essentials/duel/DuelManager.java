package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates the whole dueling feature: requests, arenas, kits, the round/best-of
 * lifecycle, forfeits (via {@code /leave} or disconnect), inventory save/restore, and
 * the persistent duel-win counter. Player state is always restored when a match ends,
 * however it ends.
 */
public final class DuelManager {

    private static final class Request {
        final UUID challenger;
        final String challengerName;
        final DuelSettings settings;
        final long expiresAt;

        Request(UUID challenger, String challengerName, DuelSettings settings, long expiresAt) {
            this.challenger = challenger;
            this.challengerName = challengerName;
            this.settings = settings;
            this.expiresAt = expiresAt;
        }
    }

    private final LumenEssentials plugin;
    private final KitFactory kitFactory;
    private final Map<UUID, DuelMatch> byPlayer = new ConcurrentHashMap<>();
    private final Map<String, DuelArena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Request> requests = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> wins = new ConcurrentHashMap<>();

    public DuelManager(LumenEssentials plugin) {
        this.plugin = plugin;
        this.kitFactory = new KitFactory(plugin);
    }

    public void load() {
        arenas.clear();
        wins.clear();
        FileConfiguration data = plugin.configManager().duelsData();
        ConfigurationSection arenaSec = data.getConfigurationSection("arenas");
        if (arenaSec != null) {
            for (String name : arenaSec.getKeys(false)) {
                ConfigurationSection s = arenaSec.getConfigurationSection(name);
                if (s == null) {
                    continue;
                }
                arenas.put(name.toLowerCase(Locale.ROOT), new DuelArena(name.toLowerCase(Locale.ROOT),
                        readLoc(s.getConfigurationSection("spawn1")),
                        readLoc(s.getConfigurationSection("spawn2"))));
            }
        }
        ConfigurationSection winSec = data.getConfigurationSection("wins");
        if (winSec != null) {
            for (String key : winSec.getKeys(false)) {
                try {
                    wins.put(UUID.fromString(key), winSec.getInt(key, 0));
                } catch (IllegalArgumentException ignored) {
                    // skip
                }
            }
        }
    }

    private void saveData() {
        FileConfiguration data = plugin.configManager().duelsData();
        data.set("arenas", null);
        for (DuelArena arena : arenas.values()) {
            writeLoc(data, "arenas." + arena.name() + ".spawn1", arena.spawn1());
            writeLoc(data, "arenas." + arena.name() + ".spawn2", arena.spawn2());
        }
        for (Map.Entry<UUID, Integer> entry : wins.entrySet()) {
            data.set("wins." + entry.getKey(), entry.getValue());
        }
        plugin.configManager().saveDuelsData();
    }

    // --- Duel wins (used by /stats) ---------------------------------------

    public int getWins(UUID uuid) {
        return wins.getOrDefault(uuid, 0);
    }

    private void addWin(UUID uuid) {
        wins.merge(uuid, 1, Integer::sum);
    }

    // --- Requests ----------------------------------------------------------

    public boolean isBusy(UUID uuid) {
        return byPlayer.containsKey(uuid);
    }

    public void sendRequest(Player challenger, Player target, DuelSettings settings) {
        if (challenger.equals(target)) {
            MessageUtil.send(challenger, "&cYou cannot duel yourself.");
            return;
        }
        if (isBusy(challenger.getUniqueId()) || isBusy(target.getUniqueId())) {
            MessageUtil.send(challenger, "&cOne of you is already in a duel.");
            return;
        }
        if (freeArena() == null) {
            MessageUtil.send(challenger, "&cNo duel arena is available. Ask an operator to set one up.");
            return;
        }
        requests.put(target.getUniqueId(), new Request(challenger.getUniqueId(),
                challenger.getName(), settings, System.currentTimeMillis() + 60_000L));
        MessageUtil.send(challenger, "&aDuel request sent to &f" + target.getName()
                + " &7(" + settings.kit() + ", best of " + settings.rounds() + ")");
        MessageUtil.send(target, "&e" + challenger.getName() + " &7challenged you to a duel &8("
                + settings.kit() + ", best of " + settings.rounds() + ")&7. &a/duelaccept");
    }

    public void accept(Player target) {
        Request request = requests.remove(target.getUniqueId());
        if (request == null || request.expiresAt < System.currentTimeMillis()) {
            MessageUtil.send(target, "&cYou have no pending duel requests.");
            return;
        }
        Player challenger = plugin.getServer().getPlayer(request.challenger);
        if (challenger == null) {
            MessageUtil.send(target, "&cThat player is no longer online.");
            return;
        }
        if (isBusy(challenger.getUniqueId()) || isBusy(target.getUniqueId())) {
            MessageUtil.send(target, "&cOne of you is already in a duel.");
            return;
        }
        DuelArena arena = freeArena();
        if (arena == null) {
            MessageUtil.send(target, "&cNo duel arena is available right now.");
            return;
        }
        startMatch(challenger, target, request.settings, arena);
    }

    // --- Match lifecycle ---------------------------------------------------

    private void startMatch(Player p1, Player p2, DuelSettings settings, DuelArena arena) {
        arena.setInUse(true);
        DuelMatch match = new DuelMatch(arena, p1.getUniqueId(), p1.getName(),
                p2.getUniqueId(), p2.getName(), settings);
        match.setSaved(p1.getUniqueId(), PlayerState.capture(p1));
        match.setSaved(p2.getUniqueId(), PlayerState.capture(p2));
        byPlayer.put(p1.getUniqueId(), match);
        byPlayer.put(p2.getUniqueId(), match);
        applyEnvironment(match);
        beginRound(match);
    }

    private void applyEnvironment(DuelMatch match) {
        Location anchor = match.arena().spawn1();
        if (anchor == null || anchor.getWorld() == null) {
            return;
        }
        World world = anchor.getWorld();
        try {
            switch (match.settings().weather()) {
                case RAIN:
                    world.setStorm(true);
                    world.setThundering(false);
                    break;
                case THUNDER:
                    world.setStorm(true);
                    world.setThundering(true);
                    break;
                case CLEAR:
                default:
                    world.setStorm(false);
                    world.setThundering(false);
                    break;
            }
            world.setTime(match.settings().time() == DuelSettings.Time.NIGHT ? 18000L : 1000L);
        } catch (Throwable ignored) {
            // weather/time control unsupported; ignore
        }
    }

    private void beginRound(DuelMatch match) {
        Player p1 = plugin.getServer().getPlayer(match.player1());
        Player p2 = plugin.getServer().getPlayer(match.player2());
        if (p1 == null || p2 == null) {
            forfeit(p1 == null ? match.player1() : match.player2());
            return;
        }
        match.setState(DuelMatch.State.COUNTDOWN);
        prepare(p1, match.arena().spawn1(), match.settings().kit());
        prepare(p2, match.arena().spawn2(), match.settings().kit());

        String header = "&bRound " + match.round();
        DuelUtil.title(p1, header, "&7Get ready...");
        DuelUtil.title(p2, header, "&7Get ready...");

        // 3..2..1..GO countdown.
        schedule(() -> countdown(match, 3), 20L);
        schedule(() -> countdown(match, 2), 40L);
        schedule(() -> countdown(match, 1), 60L);
        schedule(() -> {
            if (byPlayer.containsKey(match.player1())) {
                match.setState(DuelMatch.State.FIGHTING);
                Player a = plugin.getServer().getPlayer(match.player1());
                Player b = plugin.getServer().getPlayer(match.player2());
                if (a != null) {
                    DuelUtil.title(a, "&aGO!", "");
                }
                if (b != null) {
                    DuelUtil.title(b, "&aGO!", "");
                }
            }
        }, 80L);
    }

    private void countdown(DuelMatch match, int n) {
        if (!byPlayer.containsKey(match.player1())) {
            return;
        }
        Player a = plugin.getServer().getPlayer(match.player1());
        Player b = plugin.getServer().getPlayer(match.player2());
        if (a != null) {
            DuelUtil.title(a, "&e" + n, "");
        }
        if (b != null) {
            DuelUtil.title(b, "&e" + n, "");
        }
    }

    private void prepare(Player player, Location spawn, String kit) {
        DuelUtil.clearEffects(player);
        DuelUtil.heal(player);
        if (spawn != null) {
            player.teleport(spawn);
        }
        kitFactory.apply(player, kit);
    }

    /** Handles a potentially-lethal hit during a duel: no real death/drops occur. */
    public void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        DuelMatch match = byPlayer.get(victim.getUniqueId());
        if (match == null) {
            return;
        }
        if (match.state() == DuelMatch.State.COUNTDOWN) {
            event.setCancelled(true); // invulnerable during the countdown
            return;
        }
        if (match.state() != DuelMatch.State.FIGHTING) {
            return;
        }
        double remaining = victim.getHealth() - event.getFinalDamage();
        if (remaining <= 0) {
            event.setCancelled(true);
            DuelUtil.heal(victim);
            endRound(match, match.opponent(victim.getUniqueId()));
        }
    }

    private void endRound(DuelMatch match, UUID roundWinner) {
        match.addScore(roundWinner);
        broadcast(match, "&7Round &f" + match.round() + " &7won by &f" + match.name(roundWinner)
                + " &8(" + match.score(match.player1()) + " - " + match.score(match.player2()) + ")");

        if (match.score(roundWinner) >= match.settings().winsNeeded()) {
            endMatch(match, roundWinner, "&6" + match.name(roundWinner) + " &awon the duel!");
            return;
        }
        match.nextRound();
        schedule(() -> {
            if (byPlayer.containsKey(match.player1())) {
                beginRound(match);
            }
        }, 60L);
    }

    private void endMatch(DuelMatch match, UUID winner, String announcement) {
        match.setState(DuelMatch.State.ENDED);
        restore(match, match.player1());
        restore(match, match.player2());
        addWin(winner);
        byPlayer.remove(match.player1());
        byPlayer.remove(match.player2());
        match.arena().setInUse(false);
        broadcast(match, announcement);
        saveData();
    }

    private void restore(DuelMatch match, UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        PlayerState saved = match.saved(uuid);
        if (player != null && saved != null) {
            saved.restore(player);
        }
    }

    /** Forfeit: the other player wins the whole match. Used by /leave and on quit. */
    public boolean forfeit(UUID uuid) {
        DuelMatch match = byPlayer.get(uuid);
        if (match == null) {
            return false;
        }
        UUID winner = match.opponent(uuid);
        Player loser = plugin.getServer().getPlayer(uuid);
        if (loser != null) {
            MessageUtil.send(loser, "&cYou left the duel.");
        }
        endMatch(match, winner, "&6" + match.name(winner) + " &awon the duel &7(opponent left)");
        return true;
    }

    public void handleQuit(UUID uuid) {
        requests.remove(uuid);
        if (byPlayer.containsKey(uuid)) {
            forfeit(uuid);
        }
    }

    // --- Custom kit --------------------------------------------------------

    public void saveCustomKit(Player player) {
        FileConfiguration data = plugin.configManager().duelsData();
        data.set("customkits." + player.getUniqueId() + ".contents",
                new ArrayList<>(java.util.Arrays.asList(player.getInventory().getContents())));
        data.set("customkits." + player.getUniqueId() + ".armor",
                new ArrayList<>(java.util.Arrays.asList(player.getInventory().getArmorContents())));
        plugin.configManager().saveDuelsData();
        MessageUtil.send(player, "&aSaved your current inventory as your Custom duel kit.");
    }

    public void applyCustomKit(Player player) {
        FileConfiguration data = plugin.configManager().duelsData();
        List<?> contents = data.getList("customkits." + player.getUniqueId() + ".contents");
        List<?> armor = data.getList("customkits." + player.getUniqueId() + ".armor");
        if (contents == null) {
            MessageUtil.send(player, "&eNo custom kit set - using Sword. Save one with &f/duel savekit&e.");
            new KitFactory(plugin).apply(player, "sword");
            return;
        }
        try {
            player.getInventory().setContents(toItemArray(contents, 36));
            if (armor != null) {
                player.getInventory().setArmorContents(toItemArray(armor, 4));
            }
        } catch (Throwable ignored) {
            // ignore malformed kit
        }
    }

    private ItemStack[] toItemArray(List<?> list, int size) {
        ItemStack[] array = new ItemStack[Math.max(size, list.size())];
        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);
            array[i] = o instanceof ItemStack ? (ItemStack) o : null;
        }
        return array;
    }

    // --- Arenas (admin) ----------------------------------------------------

    public boolean createArena(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        if (arenas.containsKey(key)) {
            return false;
        }
        arenas.put(key, new DuelArena(key, null, null));
        saveData();
        return true;
    }

    public boolean setSpawn(String name, int which, Location location) {
        DuelArena arena = arenas.get(name.toLowerCase(Locale.ROOT));
        if (arena == null) {
            return false;
        }
        if (which == 1) {
            arena.setSpawn1(location);
        } else {
            arena.setSpawn2(location);
        }
        saveData();
        return true;
    }

    public boolean removeArena(String name) {
        boolean removed = arenas.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            saveData();
        }
        return removed;
    }

    public List<String> arenaNames() {
        return new ArrayList<>(arenas.keySet());
    }

    private DuelArena freeArena() {
        for (DuelArena arena : arenas.values()) {
            if (arena.isReady() && !arena.isInUse()) {
                return arena;
            }
        }
        return null;
    }

    public void shutdown() {
        // End any active matches cleanly, restoring inventories.
        for (DuelMatch match : new ArrayList<>(byPlayer.values())) {
            if (byPlayer.containsKey(match.player1())) {
                endMatch(match, match.player1(), "&cDuel ended (server reload).");
            }
        }
        saveData();
    }

    // --- Helpers -----------------------------------------------------------

    private void broadcast(DuelMatch match, String message) {
        Player p1 = plugin.getServer().getPlayer(match.player1());
        Player p2 = plugin.getServer().getPlayer(match.player2());
        if (p1 != null) {
            MessageUtil.send(p1, message);
        }
        if (p2 != null) {
            MessageUtil.send(p2, message);
        }
    }

    private void schedule(Runnable runnable, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }

    private Location readLoc(ConfigurationSection s) {
        if (s == null) {
            return null;
        }
        World world = plugin.getServer().getWorld(s.getString("world", "world"));
        if (world == null) {
            return null;
        }
        return new Location(world, s.getDouble("x"), s.getDouble("y"), s.getDouble("z"),
                (float) s.getDouble("yaw"), (float) s.getDouble("pitch"));
    }

    private void writeLoc(FileConfiguration data, String path, Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }
        data.set(path + ".world", loc.getWorld().getName());
        data.set(path + ".x", loc.getX());
        data.set(path + ".y", loc.getY());
        data.set(path + ".z", loc.getZ());
        data.set(path + ".yaw", loc.getYaw());
        data.set(path + ".pitch", loc.getPitch());
    }
}
