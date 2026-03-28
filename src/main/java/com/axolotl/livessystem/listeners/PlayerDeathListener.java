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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // Handle lives loss first
        plugin.getLivesManager().handleDeath(player);

        // Don't drop a token if the feature is disabled
        if (!plugin.getConfig().getBoolean("drop-token-on-death", true)) return;

        // Don't drop a token if the player is already eliminated (0 lives)
        // handleDeath() already decremented, so check if they had at least 1 life
        // before death — meaning they had >0 lives before this death occurred.
        // We do this by checking if they still have >=0 lives after the decrement;
        // if lives == 0 they were just eliminated, they still get a token dropped.
        // But if they were already at 0 before death, skip (shouldn't happen since
        // eliminated players are in spectator, but guard anyway).
        int livesAfter = plugin.getLivesManager().getLives(player.getUniqueId());
        if (livesAfter < 0) return; // safety guard

        // PvP-only mode — check if the killer was a player
        boolean pvpOnly = plugin.getConfig().getBoolean("drop-token-pvp-only", false);
        if (pvpOnly) {
            Entity killer = player.getKiller();
            if (!(killer instanceof Player)) return;
        }

        // Drop a Life Token at the player's death location
        ItemStack token = plugin.getItemManager().createLifeToken();
        player.getWorld().dropItemNaturally(player.getLocation(), token);
    }
}
