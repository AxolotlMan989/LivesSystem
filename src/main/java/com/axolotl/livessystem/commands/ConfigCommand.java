package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigCommand implements CommandExecutor {

    private final LivesSystem plugin;

    public ConfigCommand(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cThis command can only be run by a player.");
            return true;
        }
        if (!player.hasPermission("livessystem.admin")) {
            player.sendMessage(plugin.getLivesManager().colorize(
                    plugin.getConfig().getString("messages.no-permission",
                            "&cYou don't have permission to do that.")));
            return true;
        }
        plugin.getConfigGUI().open(player);
        return true;
    }
}
