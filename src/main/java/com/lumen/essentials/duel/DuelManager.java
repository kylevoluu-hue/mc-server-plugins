package com.lumen.essentials.duel;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.utilities.MessageUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates dueling: direct challenges, the matchmaking queue, team match lifecycle
 * (countdown, rounds, elimination), forfeits, the auto-generated arena world, custom
 * kit slots, and the persistent duel-win counter. Player state is always restored when
 * a match ends, however it ends.
 */
public final class DuelManager {

    private static final class Request {
        final UUID challenger;
        final DuelSettings settings;
        final long expiresAt;

        Request(UUID challenger, DuelSettings settings, long expiresAt) {
            this.challenger = challenger;
            this.settings = settings;
            this.expiresAt = expiresAt;
        }
    }

    private final LumenEssentials plugin;
    private final KitFactory kitFactory;
    private final ArenaManager arenaManager;
    private final QueueManager queueManager;
    private final Map<UUID, DuelMatch> byPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, Request> requests = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> wins = new ConcurrentHashMap<>();

    public DuelManager(LumenEssentials plugin) {
        this.plugin = plugin;
        this.kitFactory = new KitFactory(plugin);
        this.arenaManager = new ArenaManager(plugin);
        this.queueManager = new QueueManager(plugin, this);
    }

    public ArenaManager arenaManager() {
        return arenaManager;
    }

    public QueueManager queueManager() {
        return queueManager;
    }

    public void load() {
        wins.clear();
        FileConfiguration data = plugin.configManager().duelsData();
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
        for (Map.Entry<UUID, Integer> entry : wins.entrySet()) {
            data.set("wins." + entry.getKey(), entry.getValue());
        }
        plugin.configManager().saveDuelsData();
    }

    public int getWins(UUID uuid) {
        return wins.getOrDefault(uuid, 0);
    }

    private void addWin(UUID uuid) {
        wins.merge(uuid, 1, Integer::sum);
    }

    public boolean isBusy(UUID uuid) {
        return byPlayer.containsKey(uuid) || queueManager.isQueued(uuid);
    }

    // --- Direct challenges (1v1) -------------------------------------------

    public void sendRequest(Player challenger, Player target, DuelSettings settings) {
        if (challenger.equals(target)) {
            MessageUtil.send(challenger, "&cYou cannot duel yourself.");
            return;
        }
        if (isBusy(challenger.getUniqueId()) || isBusy(target.getUniqueId())) {
            MessageUtil.send(challenger, "&cOne of you is already in a duel or queue.");
            return;
        }
        requests.put(target.getUniqueId(), new Request(challenger.getUniqueId(), settings,
                System.currentTimeMillis() + 60_000L));
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
            MessageUtil.send(target, "&cOne of you is already busy.");
            return;
        }
        startMatch(DuelMode.ONE_V_ONE, request.settings,
                java.util.Collections.singletonList(challenger.getUniqueId()),
                java.util.Collections.singletonList(target.getUniqueId()));
    }

    // --- Match lifecycle ---------------------------------------------------

    /** Starts a match between two pre-formed teams. Used by both queue and challenge. */
    public void startMatch(DuelMode mode, DuelSettings settings, List<UUID> teamA, List<UUID> teamB) {
        Integer tile = arenaManager.allocate();
        if (tile == null) {
            messageAll(teamA, teamB, "&cNo duel arena is available right now, try again.");
            return;
        }
        Map<UUID, String> names = new HashMap<>();
        for (UUID uuid : concat(teamA, teamB)) {
            Player p = plugin.getServer().getPlayer(uuid);
            names.put(uuid, p == null ? "?" : p.getName());
        }
        DuelMatch match = new DuelMatch(mode, settings, tile, teamA, teamB, names);
        for (UUID uuid : match.allPlayers()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                match.setSaved(uuid, PlayerState.capture(p));
            }
            byPlayer.put(uuid, match);
        }
        applyEnvironment(match);
        beginRound(match);
    }

    private void applyEnvironment(DuelMatch match) {
        World world = arenaManager.world();
        if (world == null) {
            return;
        }
        try {
            switch (match.settings().weather()) {
                case RAIN: world.setStorm(true); world.setThundering(false); break;
                case THUNDER: world.setStorm(true); world.setThundering(true); break;
                case CLEAR:
                default: world.setStorm(false); world.setThundering(false); break;
            }
            world.setTime(match.settings().time() == DuelSettings.Time.NIGHT ? 18000L : 1000L);
        } catch (Throwable ignored) {
            // ignore
        }
    }

    private void beginRound(DuelMatch match) {
        if (match.teamA().isEmpty() || match.teamB().isEmpty()) {
            endMatch(match, match.teamA().isEmpty() ? 1 : 0);
            return;
        }
        match.resetAlive();
        match.setState(DuelMatch.State.COUNTDOWN);

        List<Location>[] spawns = arenaManager.spawns(match.arenaTile(), match.mode().teamSize());
        teleportTeam(match, 0, spawns[0]);
        teleportTeam(match, 1, spawns[1]);

        forEachOnline(match, p -> DuelUtil.title(p, "&bRound " + match.round(), "&7Get ready..."));

        schedule(() -> countdown(match, 3), 20L);
        schedule(() -> countdown(match, 2), 40L);
        schedule(() -> countdown(match, 1), 60L);
        schedule(() -> {
            if (active(match)) {
                match.setState(DuelMatch.State.FIGHTING);
                forEachOnline(match, p -> DuelUtil.title(p, "&aGO!", ""));
            }
        }, 80L);
    }

    private void teleportTeam(DuelMatch match, int team, List<Location> spawns) {
        List<UUID> members = match.team(team);
        for (int i = 0; i < members.size(); i++) {
            Player p = plugin.getServer().getPlayer(members.get(i));
            if (p == null) {
                continue;
            }
            Location spawn = spawns.get(Math.min(i, spawns.size() - 1));
            prepare(p, spawn, match.settings().kit());
        }
    }

    private void prepare(Player player, Location spawn, String kit) {
        DuelUtil.clearEffects(player);
        DuelUtil.heal(player);
        try {
            player.setGameMode(GameMode.SURVIVAL);
        } catch (Throwable ignored) {
            // ignore
        }
        if (spawn != null) {
            player.teleport(spawn);
        }
        kitFactory.apply(player, kit);
    }

    private void countdown(DuelMatch match, int n) {
        if (active(match)) {
            forEachOnline(match, p -> DuelUtil.title(p, "&e" + n, ""));
        }
    }

    /** A potentially-lethal hit during a duel: no real death/drops; eliminates instead. */
    public void handleDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player victim = (Player) event.getEntity();
        DuelMatch match = byPlayer.get(victim.getUniqueId());
        if (match == null) {
            return;
        }
        int team = match.teamOf(victim.getUniqueId());
        if (match.state() == DuelMatch.State.COUNTDOWN) {
            event.setCancelled(true);
            return;
        }
        if (match.state() != DuelMatch.State.FIGHTING) {
            return;
        }
        if (!match.alive(team).contains(victim.getUniqueId())) {
            event.setCancelled(true); // already eliminated (spectating)
            return;
        }
        if (victim.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            eliminate(match, victim, team);
        }
    }

    private void eliminate(DuelMatch match, Player victim, int team) {
        DuelUtil.heal(victim);
        match.eliminate(victim.getUniqueId());
        try {
            victim.setGameMode(GameMode.SPECTATOR);
            victim.teleport(arenaManager.center(match.arenaTile()));
        } catch (Throwable ignored) {
            // ignore
        }
        if (match.alive(team).isEmpty()) {
            endRound(match, 1 - team);
        }
    }

    private void endRound(DuelMatch match, int winnerTeam) {
        match.addScore(winnerTeam);
        broadcast(match, "&7Round &f" + match.round() + " &7to &f" + match.teamName(winnerTeam)
                + " &8(" + match.score(0) + " - " + match.score(1) + ")");
        if (match.score(winnerTeam) >= match.settings().winsNeeded()) {
            endMatch(match, winnerTeam);
            return;
        }
        match.nextRound();
        schedule(() -> {
            if (active(match)) {
                beginRound(match);
            }
        }, 60L);
    }

    private void endMatch(DuelMatch match, int winnerTeam) {
        match.setState(DuelMatch.State.ENDED);
        for (UUID uuid : match.allPlayers()) {
            restore(match, uuid);
            byPlayer.remove(uuid);
        }
        for (UUID winner : match.team(winnerTeam)) {
            addWin(winner);
        }
        arenaManager.release(match.arenaTile());
        broadcast(match, "&6" + match.teamName(winnerTeam) + " &awon the duel!");
        saveData();
    }

    private void restore(DuelMatch match, UUID uuid) {
        Player player = plugin.getServer().getPlayer(uuid);
        PlayerState saved = match.saved(uuid);
        if (player != null && saved != null) {
            saved.restore(player);
        }
    }

    /** Forfeit (via /leave or disconnect): removes the player; their team may lose. */
    public boolean forfeit(UUID uuid) {
        if (queueManager.leave(uuid)) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                MessageUtil.send(p, "&cYou left the duel queue.");
            }
            return true;
        }
        DuelMatch match = byPlayer.get(uuid);
        if (match == null) {
            return false;
        }
        int team = match.teamOf(uuid);
        restore(match, uuid);
        byPlayer.remove(uuid);
        match.removePlayer(uuid);
        Player loser = plugin.getServer().getPlayer(uuid);
        if (loser != null) {
            MessageUtil.send(loser, "&cYou left the duel.");
        }
        if (match.team(team).isEmpty()) {
            endMatch(match, 1 - team);
        } else if (match.state() == DuelMatch.State.FIGHTING && match.alive(team).isEmpty()) {
            endRound(match, 1 - team);
        }
        return true;
    }

    public void handleQuit(UUID uuid) {
        requests.remove(uuid);
        forfeit(uuid);
    }

    public void shutdown() {
        for (DuelMatch match : new ArrayList<>(byPlayer.values())) {
            if (active(match)) {
                endMatch(match, 0);
            }
        }
        saveData();
    }

    // --- Custom kit slots (3 per player) -----------------------------------

    public void saveCustomKit(Player player, int slot) {
        slot = Math.max(1, Math.min(3, slot));
        FileConfiguration data = plugin.configManager().duelsData();
        String base = "customkits." + player.getUniqueId() + ".slot" + slot;
        data.set(base + ".contents",
                new ArrayList<>(java.util.Arrays.asList(player.getInventory().getContents())));
        data.set(base + ".armor",
                new ArrayList<>(java.util.Arrays.asList(player.getInventory().getArmorContents())));
        plugin.configManager().saveDuelsData();
        MessageUtil.send(player, "&aSaved your inventory as Custom kit slot &f" + slot + "&a.");
    }

    public boolean hasCustomKit(UUID uuid, int slot) {
        return plugin.configManager().duelsData()
                .getList("customkits." + uuid + ".slot" + slot + ".contents") != null;
    }

    public void applyCustomKit(Player player, int slot) {
        FileConfiguration data = plugin.configManager().duelsData();
        String base = "customkits." + player.getUniqueId() + ".slot" + slot;
        List<?> contents = data.getList(base + ".contents");
        List<?> armor = data.getList(base + ".armor");
        if (contents == null) {
            MessageUtil.send(player, "&eCustom slot " + slot + " is empty - using Sword. Save one with &f/duel savekit " + slot + "&e.");
            kitFactory.apply(player, "Sword");
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

    // --- Helpers -----------------------------------------------------------

    private boolean active(DuelMatch match) {
        for (UUID uuid : match.allPlayers()) {
            if (byPlayer.containsKey(uuid)) {
                return true;
            }
        }
        return false;
    }

    private void broadcast(DuelMatch match, String message) {
        forEachOnline(match, p -> MessageUtil.send(p, message));
    }

    private void forEachOnline(DuelMatch match, java.util.function.Consumer<Player> action) {
        for (UUID uuid : match.allPlayers()) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                action.accept(p);
            }
        }
    }

    private void messageAll(List<UUID> a, List<UUID> b, String message) {
        for (UUID uuid : concat(a, b)) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null) {
                MessageUtil.send(p, message);
            }
        }
    }

    private List<UUID> concat(List<UUID> a, List<UUID> b) {
        List<UUID> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private void schedule(Runnable runnable, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, runnable, delayTicks);
    }
}
