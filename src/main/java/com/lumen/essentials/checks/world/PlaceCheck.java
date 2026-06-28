package com.lumen.essentials.checks.world;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.Check;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

/** Base for block-place detections (fast place, scaffold, etc.). */
public abstract class PlaceCheck extends Check {

    protected PlaceCheck(LumenEssentials plugin, String name) {
        super(plugin, "world", name);
    }

    public abstract void handlePlace(Player player, PlayerData data, BlockPlaceEvent event);
}
