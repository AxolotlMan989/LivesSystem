package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ReviveBookCommand implements CommandExecutor {

    private final LivesSystem plugin;

    public ReviveBookCommand(LivesSystem plugin) {
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
            sender.sendMessage(lm.colorize("&cUsage: /revivebook <player>"));
            return true;
        }

        ItemStack book = plugin.getItemManager().createReviveBook();
        target.getInventory().addItem(book);
        sender.sendMessage(lm.colorize("&aGave a &6Revive Book &ato &e" + target.getName() + "&a."));
        if (!target.equals(sender)) {
            target.sendMessage(lm.colorize("&aYou received a &6Revive Book&a."));
        }
        return true;
    }
}
