package com.axolotl.livessystem.managers;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TabManager {

    private final LivesSystem plugin;

    // Scoreboard team name prefix — must be ≤16 chars
    private static final String TEAM_PREFIX = "livessys_";

    public TabManager(LivesSystem plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        updateAll();
    }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("tab-display", true)) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    /**
     * Uses a scoreboard Team suffix to show lives next to the player's name
     * in the TAB list WITHOUT replacing the player list name.
     * This preserves the vanilla health/ping display completely.
     */
    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("tab-display", true)) {
            clearPlayer(player);
            return;
        }

        LivesManager lm   = plugin.getLivesManager();
        int lives         = lm.getLives(player.getUniqueId());
        boolean eliminated = lm.isEliminated(player.getUniqueId());

        String color;
        if (eliminated) {
            color = plugin.getConfig().getString("tab-colors.dead", "&8");
        } else if (lives >= 4) {
            color = plugin.getConfig().getString("tab-colors.high", "&a");
        } else if (lives >= 2) {
            color = plugin.getConfig().getString("tab-colors.medium", "&e");
        } else {
            color = plugin.getConfig().getString("tab-colors.low", "&c");
        }

        String format    = plugin.getConfig().getString("tab-format", "&c❤ %lives% lives");
        String livesText = eliminated ? "ELIMINATED" : String.valueOf(lives);
        String suffix    = colorize(" " + color + format.replace("%lives%", livesText));

        // Apply via scoreboard team suffix so the vanilla list name is untouched
        applyTeamSuffix(player, suffix);

        // Reset player list name to default (undoes any previous setPlayerListName calls)
        player.setPlayerListName(null);
    }

    public void clearPlayer(Player player) {
        player.setPlayerListName(null);
        removeFromTeams(player);
    }

    // ─── Scoreboard Team Handling ─────────────────────────────────────────────

    private void applyTeamSuffix(Player player, String suffix) {
        // Use the main scoreboard so it's visible to all players
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remove player from any existing livessystem teams first
        removeFromTeams(player);

        // Team name is based on suffix so players with same lives share a team
        // Truncate to 16 chars (Bukkit team name limit)
        String teamName = (TEAM_PREFIX + Math.abs(suffix.hashCode())).substring(0,
                Math.min(TEAM_PREFIX.length() + 8, 16));

        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        // Suffix can be up to 64 chars in 1.13+
        team.setSuffix(suffix);
        team.addEntry(player.getName());
    }

    private void removeFromTeams(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : board.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.removeEntry(player.getName());
            }
        }
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
