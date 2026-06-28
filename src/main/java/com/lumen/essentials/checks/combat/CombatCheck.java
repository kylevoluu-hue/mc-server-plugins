package com.lumen.essentials.checks.combat;

import com.lumen.essentials.LumenEssentials;
import com.lumen.essentials.checks.Check;
import com.lumen.essentials.player.PlayerData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** Base for combat detections, invoked when a player attacks an entity. */
public abstract class CombatCheck extends Check {

    protected CombatCheck(LumenEssentials plugin, String name) {
        super(plugin, "combat", name);
    }

    public abstract void handleAttack(Player attacker, PlayerData data, Entity victim,
                                      EntityDamageByEntityEvent event);
}
