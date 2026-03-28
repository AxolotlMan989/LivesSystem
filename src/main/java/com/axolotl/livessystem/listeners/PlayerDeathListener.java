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

        // Drop token before deducting lives so we can check pre-death state
        if (plugin.getConfig().getBoolean("drop-token-on-death", true)) {
            dropToken(event, player);
        }

        plugin.getLivesManager().handleDeath(player);
    }

    private void dropToken(PlayerDeathEvent event, Player player) {
        if (plugin.getLivesManager().isEliminated(player.getUniqueId())) return;
        if (player.hasPermission("livessystem.bypass")) return;

        if (plugin.getConfig().getBoolean("drop-token-pvp-only", false)) {
            Entity killer = player.getKiller();
            if (!(killer instanceof Player)) return;
        }

        // Drop naturally in the world at the death location
        ItemStack token = plugin.getItemManager().createLifeToken();
        player.getWorld().dropItemNaturally(player.getLocation(), token);
    }
}
