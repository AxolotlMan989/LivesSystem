package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

    private final LivesSystem plugin;

    public ReloadCommand(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LivesManager lm = plugin.getLivesManager();

        if (!sender.hasPermission("livessystem.admin")) {
            sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.no-permission", "&cNo permission.")));
            return true;
        }

        plugin.reload();
        sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.config-reloaded", "&aConfig reloaded!")));
        return true;
    }
}
