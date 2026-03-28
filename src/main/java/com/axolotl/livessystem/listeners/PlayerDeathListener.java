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

    /**
     * LOWEST priority — runs before Corps and any other death loot mod,
     * so the token is already in event.getDrops() when Corps collects it.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // ── Token drop ────────────────────────────────────────────────────────
        // Do this BEFORE handleDeath() so we can read the pre-death lives count.
        if (plugin.getConfig().getBoolean("drop-token-on-death", true)) {
            dropTokenIfEligible(event, player);
        }

        // ── Lives deduction ───────────────────────────────────────────────────
        plugin.getLivesManager().handleDeath(player);
    }

    private void dropTokenIfEligible(PlayerDeathEvent event, Player player) {
        // Don't drop if already eliminated (spectator players shouldn't die,
        // but guard against edge cases)
        if (plugin.getLivesManager().isEliminated(player.getUniqueId())) return;

        // Don't drop if player has bypass permission
        if (player.hasPermission("livessystem.bypass")) return;

        // PvP-only check
        if (plugin.getConfig().getBoolean("drop-token-pvp-only", false)) {
            Entity killer = player.getKiller();
            if (!(killer instanceof Player)) return;
        }

        // Add to the event drops list — Corps reads this list to populate
        // the corpse, so the token ends up inside the corpse inventory.
        // If Corps is not installed it falls to the ground normally.
        ItemStack token = plugin.getItemManager().createLifeToken();
        event.getDrops().add(token);
    }
}
