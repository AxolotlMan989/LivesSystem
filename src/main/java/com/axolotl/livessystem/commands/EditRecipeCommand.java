package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditRecipeCommand implements CommandExecutor {

    private final LivesSystem plugin;
    private final String itemType; // "revivebook" or "lifetoken"

    public EditRecipeCommand(LivesSystem plugin, String itemType) {
        this.plugin   = plugin;
        this.itemType = itemType;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LivesManager lm = plugin.getLivesManager();

        if (!(sender instanceof Player player)) {
            sender.sendMessage(lm.colorize("&cThis command can only be run by a player."));
            return true;
        }
        if (!player.hasPermission("livessystem.admin")) {
            player.sendMessage(lm.colorize(plugin.getConfig().getString(
                    "messages.no-permission", "&cYou don't have permission to do that.")));
            return true;
        }

        plugin.getRecipeEditorGUI().open(player, itemType);
        return true;
    }
}
