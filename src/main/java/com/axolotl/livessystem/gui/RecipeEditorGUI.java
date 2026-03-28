package com.axolotl.livessystem.gui;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.listeners.CraftItemListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe editor GUI.
 *
 * Layout (54-slot chest):
 *   Slots 0-53:
 *     Row 0: filler
 *     Row 1: [fill][S1][S2][S3][fill][fill][OUT][fill][fill]
 *     Row 2: [fill][S4][S5][S6][fill][ ▶ ][fill][fill][fill]
 *     Row 3: [fill][S7][S8][S9][fill][fill][fill][fill][fill]
 *     Row 4: filler
 *     Row 5: [SAV][fill][fill][fill][MOD][fill][RBK][LTK][CLO]
 *
 * RBK = insert Revive Book placeholder
 * LTK = insert Life Token placeholder
 */
public class RecipeEditorGUI {

    private final LivesSystem plugin;

    public static final int[] GRID_SLOTS = { 10, 11, 12, 19, 20, 21, 28, 29, 30 };
    public static final int SLOT_OUTPUT  = 24;
    public static final int SLOT_ARROW   = 23;
    public static final int SLOT_MODE    = 40;
    public static final int SLOT_SAVE    = 36;
    public static final int SLOT_CLOSE   = 44;
    public static final int SLOT_INSERT_BOOK  = 42;  // Insert Revive Book placeholder
    public static final int SLOT_INSERT_TOKEN = 43;  // Insert Life Token placeholder

    public static final String META_ITEM_TYPE = "livessystem_editor_type";
    public static final String META_SHAPED    = "livessystem_editor_shaped";

    // NBT-like tag stored on placeholder items so we can identify them
    private static final String PLACEHOLDER_BOOK_NAME  = "§6§l[Revive Book]";
    private static final String PLACEHOLDER_TOKEN_NAME = "§b§l[Life Token]";

    public RecipeEditorGUI(LivesSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, String itemType) {
        boolean shaped = plugin.getConfig().getBoolean(
                configPath(itemType) + ".recipe.shaped", true);

        String label = itemType.equals("revivebook") ? "Revive Book" : "Life Token";
        Inventory inv = Bukkit.createInventory(null, 54,
                colorize("&8Edit Recipe: &6" + label));

        // Fill background
        ItemStack filler = buildFiller();
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Load saved grid
        loadGridFromConfig(inv, itemType);

        inv.setItem(SLOT_ARROW,  buildDisplay(Material.ARROW, "&7▶", null));
        inv.setItem(SLOT_OUTPUT, itemType.equals("revivebook")
                ? plugin.getItemManager().createReviveBook()
                : plugin.getItemManager().createLifeToken());
        inv.setItem(SLOT_MODE,         buildModeButton(shaped));
        inv.setItem(SLOT_SAVE,         buildButton(Material.LIME_CONCRETE,  "&a&lSAVE",  "&7Save and activate recipe"));
        inv.setItem(SLOT_CLOSE,        buildButton(Material.RED_CONCRETE,   "&c&lCLOSE", "&7Discard and close"));
        inv.setItem(SLOT_INSERT_BOOK,  buildButton(Material.ENCHANTED_BOOK, "&6&l[Revive Book]",
                "&7Click to add a Revive Book\n&7slot to the grid"));
        inv.setItem(SLOT_INSERT_TOKEN, buildButton(Material.NETHER_STAR,    "&b&l[Life Token]",
                "&7Click to add a Life Token\n&7slot to the grid"));

        player.setMetadata(META_ITEM_TYPE,
                new org.bukkit.metadata.FixedMetadataValue(plugin, itemType));
        player.setMetadata(META_SHAPED,
                new org.bukkit.metadata.FixedMetadataValue(plugin, shaped ? 1 : 0));

        player.openInventory(inv);
    }

    // ─── Grid persistence ─────────────────────────────────────────────────────

    private void loadGridFromConfig(Inventory inv, String itemType) {
        String base = configPath(itemType) + ".recipe.grid";
        for (int i = 0; i < 9; i++) {
            String raw = plugin.getConfig().getString(base + "." + i, "AIR").toUpperCase();
            ItemStack item = rawToDisplayItem(raw);
            if (item != null) inv.setItem(GRID_SLOTS[i], item);
        }
    }

    public void saveGridToConfig(Inventory inv, String itemType, boolean shaped) {
        String base = configPath(itemType) + ".recipe";
        plugin.getConfig().set(base + ".shaped", shaped);
        for (int i = 0; i < 9; i++) {
            ItemStack slot = inv.getItem(GRID_SLOTS[i]);
            plugin.getConfig().set(base + ".grid." + i, displayItemToRaw(slot));
        }
        plugin.saveConfig();
        plugin.getCraftItemListener().reloadRecipes();
    }

    // ─── Placeholder items ────────────────────────────────────────────────────

    /**
     * Creates the placeholder item shown in the grid to represent a plugin item slot.
     */
    public ItemStack buildReviveBookPlaceholder() {
        return buildNamedItem(Material.ENCHANTED_BOOK, PLACEHOLDER_BOOK_NAME,
                "&7Requires a &6Revive Book");
    }

    public ItemStack buildLifeTokenPlaceholder() {
        return buildNamedItem(Material.NETHER_STAR, PLACEHOLDER_TOKEN_NAME,
                "&7Requires a &bLife Token");
    }

    public boolean isReviveBookPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return PLACEHOLDER_BOOK_NAME.equals(item.getItemMeta().getDisplayName());
    }

    public boolean isLifeTokenPlaceholder(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return PLACEHOLDER_TOKEN_NAME.equals(item.getItemMeta().getDisplayName());
    }

    // ─── Raw <-> display item conversion ─────────────────────────────────────

    /**
     * Converts a config raw string (e.g. "NETHER_STAR", "PLUGIN_LIFE_TOKEN") to
     * the item that should be displayed in the editor grid.
     */
    private ItemStack rawToDisplayItem(String raw) {
        if (raw == null || raw.equals("AIR")) return null;
        if (raw.equals(CraftItemListener.PLUGIN_REVIVE_BOOK)) return buildReviveBookPlaceholder();
        if (raw.equals(CraftItemListener.PLUGIN_LIFE_TOKEN))  return buildLifeTokenPlaceholder();
        try {
            Material mat = Material.valueOf(raw);
            if (mat == Material.AIR) return null;
            return new ItemStack(mat);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Converts a grid slot item back to the raw string saved in config.
     */
    private String displayItemToRaw(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || isUIItem(item)) return "AIR";
        if (isReviveBookPlaceholder(item)) return CraftItemListener.PLUGIN_REVIVE_BOOK;
        if (isLifeTokenPlaceholder(item))  return CraftItemListener.PLUGIN_LIFE_TOKEN;
        return item.getType().name();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    public boolean isGridSlot(int slot) {
        for (int s : GRID_SLOTS) if (s == slot) return true;
        return false;
    }

    public boolean isUIItem(ItemStack item) {
        if (item == null) return false;
        Material m = item.getType();
        return m == Material.GRAY_STAINED_GLASS_PANE
                || m == Material.LIME_CONCRETE
                || m == Material.RED_CONCRETE
                || m == Material.CYAN_CONCRETE;
        // Note: ARROW, ENCHANTED_BOOK, NETHER_STAR are NOT blocked here
        // because they may be valid recipe ingredients
    }

    public String configPath(String itemType) {
        return itemType.equals("revivebook") ? "revive-book" : "life-token";
    }

    public ItemStack buildModeButton(boolean shaped) {
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Current: " + (shaped ? "&bSHAPED" : "&eSHAPELESS")));
        lore.add(colorize(""));
        lore.add(colorize("&7Click to toggle."));
        return buildButtonWithLore(Material.CYAN_CONCRETE,
                "&b&lMODE: " + (shaped ? "SHAPED" : "SHAPELESS"), lore);
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

    private ItemStack buildNamedItem(Material mat, String name, String lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name); // already colorized
            List<String> l = new ArrayList<>();
            l.add(colorize(lore));
            meta.setLore(l);
            item.setItemMeta(meta);
        }
        return item;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
