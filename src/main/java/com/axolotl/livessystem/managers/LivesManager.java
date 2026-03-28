package com.axolotl.livessystem.managers;

import com.axolotl.livessystem.LivesSystem;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LivesManager {

    private final LivesSystem plugin;
    private final File dataFile;
    private FileConfiguration dataConfig;

    // UUID -> lives count (-1 = eliminated/spectator)
    private final Map<UUID, Integer> livesData = new HashMap<>();

    public LivesManager(LivesSystem plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
        loadData();
    }

    public void reload() {
        loadData();
    }

    // ─── Data IO ────────────────────────────────────────────────────────────────

    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        livesData.clear();
        if (dataConfig.contains("players")) {
            for (String key : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int lives = dataConfig.getInt("players." + key + ".lives");
                    livesData.put(uuid, lives);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void saveData() {
        for (Map.Entry<UUID, Integer> entry : livesData.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString() + ".lives", entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    public void resetAllData() {
        livesData.clear();
        dataConfig.set("players", null);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml after reset: " + e.getMessage());
        }
    }

    // ─── Lives API ───────────────────────────────────────────────────────────────

    public int getLives(UUID uuid) {
        if (!livesData.containsKey(uuid)) {
            int starting = plugin.getConfig().getInt("starting-lives", 5);
            livesData.put(uuid, starting);
        }
        return livesData.get(uuid);
    }

    /** Returns a list of all UUIDs with 0 or fewer lives. */
    public java.util.List<UUID> getEliminatedPlayers() {
        return livesData.entrySet().stream()
                .filter(e -> e.getValue() <= 0)
                .map(java.util.Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean isEliminated(UUID uuid) {
        return getLives(uuid) <= 0;
    }

    public void setLives(UUID uuid, int amount) {
        int max = plugin.getConfig().getInt("max-lives", 10);
        int clamped = Math.max(0, Math.min(amount, max));
        livesData.put(uuid, clamped);
        saveData();
        updatePlayerState(uuid);
        plugin.getTabManager().updateAll();
    }

    public void addLives(UUID uuid, int amount) {
        setLives(uuid, getLives(uuid) + amount);
    }

    public void removeLives(UUID uuid, int amount) {
        setLives(uuid, getLives(uuid) - amount);
    }

    /**
     * Called on player death. Reduces lives by 1.
     * If lives reach 0, puts the player into spectator mode.
     */
    public void handleDeath(Player player) {
        if (player.hasPermission("livessystem.bypass")) return;

        UUID uuid = player.getUniqueId();
        int current = getLives(uuid);

        if (current <= 0) return; // Already eliminated

        int newLives = current - 1;
        livesData.put(uuid, newLives);
        saveData();

        FileConfiguration config = plugin.getConfig();

        if (newLives <= 0) {
            // Eliminated
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.setGameMode(GameMode.SPECTATOR);
                    playSound(player, config.getString("sounds.eliminated", "ENTITY_WITHER_SPAWN"));
                    String msg = colorize(config.getString("messages.death-eliminated",
                            "&4You have been eliminated! You are now in spectator mode."));
                    player.sendMessage(msg);
                    if (config.getBoolean("action-bar", true)) {
                        sendActionBar(player, msg);
                    }
                    // Broadcast elimination
                    Bukkit.broadcastMessage(colorize("&c&l" + player.getName() + " &chas been eliminated!"));
                }
            }, 20L); // 1 second after death
        } else {
            // Lost a life
            playSound(player, config.getString("sounds.death", "ENTITY_WITHER_DEATH"));
            String msg = colorize(config.getString("messages.death-lost-life",
                    "&cYou lost a life! You have &e%lives% &clives remaining.")
                    .replace("%lives%", String.valueOf(newLives)));
            player.sendMessage(msg);
            if (config.getBoolean("action-bar", true)) {
                sendActionBar(player, msg);
            }
        }

        plugin.getTabManager().updateAll();
    }

    /**
     * Revives an eliminated player. Sets their lives to the configured revive amount.
     */
    public boolean revivePlayer(UUID targetUuid, Player revivedBy) {
        if (!isEliminated(targetUuid)) return false;

        int reviveLives = plugin.getConfig().getInt("revive-lives", 1);
        livesData.put(targetUuid, reviveLives);
        saveData();

        Player target = Bukkit.getPlayer(targetUuid);
        if (target != null && target.isOnline()) {
            target.setGameMode(GameMode.SURVIVAL);
            playSound(target, plugin.getConfig().getString("sounds.revived", "ENTITY_PLAYER_LEVELUP"));

            String reviverName = revivedBy != null ? revivedBy.getName() : "an admin";
            String msg = colorize(plugin.getConfig().getString("messages.revived",
                    "&aYou have been revived by &e%player%&a! You have &e%lives% &alives.")
                    .replace("%player%", reviverName)
                    .replace("%lives%", String.valueOf(reviveLives)));
            target.sendMessage(msg);
            if (plugin.getConfig().getBoolean("action-bar", true)) {
                sendActionBar(target, msg);
            }
            Bukkit.broadcastMessage(colorize("&a&l" + target.getName() + " &ahas been revived by &e" + reviverName + "&a!"));
        }

        plugin.getTabManager().updateAll();
        return true;
    }

    /**
     * Updates a player's gamemode to match their lives state (called on join etc.)
     */
    public void updatePlayerState(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null || !player.isOnline()) return;
        if (isEliminated(uuid)) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        } else {
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    // ─── Utilities ───────────────────────────────────────────────────────────────

    public String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }

    public void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + soundName);
        }
    }

    public void sendActionBar(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                new TextComponent(colorize(message)));
    }
}
