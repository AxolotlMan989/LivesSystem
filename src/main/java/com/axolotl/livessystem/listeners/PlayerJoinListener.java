package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final LivesSystem plugin;

    public PlayerJoinListener(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Initialize lives if first join
        plugin.getLivesManager().getLives(player.getUniqueId());

        // Delay by 2 ticks (100ms) so the client finishes loading before
        // we touch the scoreboard/TAB — fixes health display disappearing on join
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            // Restore correct game mode if eliminated
            plugin.getLivesManager().updatePlayerState(player.getUniqueId());
            // Update TAB for everyone
            plugin.getTabManager().updateAll();
        }, 2L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getLivesManager().saveData();
        plugin.getTabManager().clearPlayer(event.getPlayer());
        plugin.getTabManager().updateAll();
    }
}
