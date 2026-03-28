package com.axolotl.livessystem.managers;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private final LivesSystem plugin;

    public ItemManager(LivesSystem plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        // Nothing to reload — identification is purely name-based now
    }

    // ─── Item Creation ────────────────────────────────────────────────────────

    public ItemStack createReviveBook() {
        String matName = plugin.getConfig().getString("revive-book.material", "BOOK");
        Material material;
        try { material = Material.valueOf(matName.toUpperCase()); }
        catch (IllegalArgumentException e) { material = Material.BOOK; }

        int reviveLives = plugin.getConfig().getInt("revive-lives", 1);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(plugin.getConfig().getString("revive-book.name", "&6&lRevive Book")));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("revive-book.lore")) {
                lore.add(colorize(line.replace("%revive-lives%", String.valueOf(reviveLives))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createLifeToken() {
        String matName = plugin.getConfig().getString("life-token.material", "NETHER_STAR");
        Material material;
        try { material = Material.valueOf(matName.toUpperCase()); }
        catch (IllegalArgumentException e) { material = Material.NETHER_STAR; }

        int tokenLives = plugin.getConfig().getInt("life-token-lives", 1);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(plugin.getConfig().getString("life-token.name", "&b&lLife Token")));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getConfig().getStringList("life-token.lore")) {
                lore.add(colorize(line.replace("%token-lives%", String.valueOf(tokenLives))));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ─── Item Identification (name-based) ─────────────────────────────────────
    // Items are matched by their display name so datapack-crafted items
    // (which can't carry NBT) are recognised the same as /revivebook or /lifetoken items.

    public boolean isReviveBook(ItemStack item) {
        if (!plugin.getConfig().getBoolean("revive-book.enabled", true)) return false;
        if (item == null || !item.hasItemMeta()) return false;
        String displayName = item.getItemMeta().getDisplayName();
        String expected    = colorize(plugin.getConfig().getString("revive-book.name", "&6&lRevive Book"));
        return displayName.equals(expected);
    }

    public boolean isLifeToken(ItemStack item) {
        if (!plugin.getConfig().getBoolean("life-token.enabled", true)) return false;
        if (item == null || !item.hasItemMeta()) return false;
        String displayName = item.getItemMeta().getDisplayName();
        String expected    = colorize(plugin.getConfig().getString("life-token.name", "&b&lLife Token"));
        return displayName.equals(expected);
    }

    public String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
