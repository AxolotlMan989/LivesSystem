package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class LifeTokenCommand implements CommandExecutor {

    private final LivesSystem plugin;

    public LifeTokenCommand(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LivesManager lm = plugin.getLivesManager();

        if (!sender.hasPermission("livessystem.admin")) {
            sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.player-not-found", "&cPlayer not found.")));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(lm.colorize("&cUsage: /lifetoken <player>"));
            return true;
        }

        ItemStack token = plugin.getItemManager().createLifeToken();
        target.getInventory().addItem(token);
        sender.sendMessage(lm.colorize("&aGave a &bLife Token &ato &e" + target.getName() + "&a."));
        if (!target.equals(sender)) {
            target.sendMessage(lm.colorize("&aYou received a &bLife Token&a."));
        }
        return true;
    }
}
