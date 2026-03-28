package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AddLivesCommand implements CommandExecutor {

    private final LivesSystem plugin;

    public AddLivesCommand(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LivesManager lm = plugin.getLivesManager();

        if (!sender.hasPermission("livessystem.admin")) {
            sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(lm.colorize("&cUsage: /addlives <player> <amount>"));
            return true;
        }

        @SuppressWarnings("deprecation")
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.player-not-found", "&cPlayer not found.")));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(lm.colorize("&cInvalid number."));
            return true;
        }

        lm.addLives(target.getUniqueId(), amount);
        sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.lives-added", "&aAdded &e%amount% &alives to &e%player%&a.")
                .replace("%player%", target.getName())
                .replace("%amount%", String.valueOf(amount))));
        return true;
    }
}
