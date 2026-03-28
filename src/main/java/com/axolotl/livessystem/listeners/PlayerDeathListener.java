package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerDeathListener implements Listener {

    private final LivesSystem plugin;

    public PlayerDeathListener(LivesSystem plugin) {
        this.plugin = plugin;
    }

    // Run at LOW priority so we add to the drops list BEFORE Corps (or any other
    // death loot mod) reads it at NORMAL/HIGH priority and collects items.
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Handle lives loss
        plugin.getLivesManager().handleDeath(player);

        // Don't drop a token if the feature is disabled
        if (!plugin.getConfig().getBoolean("drop-token-on-death", true)) return;

        // Don't drop if player was already eliminated before this death
        int livesAfter = plugin.getLivesManager().getLives(player.getUniqueId());
        if (livesAfter < 0) return;

        // PvP-only check
        boolean pvpOnly = plugin.getConfig().getBoolean("drop-token-pvp-only", false);
        if (pvpOnly) {
            Entity killer = player.getKiller();
            if (!(killer instanceof Player)) return;
        }

        // Add the token to the event's drop list instead of spawning it in the world.
        // Corps mod (and similar mods) read from this list to populate the corpse,
        // so the token will appear inside the corpse rather than on the ground.
        ItemStack token = plugin.getItemManager().createLifeToken();
        event.getDrops().add(token);
    }
}
