package com.axolotl.livessystem.gui;

import com.axolotl.livessystem.LivesSystem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe editor GUI — a 54-slot chest that mimics a crafting table.
 *
 * Layout:
 *   Col:   0      1      2      3      4      5      6      7      8
 * Row 0: [fill] [fill] [fill] [fill] [fill] [fill] [fill] [fill] [fill]
 * Row 1: [fill] [S1]   [S2]   [S3]   [fill] [fill] [OUT]  [fill] [fill]
 * Row 2: [fill] [S4]   [S5]   [S6]   [fill] [ARR]  [fill] [fill] [fill]
 * Row 3: [fill] [S7]   [S8]   [S9]   [fill] [fill] [fill] [fill] [fill]
 * Row 4: [fill] [fill] [fill] [fill] [fill] [fill] [fill] [fill] [fill]
 * Row 5: [SAVE] [fill] [fill] [fill] [MODE] [fill] [fill] [fill] [CLOSE]
 *
 * The 9 crafting grid slots (S1-S9) map to the standard 3x3 grid.
 * OUT is a display-only slot showing the resulting named item.
 * MODE toggles between Shaped and Shapeless.
 * SAVE writes the recipe to config and re-registers it.
 */
public class RecipeEditorGUI {

    private final LivesSystem plugin;

    // The 9 crafting input slots in inventory index terms
    public static final int[] GRID_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
    public static final int SLOT_OUTPUT  = 24;
    public static final int SLOT_ARROW   = 23;
    public static final int SLOT_MODE    = 40;
    public static final int SLOT_SAVE    = 36;
    public static final int SLOT_CLOSE   = 44;

    // Metadata keys stored on the opener to track editor state
    public static final String META_ITEM_TYPE = "livessystem_editor_type";   // "revivebook" or "lifetoken"
    public static final String META_SHAPED    = "livessystem_editor_shaped"; // 1 = shaped, 0 = shapeless

    public RecipeEditorGUI(LivesSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String itemType) {
        boolean shaped = plugin.getConfig().getBoolean(
                configPath(itemType) + ".recipe.shaped", true);

        String label = itemType.equals("revivebook") ? "Revive Book" : "Life Token";
        String title = colorize("&8Edit Recipe: &6" + label);
        Inventory inv = Bukkit.createInventory(null, 54, title);

        // Fill background
        ItemStack filler = buildFiller();
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Load existing grid from config
        loadGridFromConfig(inv, itemType);

        // Arrow indicator
        inv.setItem(SLOT_ARROW, buildDisplay(Material.ARROW, "&7▶", null));

        // Output preview
        inv.setItem(SLOT_OUTPUT, itemType.equals("revivebook")
                ? plugin.getItemManager().createReviveBook()
                : plugin.getItemManager().createLifeToken());

        // Mode toggle
        inv.setItem(SLOT_MODE, buildModeButton(shaped));

        // Save button
        inv.setItem(SLOT_SAVE, buildButton(Material.LIME_CONCRETE, "&a&lSAVE RECIPE",
                "&7Click to save and activate this recipe."));

        // Close button
        inv.setItem(SLOT_CLOSE, buildButton(Material.RED_CONCRETE, "&c&lCLOSE",
                "&7Discard changes and close."));

        // Store state in player metadata
        player.setMetadata(META_ITEM_TYPE,
                new org.bukkit.metadata.FixedMetadataValue(plugin, itemType));
        player.setMetadata(META_SHAPED,
                new org.bukkit.metadata.FixedMetadataValue(plugin, shaped ? 1 : 0));

        player.openInventory(inv);
    }

    // ─── Grid ─────────────────────────────────────────────────────────────────

    /**
     * Loads the saved recipe grid from config into the GUI slots.
     */
    private void loadGridFromConfig(Inventory inv, String itemType) {
        String base = configPath(itemType) + ".recipe.grid";
        for (int i = 0; i < 9; i++) {
            String matName = plugin.getConfig().getString(base + "." + i, "AIR");
            Material mat;
            try { mat = Material.valueOf(matName.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.AIR; }
            if (mat != Material.AIR) {
                inv.setItem(GRID_SLOTS[i], new ItemStack(mat));
            }
        }
    }

    /**
     * Reads the current GUI grid contents and saves them to config.
     */
    public void saveGridToConfig(Inventory inv, String itemType, boolean shaped) {
        String base = configPath(itemType) + ".recipe";
        plugin.getConfig().set(base + ".shaped", shaped);
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inv.getItem(GRID_SLOTS[i]);
            String matName = (slot == null || slot.getType() == Material.AIR
                    || isUIItem(slot)) ? "AIR" : slot.getType().name();
            plugin.getConfig().set(base + ".grid." + i, matName);
        }
        plugin.saveConfig();
        plugin.getCraftItemListener().reloadRecipes();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public boolean isGridSlot(int slot) {
        for (int s : GRID_SLOTS) if (s == slot) return true;
        return false;
    }

    /** Returns true if this item is a UI button/filler that shouldn't be treated as an ingredient. */
    public boolean isUIItem(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.GRAY_STAINED_GLASS_PANE
                || m == Material.LIME_CONCRETE
                || m == Material.RED_CONCRETE
                || m == Material.CYAN_CONCRETE
                || m == Material.ARROW
                || m == Material.PAPER;
    }

    public String configPath(String itemType) {
        return itemType.equals("revivebook") ? "revive-book" : "life-token";
    }

    public ItemStack buildModeButton(boolean shaped) {
        String modeName = shaped ? "&b&lSHAPED" : "&e&lSHAPELESS";
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Current: " + modeName));
        lore.add("");
        lore.add(colorize("&7Click to toggle."));
        lore.add(colorize(shaped
                ? "&8Shapeless: ingredients in any order"
                : "&8Shaped: ingredient positions matter"));
        return buildButtonWithLore(Material.CYAN_CONCRETE,
                "&b&lRECIPE MODE: " + (shaped ? "SHAPED" : "SHAPELESS"), lore);
    }

    private ItemStack buildFiller() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = g.getItemMeta();
        if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
        return g;
    }

    private ItemStack buildDisplay(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            if (lore != null) {
                List<String> l = new ArrayList<>();
                l.add(colorize(lore));
                meta.setLore(l);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildButton(Material mat, String name, String lore) {
        return buildDisplay(mat, name, lore);
    }

    private ItemStack buildButtonWithLore(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
