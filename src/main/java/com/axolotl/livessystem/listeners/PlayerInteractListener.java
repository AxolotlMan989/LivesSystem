package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.ItemManager;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractListener implements Listener {

    private final LivesSystem plugin;

    public PlayerInteractListener(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemManager im = plugin.getItemManager();
        LivesManager lm = plugin.getLivesManager();

        if (!event.getAction().name().startsWith("RIGHT_CLICK")) return;

        // ── Revive Book → open GUI ───────────────────────────────────────────
        if (im.isReviveBook(item)) {
            event.setCancelled(true);
            plugin.getReviveGUI().open(player, 0);
            return;
        }

        // ── Life Token → grant lives ─────────────────────────────────────────
        if (im.isLifeToken(item)) {
            event.setCancelled(true);

            int max     = plugin.getConfig().getInt("max-lives", 10);
            int current = lm.getLives(player.getUniqueId());

            if (current >= max) {
                player.sendMessage(lm.colorize(plugin.getConfig().getString(
                        "messages.at-max-lives", "&cYou are already at the maximum number of lives!")));
                return;
            }

            int tokenLives = plugin.getConfig().getInt("life-token-lives", 1);
            lm.addLives(player.getUniqueId(), tokenLives);

            int newLives = lm.getLives(player.getUniqueId());
            String msg = lm.colorize(plugin.getConfig().getString("messages.life-token-used",
                    "&aYou used a Life Token! You now have &e%lives% &alives.")
                    .replace("%lives%", String.valueOf(newLives)));
            player.sendMessage(msg);
            lm.playSound(player, plugin.getConfig().getString("sounds.life-token",
                    "ENTITY_EXPERIENCE_ORB_PICKUP"));

            if (plugin.getConfig().getBoolean("life-token.consume-on-use", true)) {
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);
            }
        }
    }
}
