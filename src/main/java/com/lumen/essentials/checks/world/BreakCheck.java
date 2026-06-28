package com.lumen.essentials.checks.world;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.Check;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/** Base for block-break detections (fast break, nuker, etc.). */
public abstract class BreakCheck extends Check {

    protected BreakCheck(LumenEssentials plugin, String name) {
        super(plugin, "world", name);
    }

    public abstract void handleBreak(Player player, PlayerData data, BlockBreakEvent event);
}
