package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.ItemManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

public class CraftItemListener implements Listener {

    private final LivesSystem plugin;

    private NamespacedKey reviveBookKey;
    private NamespacedKey lifeTokenKey;

    // Special sentinel strings used in the config grid to represent plugin items
    public static final String PLUGIN_REVIVE_BOOK  = "PLUGIN_REVIVE_BOOK";
    public static final String PLUGIN_LIFE_TOKEN   = "PLUGIN_LIFE_TOKEN";

    public CraftItemListener(LivesSystem plugin) {
        this.plugin        = plugin;
        this.reviveBookKey = new NamespacedKey(plugin, "revive_book_recipe");
        this.lifeTokenKey  = new NamespacedKey(plugin, "life_token_recipe");
        reloadRecipes();
    }

    // ─── Recipe Registration ──────────────────────────────────────────────────

    public void reloadRecipes() {
        plugin.getServer().removeRecipe(reviveBookKey);
        plugin.getServer().removeRecipe(lifeTokenKey);
        registerFromConfig("revive-book", reviveBookKey,
                plugin.getItemManager().createReviveBook());
        registerFromConfig("life-token", lifeTokenKey,
                plugin.getItemManager().createLifeToken());
    }

    private void registerFromConfig(String section, NamespacedKey key, ItemStack result) {
        boolean shaped   = plugin.getConfig().getBoolean(section + ".recipe.shaped", true);
        String[] grid    = loadRawGrid(section); // raw strings, may include PLUGIN_ sentinels

        // For Bukkit recipe registration we substitute plugin items with their
        // base material so Bukkit knows roughly what to expect in the grid.
        // Actual name-checking happens in matchesRecipe() at craft time.
        Material[] matGrid = toMaterialGrid(grid);

        if (shaped) {
            StringBuilder row1 = new StringBuilder();
            StringBuilder row2 = new StringBuilder();
            StringBuilder row3 = new StringBuilder();
            char[] chars = {'A','B','C','D','E','F','G','H','I'};
            List<Character> usedChars = new ArrayList<>();
            List<Material>  usedMats  = new ArrayList<>();

            for (int i = 0; i < 9; i++) {
                char c;
                if (matGrid[i] == null || matGrid[i] == Material.AIR) {
                    c = ' ';
                } else {
                    int existing = usedMats.indexOf(matGrid[i]);
                    if (existing >= 0) {
                        c = usedChars.get(existing);
                    } else {
                        c = chars[usedChars.size()];
                        usedChars.add(c);
                        usedMats.add(matGrid[i]);
                    }
                }
                if (i < 3)      row1.append(c);
                else if (i < 6) row2.append(c);
                else            row3.append(c);
            }

            if (usedChars.isEmpty()) return;

            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(row1.toString(), row2.toString(), row3.toString());
            for (int i = 0; i < usedChars.size(); i++) {
                recipe.setIngredient(usedChars.get(i),
                        new RecipeChoice.MaterialChoice(usedMats.get(i)));
            }
            plugin.getServer().addRecipe(recipe);

        } else {
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            boolean any = false;
            for (Material mat : matGrid) {
                if (mat != null && mat != Material.AIR) {
                    recipe.addIngredient(new RecipeChoice.MaterialChoice(mat));
                    any = true;
                }
            }
            if (!any) return;
            plugin.getServer().addRecipe(recipe);
        }

        plugin.getLogger().info("Registered recipe for " + section
                + " (" + (shaped ? "shaped" : "shapeless") + ").");
    }

    // ─── Grid Loading ─────────────────────────────────────────────────────────

    /** Loads raw grid strings from config, preserving PLUGIN_ sentinel values. */
    private String[] loadRawGrid(String section) {
        String[] grid = new String[9];
        for (int i = 0; i < 9; i++) {
            grid[i] = plugin.getConfig().getString(
                    section + ".recipe.grid." + i, "AIR").toUpperCase();
        }
        return grid;
    }

    /**
     * Converts a raw grid (which may contain PLUGIN_ sentinels) into Materials.
     * Plugin items are substituted with their base material for Bukkit registration.
     */
    private Material[] toMaterialGrid(String[] rawGrid) {
        Material[] mats = new Material[9];
        for (int i = 0; i < 9; i++) {
            mats[i] = rawToMaterial(rawGrid[i]);
        }
        return mats;
    }

    private Material rawToMaterial(String raw) {
        if (raw == null || raw.equals("AIR")) return Material.AIR;
        if (raw.equals(PLUGIN_REVIVE_BOOK)) {
            // Base material of the revive book
            String mat = plugin.getConfig().getString("revive-book.material", "ENCHANTED_BOOK");
            try { return Material.valueOf(mat.toUpperCase()); }
            catch (IllegalArgumentException e) { return Material.ENCHANTED_BOOK; }
        }
        if (raw.equals(PLUGIN_LIFE_TOKEN)) {
            // Base material of the life token
            String mat = plugin.getConfig().getString("life-token.material", "NETHER_STAR");
            try { return Material.valueOf(mat.toUpperCase()); }
            catch (IllegalArgumentException e) { return Material.NETHER_STAR; }
        }
        try { return Material.valueOf(raw); }
        catch (IllegalArgumentException e) { return Material.AIR; }
    }

    // ─── Craft Interception ───────────────────────────────────────────────────

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemManager im = plugin.getItemManager();
        CraftingInventory inv = event.getInventory();

        if (matchesRecipe(inv, "revive-book")) {
            inv.setResult(im.createReviveBook());
        } else if (matchesRecipe(inv, "life-token")) {
            inv.setResult(im.createLifeToken());
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemManager im        = plugin.getItemManager();

        if (matchesRecipe(inv, "revive-book")) {
            event.setCurrentItem(im.createReviveBook());
        } else if (matchesRecipe(inv, "life-token")) {
            event.setCurrentItem(im.createLifeToken());
        }
    }

    // ─── Recipe Matching ──────────────────────────────────────────────────────

    private boolean matchesRecipe(CraftingInventory inv, String section) {
        boolean shaped   = plugin.getConfig().getBoolean(section + ".recipe.shaped", true);
        String[] rawGrid = loadRawGrid(section);
        ItemStack[] matrix = inv.getMatrix();

        if (shaped) {
            for (int i = 0; i < 9; i++) {
                if (!slotMatches(matrix[i], rawGrid[i])) return false;
            }
            return true;
        } else {
            // Shapeless — same multiset regardless of position
            List<String> required = new ArrayList<>();
            for (String s : rawGrid) {
                if (s != null && !s.equals("AIR")) required.add(s);
            }
            List<ItemStack> present = new ArrayList<>();
            for (ItemStack slot : matrix) {
                if (slot != null && slot.getType() != Material.AIR) present.add(slot);
            }
            if (required.size() != present.size()) return false;

            List<ItemStack> copy = new ArrayList<>(present);
            for (String req : required) {
                boolean found = false;
                for (int j = 0; j < copy.size(); j++) {
                    if (slotMatches(copy.get(j), req)) {
                        copy.remove(j);
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }

    /**
     * Returns true if the given ItemStack satisfies the raw grid requirement.
     *
     * PLUGIN_REVIVE_BOOK  → must be a Revive Book (checked by display name)
     * PLUGIN_LIFE_TOKEN   → must be a Life Token (checked by display name)
     * AIR / null          → slot must be empty
     * anything else       → material must match
     */
    private boolean slotMatches(ItemStack item, String raw) {
        if (raw == null || raw.equals("AIR")) {
            return item == null || item.getType() == Material.AIR;
        }

        if (item == null || item.getType() == Material.AIR) return false;

        ItemManager im = plugin.getItemManager();

        if (raw.equals(PLUGIN_REVIVE_BOOK)) return im.isReviveBook(item);
        if (raw.equals(PLUGIN_LIFE_TOKEN))  return im.isLifeToken(item);

        try {
            return item.getType() == Material.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
