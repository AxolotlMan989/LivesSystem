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
 * Centralized config GUI opened by /lsconfig.
 *
 * Layout (54 slots):
 * Row 0: [ REVIVE BOOK TOGGLE ][ LIFE TOKEN TOGGLE ][ DROP TOKEN TOGGLE ][ PVP ONLY TOGGLE ][ TAB TOGGLE ][ . ][ . ][ . ][ . ]
 * Row 1: [ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ]
 * Row 2: [ EDIT REVIVE BOOK RECIPE ][ EDIT LIFE TOKEN RECIPE ][ . ][ . ][ . ][ . ][ . ][ . ][ . ]
 * Row 3: filler
 * Row 4: filler
 * Row 5: [ . ][ . ][ . ][ . ][ . ][ . ][ . ][ . ][ CLOSE ]
 *
 * Toggleable options show GREEN (on) or RED (off).
 * Non-toggleable items (item names, sounds etc.) show a note that
 * they must be configured in config.yml.
 */
public class ConfigGUI {

    private final LivesSystem plugin;

    // Slot assignments
    public static final int SLOT_REVIVE_BOOK_ENABLED  = 0;
    public static final int SLOT_LIFE_TOKEN_ENABLED   = 1;
    public static final int SLOT_DROP_TOKEN           = 2;
    public static final int SLOT_PVP_ONLY             = 3;
    public static final int SLOT_TAB_DISPLAY          = 4;
    public static final int SLOT_EDIT_REVIVE_RECIPE   = 18;
    public static final int SLOT_EDIT_TOKEN_RECIPE    = 19;
    public static final int SLOT_FILE_ONLY_INFO       = 26;
    public static final int SLOT_CLOSE                = 53;

    public static final String GUI_TITLE = "§8LivesSystem Config";

    public ConfigGUI(LivesSystem plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);

        // Fill background
        ItemStack filler = buildFiller();
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Toggles
        inv.setItem(SLOT_REVIVE_BOOK_ENABLED, buildToggle(
                Material.ENCHANTED_BOOK,
                "Revive Book",
                "revive-book.enabled",
                "Allows players to use the Revive Book item.",
                true));

        inv.setItem(SLOT_LIFE_TOKEN_ENABLED, buildToggle(
                Material.NETHER_STAR,
                "Life Token",
                "life-token.enabled",
                "Allows players to use the Life Token item.",
                true));

        inv.setItem(SLOT_DROP_TOKEN, buildToggle(
                Material.CHEST,
                "Drop Token on Death",
                "drop-token-on-death",
                "Players drop a Life Token when they die.",
                true));

        inv.setItem(SLOT_PVP_ONLY, buildToggle(
                Material.IRON_SWORD,
                "PvP Only Token Drop",
                "drop-token-pvp-only",
                "Token only drops on player kills.",
                false));

        inv.setItem(SLOT_TAB_DISPLAY, buildToggle(
                Material.NAME_TAG,
                "TAB Lives Display",
                "tab-display",
                "Show lives count in the TAB list.",
                true));

        // Recipe editors
        inv.setItem(SLOT_EDIT_REVIVE_RECIPE, buildButton(
                Material.ENCHANTED_BOOK,
                "&6&lEdit Revive Book Recipe",
                List.of("&7Click to open the recipe editor",
                        "&7for the Revive Book.")));

        inv.setItem(SLOT_EDIT_TOKEN_RECIPE, buildButton(
                Material.NETHER_STAR,
                "&b&lEdit Life Token Recipe",
                List.of("&7Click to open the recipe editor",
                        "&7for the Life Token.")));

        // File-only config info
        inv.setItem(SLOT_FILE_ONLY_INFO, buildButton(
                Material.BOOK,
                "&e&lFile-Only Settings",
                List.of("&7The following must be configured",
                        "&7in &fconfig.yml&7:",
                        "",
                        "&8• &7Starting / max / revive lives",
                        "&8• &7Life token lives amount",
                        "&8• &7Withdraw minimum lives",
                        "&8• &7Item names, lore & materials",
                        "&8• &7TAB format & colors per life",
                        "&8• &7Sounds",
                        "&8• &7Messages",
                        "",
                        "&7Use &f/livesreload &7after editing.")));

        // Close button
        inv.setItem(SLOT_CLOSE, buildButton(
                Material.RED_CONCRETE,
                "&c&lCLOSE",
                List.of("&7Close this menu.")));

        player.openInventory(inv);
    }

    // ─── Rebuild a single slot (called after a toggle) ────────────────────────

    public void refreshToggle(Inventory inv, int slot, String configKey, String label,
                              String description, boolean defaultVal) {
        inv.setItem(slot, buildToggle(slotMaterial(slot), label, configKey, description, defaultVal));
    }

    private Material slotMaterial(int slot) {
        return switch (slot) {
            case 0  -> Material.ENCHANTED_BOOK;
            case 1  -> Material.NETHER_STAR;
            case 2  -> Material.CHEST;
            case 3  -> Material.IRON_SWORD;
            case 4  -> Material.NAME_TAG;
            default -> Material.PAPER;
        };
    }

    // ─── Item builders ────────────────────────────────────────────────────────

    public ItemStack buildTogglePublic(String label, String configKey,
                                       String description, boolean defaultVal) {
        return buildToggle(slotMaterial(0), label, configKey, description, defaultVal);
    }

    private ItemStack buildToggle(Material mat, String label, String configKey,
                                  String description, boolean defaultVal) {
        boolean enabled = plugin.getConfig().getBoolean(configKey, defaultVal);
        String status   = enabled ? "§a§lENABLED" : "§c§lDISABLED";
        Material indicator = enabled ? Material.LIME_CONCRETE : Material.RED_CONCRETE;

        // Use the indicator colour as the main material so it's obvious at a glance
        ItemStack item = new ItemStack(indicator);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize("&f&l" + label + " &8— " + status));
            List<String> lore = new ArrayList<>();
            lore.add(colorize("&7" + description));
            lore.add("");
            lore.add(colorize("&7Click to toggle."));
            // Store the config key in the item name tag so the click handler can read it
            lore.add(colorize("§0§0" + configKey)); // invisible marker line
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildButton(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(colorize(name));
            List<String> colored = new ArrayList<>();
            for (String l : lore) colored.add(colorize(l));
            meta.setLore(colored);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack g = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m  = g.getItemMeta();
        if (m != null) { m.setDisplayName(" "); g.setItemMeta(m); }
        return g;
    }

    /** Extracts the config key from the invisible marker line in toggle lore. */
    public String extractConfigKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        List<String> lore = item.getItemMeta().getLore();
        if (lore == null) return null;
        for (String line : lore) {
            if (line.startsWith("§0§0")) return line.substring(4);
        }
        return null;
    }

    public boolean isToggleSlot(int slot) {
        return slot == SLOT_REVIVE_BOOK_ENABLED
                || slot == SLOT_LIFE_TOKEN_ENABLED
                || slot == SLOT_DROP_TOKEN
                || slot == SLOT_PVP_ONLY
                || slot == SLOT_TAB_DISPLAY;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
