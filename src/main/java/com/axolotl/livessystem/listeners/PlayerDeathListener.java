package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final LivesSystem plugin;

    public PlayerDeathListener(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        plugin.getLivesManager().handleDeath(event.getEntity());
    }
}
