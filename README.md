# Lumen Essentials

A modular SMP toolkit for **Paper** and **Purpur** combining a low-false-positive
anti-cheat, a forensic anti-xray layer, and safe staff investigation tooling — all
in a single plugin.

> **Scope note:** This repository is one self-contained module of a larger server
> suite. It is intentionally focused: a clean, extensible core with a representative
> set of fully-implemented checks rather than every conceivable detection. The
> architecture is built so additional checks drop in without touching the framework.

---

## Table of contents

- [Features](#features)
- [Version compatibility](#version-compatibility)
- [Installation](#installation)
- [Building](#building)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Architecture](#architecture)
- [Anti-false-positive philosophy](#anti-false-positive-philosophy)
- [Safe use of investigation tools](#safe-use-of-investigation-tools)
- [Developer API](#developer-api)
- [Troubleshooting](#troubleshooting)
- [FAQ](#faq)
- [License](#license)

---

## Features

- **Anti-cheat** with decaying violation scores, confidence weighting and
  configurable alert/punishment thresholds.
  - Movement: Speed, Fly, NoFall, Timer.
  - Combat: Reach, AutoClicker (timing-distribution analysis).
  - World: FastBreak, Nuker, Scaffold.
- **Anti-xray** (forensic layer): valuable-ore discovery logging, an
  exposed-vs-enclosed suspicion model, per-world control, and staff alerts.
  Designed to complement — not replace — Paper's engine-level ore obfuscation.
- **Staff investigation tools**: `spawnstash` and `oresummon` create temporary,
  hidden, randomized test objects to study suspected xray/ESP/stash-finder
  behavior, with automatic cleanup and full audit logging.
- **Violation & punishment system**: data-driven, console-command punishments
  with placeholder substitution; conservative defaults to avoid false bans.
- **Staff alerts & debug/verbose** subscriptions.
- **Player profiling**: per-player violations, suspicion scores and client brand.
- **SMP feature suite**:
  - Manual **flags** (`/flag`, `/flaglist`) with a player-head GUI and reasons.
  - **Stats** GUI (`/stats`) — mob kills, player kills, deaths, playtime.
  - **Warps** (`/warp`, `/sswarp`) — operator-set named destinations.
  - **Random teleport** (`/rtp`) with a lag-safe bounded safe-location search.
  - **Teleport requests** (`/tpa`, `/tpahere`, `/tpaccept`, `/tpauto`, `/tp`) with
    warmup countdowns and cooldowns.
  - **Anti-combat-log** with a neon-blue boss-bar timer that blocks teleporting and
    kills players who log out in combat.
  - **Silk-touch spawners** — harvest mob spawners (mob type preserved) with a Silk
    Touch tool.
  - **Personal settings menu** (`/settings`) — per-player toggles (night vision,
    hostile-mob spawning, hide chat, block teleport requests, instant respawn, and
    client-side preferences).
- **Developer API** plus cancellable Bukkit events.
- **Asynchronous, batched logging** that never blocks the main thread.

---

## Version compatibility

Target platforms: **Paper** and **Purpur**, Minecraft **1.8.9 → latest** (including
the 1.21+/26.x line).

The plugin avoids hardcoded NMS and resolves version differences through a
`VersionAdapter` chosen at startup by `VersionManager`:

| Tier        | Adapter          | Versions          | Notes                                            |
|-------------|------------------|-------------------|--------------------------------------------------|
| Legacy      | `LegacyAdapter`  | 1.8.9 – 1.12.2    | No native anti-xray/ping; reflection fallbacks   |
| Modern      | `ModernAdapter`  | 1.13 – 1.19       | Native ping, gliding, Paper anti-xray            |
| Latest      | `LatestAdapter`  | 1.20+ (incl. 26.x)| All capabilities native                          |

- **Officially tested:** modern Paper (1.20.x) API surface.
- **Best-effort:** 1.13–1.19 and the latest releases (graceful capability checks).
- **Legacy (1.8.9–1.12.2):** core anti-cheat works; some features that depend on
  newer blocks/effects (barrels, shulkers, ancient debris, levitation/slow-falling
  exemptions) degrade gracefully and are skipped where the API is absent. The
  investigation **stash** tooling requires container types available on your
  version; the **oresummon** vein tool requires the configured ore blocks to exist.

Unknown/newer versions fall back to the `LatestAdapter`, so new server releases
keep working without a code change.

---

## Installation

1. Build (see below) or download `LumenEssentials.jar`.
2. Drop it into your server's `plugins/` folder.
3. Start the server once to generate the config files in `plugins/LumenEssentials/`.
4. Edit the configs and run `/luac reload`.

---

## Download / Building

**Prebuilt jar:** a ready-to-use build is committed at
[`dist/LumenEssentials-1.0.0.jar`](dist/LumenEssentials-1.0.0.jar). Download it and
drop it straight into your server's `plugins/` folder — no build step required. It
targets Java 17 bytecode and is compiled against the Bukkit/Paper API the server
provides at runtime.

**Building from source** (optional) requires a JDK 17+ and access to the Paper Maven
repository:

```bash
gradle build
```

The jar is produced at `build/libs/LumenEssentials.jar`. To add a committed Gradle
wrapper for reproducible builds, run `gradle wrapper` once in an environment with
access to `services.gradle.org`, then use `./gradlew build`.

> The build compiles against `io.papermc.paper:paper-api` from
> `https://repo.papermc.io`. In restricted/offline CI environments that repo may be
> unreachable; point the build at an internal mirror or run it where the Paper repo
> is allowlisted.

---

## Commands

Main command `/luac` (alias `/lumen`). All subcommands tab-complete and respect
permissions.

| Command                         | Permission        | Description                              |
|---------------------------------|-------------------|------------------------------------------|
| `/luac help`                    | —                 | List available subcommands               |
| `/luac reload`                  | `luac.admin`      | Hot-reload all config files              |
| `/luac info`                    | `luac.admin`      | Plugin/version/runtime status            |
| `/luac checks`                  | `luac.admin`      | List checks and their state              |
| `/luac violations [player]`     | `luac.admin`      | Recent violations                        |
| `/luac profile <player>`        | `luac.admin`      | Forensic profile of a player             |
| `/luac exempt <player>`         | `luac.exempt`     | Toggle a player's check exemption        |
| `/luac alerts`                  | `luac.alerts`     | Toggle anti-cheat alerts                 |
| `/luac debug`                   | `luac.debug`      | Toggle debug output                      |
| `/luac verbose`                 | `luac.debug`      | Toggle verbose detection output          |
| `/luac orelog [player]`         | `luac.investigate`| Recent valuable-ore discoveries          |
| `/luac inspectmine <player>`    | `luac.investigate`| Mining suspicion summary                 |
| `/luac xraytop`                 | `luac.investigate`| Top valuable-ore discoverers             |
| `/luac spawnstash <type> <size> [target]` | `luac.spawnstash` | Create a hidden test stash      |
| `/luac oresummon <ore> <amount> [target]` | `luac.oresummon`  | Create a hidden test ore vein   |

Investigation presets (every invocation is freshly randomized — different base, vein,
spawners and loot each time):

- `spawnstash <type> <size> [target]` — `type` is `chest`, `barrel`, `shulker`,
  `spawner` (filled mob spawner), `base` (a cluster of loot containers, sometimes a
  spawner), or `random`; `size` is `small`, `regular`, `large`, or `huge` (scales the
  loot and container count).
- `oresummon <ore> <amount> [target]` — `ore` accepts friendly aliases (`diamond`,
  `gold`, `ancient_debris`, `emerald`, `iron`, `copper`, `redstone`, `lapis`, `coal`,
  and crafted blocks like `diamond_block`, `gold_block`, `emerald_block`,
  `iron_block`, `netherite_block`) or `random`; `amount` is the vein size.

`[target]` is optional and is one of `<player>`, `<world> <x> <y> <z>`, or `random`
(defaults to your own location). When confirmation is enabled, append `confirm`.

### SMP feature commands

Standalone player-facing commands (separate from `/luac`):

| Command                         | Permission         | Description                                  |
|---------------------------------|--------------------|----------------------------------------------|
| `/flag <player> <reason>`       | `lumen.flag`       | Flag a player for suspicion (reason required)|
| `/flaglist`                     | `lumen.flag`       | Open the flagged-players GUI (heads)         |
| `/stats [player]`               | `lumen.stats`      | Open the stats GUI                           |
| `/warp <name>`                  | `lumen.warp`       | Warp to a configured location                |
| `/sswarp <name>`                | `lumen.warp.admin` | Set a warp at your location                  |
| `/rtp`                          | `lumen.rtp`        | Random teleport around the survival world    |
| `/rtpworld <world>`             | `lumen.rtp.admin`  | Set the RTP world                            |
| `/rtpamount <radius>`           | `lumen.rtp.admin`  | Set the RTP maximum radius                   |
| `/tpa <player>`                 | `lumen.tpa`        | Request to teleport to a player              |
| `/tpahere <player>`             | `lumen.tpa`        | Request a player to teleport to you          |
| `/tpaccept`                     | `lumen.tpa`        | Accept a pending request                     |
| `/tpauto`                       | `lumen.tpa`        | Toggle auto-accepting requests               |
| `/tp <player>` / `<p1> <p2>` / `<x y z>` | `lumen.tp` | Staff teleport                             |
| `/settings`                     | `lumen.settings`   | Open the personal settings toggle menu       |

Notes:

- **GUIs** use a DonutSMP-style read-only inventory. The flag menu shows one player
  head per flag; hover for the reason/flagger, left-click to teleport to the player,
  right-click to open their stats. The stats menu maps mob kills → zombie head,
  player kills → player head, deaths → skeleton skull, playtime → clock.
- **Warps** are simple named destinations set by operators with `/sswarp <name>`
  (e.g. `spawn`, `duels`, `crates`, `minigames`).
- **`/rtp`** scatters the player around a configurable center point in the survival
  world. The search is bounded (configurable attempts) so it stays lag-friendly.
- **Teleports** show a neon-yellow boss-bar countdown and cancel (neon-red message)
  if you move or take combat damage; per-player cooldowns are configurable.

### Personal settings (`/settings`)

A DonutSMP-style toggle menu (one themed icon per option, click to toggle, state
shown in the item). Settings persist per player in `settings.yml`.

**Enforced server-side:**

- **Night Vision** — applies an infinite night-vision effect (fullbright-style).
- **Hostile Mob Spawning** — cancels natural hostile spawns near you; they still
  spawn if a nearby player allows mobs.
- **Hide Global Chat** — removes you from global chat recipients.
- **Teleport Requests** — blocks incoming `/tpa` / `/tpahere` to you.
- **Instant Respawn** — respawns you immediately on death.

**Stored as client-cooperative preferences** (these are inherently client-side, so
the server persists the flag and exposes it; a companion client/resource layer
applies the visual effect): No Explosion Particles, Fast Crystal, Team Chat, Spoof
Coords, Hurt Cam.

### Anti-combat-log

Dealing or taking PvP damage tags both players for a configurable duration, shown on
a neon-blue boss bar (`Combat : {time}`). Each new hit resets the timer. While tagged,
teleport/warp/spawn commands are blocked. Disconnecting while tagged **kills the
player** (they drop items per server rules and respawn at their spawn on next login)
and can optionally run extra punishment commands. All tunables live under `combat:`
in `features.yml`.

---

## Permissions

| Node               | Default | Grants                                    |
|--------------------|---------|-------------------------------------------|
| `luac.admin`       | op      | Administrative commands                   |
| `luac.alerts`      | op      | Receive anti-cheat alerts                 |
| `luac.debug`       | op      | Debug/verbose output                      |
| `luac.bypass`      | false   | Bypass **all** checks (trusted players)   |
| `luac.exempt`      | op      | Manage exemptions                         |
| `luac.investigate` | op      | Investigation/forensic commands           |
| `luac.spawnstash`  | op      | Create test stashes                       |
| `luac.oresummon`   | op      | Create test ore veins                     |
| `lumen.flag`       | op      | Flag players / open the flag list         |
| `lumen.stats`      | true    | View your own stats                       |
| `lumen.stats.others`| op     | View other players' stats                 |
| `lumen.warp`       | true    | Use warps                                 |
| `lumen.warp.admin` | op      | Set/manage warps                          |
| `lumen.rtp`        | true    | Use random teleport                       |
| `lumen.rtp.admin`  | op      | Configure random teleport                 |
| `lumen.tpa`        | true    | Use teleport requests                     |
| `lumen.tp`         | op      | Staff teleport                            |
| `lumen.silkspawner`| true    | Harvest spawners with Silk Touch          |
| `lumen.settings`   | true    | Open the personal settings menu           |

---

## Configuration

Hot-reloadable files are generated on first run:

- **`config.yml`** — general settings, logging, and the anti-xray module
  (protected blocks, per-world control, suspicion weights).
- **`checks.yml`** — every check with the standard knobs: `enabled`, `buffer`,
  `threshold`, `decay`, `confidence`, `alert-threshold`, `punishment-threshold`,
  `debug`.
- **`messages.yml`** — colorized messages and the alert format (with placeholders).
- **`punishments.yml`** — per-check console-command actions with placeholders
  (`{player} {check} {vl} {ping} {tps} {world} {x} {y} {z}`).
- **`investigation.yml`** — investigation tooling (confirmation, cleanup timer,
  loot value, distances, allowed ore blocks).
- **`features.yml`** — SMP features: combat tag, teleport warmup/cooldowns, RTP
  center/radius, and warp settings.
- **`warps.yml` / `flags.yml`** — runtime data stores (written by the plugin).

Run `/luac reload` to apply changes without a restart.

---

## Architecture

```
LumenEssentials (composition root)
├── ConfigManager        five ConfigFile wrappers, hot reload
├── VersionManager       picks Legacy/Modern/Latest VersionAdapter
├── StorageManager       async, batched file logging
├── PlayerDataManager    per-player PlayerData lifecycle (join→quit)
├── CheckManager         registers checks; exposes typed lists
│   └── Check (abstract)  movement / combat / world subtypes
├── ViolationManager     decay, confidence, thresholds, events
├── AlertManager         alert + debug subscriptions
├── PunishmentManager    data-driven console-command actions
├── AntiXrayManager      ore logging + suspicion + alerts
├── InvestigationManager spawnstash / oresummon presets + cleanup + monitoring
├── CombatTagManager     anti-combat-log boss bar + combat-log kill
├── TeleportManager      tpa/warp/rtp warmup countdown + cooldowns
├── WarpManager          persistent named warps (warps.yml)
├── RtpManager           lag-safe bounded random teleport
├── FlagManager          manual flags (flags.yml) + FlagMenu GUI
├── StatsManager         vanilla statistics + StatsMenu GUI
├── gui.Menu/MenuListener read-only chest GUIs
├── CommandManager       /luac dispatcher + SubCommand registry
├── FeatureCommandHandler /flag /stats /warp /rtp /tpa ... dispatcher
└── LumenAPI             public developer API + Bukkit events
```

Design principles: single composition root (no static singletons), one
responsibility per class, checks decoupled from policy (decay/thresholds/alerts
live centrally), and the open/closed principle for adding checks (register one
class, no framework changes).

---

## Anti-false-positive philosophy

Public SMPs punish trust, not just cheaters. Lumen errs heavily toward **alert
early, punish late**:

- **Decaying violation levels** — isolated detections fade; only sustained or
  repeated behavior accumulates.
- **Confidence weighting** — noisier checks contribute less per detection.
- **Buffers** — most checks require several consecutive suspicious ticks.
- **Environmental leniency** — recent teleports/velocity/knockback, low TPS,
  vehicles, elytra, levitation/slow-falling, liquids, climbables, webs, slime/honey,
  and ping are all accounted for before flagging.
- **Conservative defaults** — punishment thresholds sit far above alert
  thresholds, and the default punishment is a console message, not a ban.
- **ESP/Tracer/BaseFinder/X-Ray** detections are **suspicion + staff alerts only**
  by default — never automatic bans.

---

## Safe use of investigation tools

`spawnstash` and `oresummon` exist for **controlled staff investigations**, not for
gameplay. Guardrails:

- Gated behind `luac.investigate` / `luac.spawnstash` / `luac.oresummon`.
- Every creation, discovery and cleanup is logged to `logs/investigation.log`.
- Objects are placed **underground and away** from the staff member, so they never
  hand normal players an advantage.
- Temporary by default: a configurable cleanup timer restores the original blocks
  exactly; outstanding objects are also restored on plugin disable.
- Optional confirmation prompt (`confirm` token).
- Discovery monitoring alerts staff when a non-staff player reaches a test object,
  reporting how long after creation it took.

Use them on a case-by-case basis, document why, and let cleanup run.

---

## Developer API

Obtain the API:

```java
LumenEssentials lumen = (LumenEssentials) Bukkit.getPluginManager().getPlugin("LumenEssentials");
LumenAPI api = lumen.api();

double speedVL = api.getViolationLevel(player, "speed");
Map<String, Double> all = api.getViolations(player);
Map<String, Double> suspicion = api.getSuspicionScores(player);
String brand = api.getClientBrand(player);
api.setExempt(player.getUniqueId(), true);
```

Listen for and veto detections/punishments via Bukkit events:

```java
@EventHandler
public void onViolation(ViolationEvent event) {
    // Cancel to suppress alert + punishment + logging for this detection.
    if (isTrusted(event.getPlayer())) event.setCancelled(true);
}

@EventHandler
public void onPunish(PunishmentEvent event) {
    // Cancel to keep the violation recorded but skip the punishment commands.
    event.setCancelled(true);
}
```

The API surface (`LumenAPI`) and the two events are the only supported integration
points; everything else is implementation detail.

---

## Troubleshooting

- **No alerts in chat** — run `/luac alerts` (toggles your subscription) and ensure
  you hold `luac.alerts`.
- **A check never fires** — verify it is `enabled` in `checks.yml` and that you do
  not hold `luac.bypass`; lower its `alert-threshold` while testing with
  `/luac verbose`.
- **Too many false positives** — raise `buffer`, lower `confidence`, raise the
  thresholds, and check for chronic low TPS (movement checks self-suppress below
  16 TPS).
- **Punishments not running** — confirm the command exists (bans need a punishment
  plugin like EssentialsX/LiteBans) and that `punishment-threshold` is reachable.
- **Build can't download paper-api** — your network/CI is blocking
  `repo.papermc.io`; use an internal mirror.

---

## FAQ

**Does this ban for X-Ray automatically?** No. X-Ray/ESP/BaseFinder produce
suspicion scores and staff alerts by default. You decide what to do with the
evidence.

**Will it replace Paper's anti-xray?** No — it's the behavioral/forensic layer.
Keep Paper's engine-mode obfuscation enabled for the packet-level protection.

**Is it Folia-compatible?** Not currently (`folia-supported: false`).

**Does it store data to a database?** Runtime state is in-memory; durable records
are appended to the async log files under `plugins/LumenEssentials/logs/`.

---

## License

MIT — see [LICENSE](LICENSE).
