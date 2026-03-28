package com.axolotl.livessystem.managers;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class TabManager {

    private final LivesSystem plugin;
    private static final String TEAM_PREFIX = "livessys_";

    public TabManager(LivesSystem plugin) {
        this.plugin = plugin;
    }

    public void reload() { updateAll(); }

    public void updateAll() {
        if (!plugin.getConfig().getBoolean("tab-display", true)) return;
        for (Player player : Bukkit.getOnlinePlayers()) updatePlayer(player);
    }

    public void updatePlayer(Player player) {
        if (!plugin.getConfig().getBoolean("tab-display", true)) {
            clearPlayer(player);
            return;
        }

        LivesManager lm    = plugin.getLivesManager();
        int lives          = lm.getLives(player.getUniqueId());
        boolean eliminated = lm.isEliminated(player.getUniqueId());

        // Clamp to 0-10 range for color lookup; above 10 uses color for 10
        int colorKey = eliminated ? 0 : Math.min(lives, 10);
        String color = plugin.getConfig().getString("tab-colors." + colorKey, "&f");

        String format    = plugin.getConfig().getString("tab-format", "&c❤ %lives% lives");
        String livesText = eliminated ? "ELIMINATED" : String.valueOf(lives);
        String suffix    = colorize(" " + color + format.replace("%lives%", livesText));

        applyTeamSuffix(player, suffix);
        player.setPlayerListName(null);
    }

    public void clearPlayer(Player player) {
        player.setPlayerListName(null);
        removeFromTeams(player);
    }

    private void applyTeamSuffix(Player player, String suffix) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        removeFromTeams(player);

        String teamName = (TEAM_PREFIX + Math.abs(suffix.hashCode()));
        teamName = teamName.substring(0, Math.min(teamName.length(), 16));

        Team team = board.getTeam(teamName);
        if (team == null) team = board.registerNewTeam(teamName);
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
