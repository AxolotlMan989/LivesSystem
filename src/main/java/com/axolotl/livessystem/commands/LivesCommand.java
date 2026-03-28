package com.axolotl.livessystem.commands;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LivesCommand implements CommandExecutor {

    private final LivesSystem plugin;

    public LivesCommand(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LivesManager lm = plugin.getLivesManager();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(lm.colorize("&cUsage: /lives <player>"));
                return true;
            }
            int lives = lm.getLives(player.getUniqueId());
            boolean elim = lm.isEliminated(player.getUniqueId());
            if (elim) {
                player.sendMessage(lm.colorize("&cYou are &4ELIMINATED&c."));
            } else {
                player.sendMessage(lm.colorize("&aYou have &e" + lives + " &alives remaining."));
            }
        } else {
            @SuppressWarnings("deprecation")
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
            if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
                sender.sendMessage(lm.colorize(plugin.getConfig().getString("messages.player-not-found",
                        "&cPlayer not found.")));
                return true;
            }
            int lives = lm.getLives(target.getUniqueId());
            boolean elim = lm.isEliminated(target.getUniqueId());
            if (elim) {
                sender.sendMessage(lm.colorize("&e" + target.getName() + " &cis &4ELIMINATED&c."));
            } else {
                sender.sendMessage(lm.colorize("&e" + target.getName() + " &ahas &e" + lives + " &alives remaining."));
            }
        }
        return true;
    }
}
