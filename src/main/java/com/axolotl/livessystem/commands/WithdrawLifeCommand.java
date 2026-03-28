package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class WithdrawLifeCommand implements CommandExecutor, TabCompleter {

    private final LivesSystem plugin;

    public WithdrawLifeCommand(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LivesManager lm = plugin.getLivesManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lm.colorize("&cThis command can only be run by a player."));
            return true;
        }
        if (!player.hasPermission("livessystem.withdraw")) {
            player.sendMessage(lm.colorize(plugin.getConfig().getString(
                    "messages.no-permission", "&cYou don't have permission to do that.")));
            return true;
        }

        int minLives = plugin.getConfig().getInt("withdraw-min-lives", 2);
        int current  = lm.getLives(player.getUniqueId());

        int amount = 1;
        if (args.length >= 1) {
            try {
                amount = Integer.parseInt(args[0]);
                if (amount < 1) {
                    player.sendMessage(lm.colorize("&cAmount must be at least 1."));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(lm.colorize("&cPlease enter a number, e.g. /withdrawlife 2"));
                return true;
            }
        }

        if (current - amount < minLives) {
            String msg = lm.colorize(plugin.getConfig().getString(
                    "messages.withdraw-too-few",
                    "&cYou need to keep at least &e%min% &clives. You currently have &e%lives%&c.")
                    .replace("%min%", String.valueOf(minLives))
                    .replace("%lives%", String.valueOf(current)));
            player.sendMessage(msg);
            return true;
        }

        lm.removeLives(player.getUniqueId(), amount);

        ItemStack token = plugin.getItemManager().createLifeToken();
        token.setAmount(amount);
        player.getInventory().addItem(token);

        lm.playSound(player, plugin.getConfig().getString(
                "sounds.life-token", "ENTITY_EXPERIENCE_ORB_PICKUP"));

        int newLives = lm.getLives(player.getUniqueId());
        String msg = lm.colorize(plugin.getConfig().getString(
                "messages.withdraw-success",
                "&aYou withdrew &e%amount% &alife/lives and received &e%amount% &aLife Token(s). You now have &e%lives% &alives.")
                .replace("%amount%", String.valueOf(amount))
                .replace("%lives%", String.valueOf(newLives)));
        player.sendMessage(msg);
        return true;
    }

    /**
     * Suggests how many lives the player can withdraw (1 up to max withdrawable),
     * instead of defaulting to player names.
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1 && sender instanceof Player player) {
            int current  = plugin.getLivesManager().getLives(player.getUniqueId());
            int minLives = plugin.getConfig().getInt("withdraw-min-lives", 2);
            int maxWithdraw = current - minLives;
            for (int i = 1; i <= maxWithdraw; i++) {
                suggestions.add(String.valueOf(i));
            }
        }
        return suggestions;
    }
}
