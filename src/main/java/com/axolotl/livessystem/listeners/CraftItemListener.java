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

    public CraftItemListener(LivesSystem plugin) {
        this.plugin        = plugin;
        this.reviveBookKey = new NamespacedKey(plugin, "revive_book_recipe");
        this.lifeTokenKey  = new NamespacedKey(plugin, "life_token_recipe");
        reloadRecipes();
    }

    // ─── Recipe Registration ──────────────────────────────────────────────────

    public void reloadRecipes() {
        // Remove old
        plugin.getServer().removeRecipe(reviveBookKey);
        plugin.getServer().removeRecipe(lifeTokenKey);

        // Re-register from config
        registerFromConfig("revive-book", reviveBookKey,
                plugin.getItemManager().createReviveBook());
        registerFromConfig("life-token", lifeTokenKey,
                plugin.getItemManager().createLifeToken());
    }

    private void registerFromConfig(String section, NamespacedKey key, ItemStack result) {
        boolean shaped = plugin.getConfig().getBoolean(section + ".recipe.shaped", true);
        Material[] grid = loadGrid(section);

        if (shaped) {
            // Build shape strings and ingredient map
            StringBuilder row1 = new StringBuilder();
            StringBuilder row2 = new StringBuilder();
            StringBuilder row3 = new StringBuilder();
            char[] chars = {'A','B','C','D','E','F','G','H','I'};
            List<Character> usedChars = new ArrayList<>();
            List<Material> usedMats  = new ArrayList<>();

            for (int i = 0; i < 9; i++) {
                char c;
                if (grid[i] == null || grid[i] == Material.AIR) {
                    c = ' ';
                } else {
                    // Reuse char if same material already mapped
                    int existing = usedMats.indexOf(grid[i]);
                    if (existing >= 0) {
                        c = usedChars.get(existing);
                    } else {
                        c = chars[usedChars.size()];
                        usedChars.add(c);
                        usedMats.add(grid[i]);
                    }
                }
                if (i < 3)      row1.append(c);
                else if (i < 6) row2.append(c);
                else            row3.append(c);
            }

            // Skip if grid is completely empty
            if (usedChars.isEmpty()) return;

            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(row1.toString(), row2.toString(), row3.toString());
            for (int i = 0; i < usedChars.size(); i++) {
                recipe.setIngredient(usedChars.get(i),
                        new RecipeChoice.MaterialChoice(usedMats.get(i)));
            }
            plugin.getServer().addRecipe(recipe);

        } else {
            // Shapeless — collect non-air materials
            ShapelessRecipe recipe = new ShapelessRecipe(key, result);
            boolean hasIngredient = false;
            for (Material mat : grid) {
                if (mat != null && mat != Material.AIR) {
                    recipe.addIngredient(new RecipeChoice.MaterialChoice(mat));
                    hasIngredient = true;
                }
            }
            if (!hasIngredient) return;
            plugin.getServer().addRecipe(recipe);
        }

        plugin.getLogger().info("Registered recipe for " + section + " ("
                + (shaped ? "shaped" : "shapeless") + ").");
    }

    private Material[] loadGrid(String section) {
        Material[] grid = new Material[9];
        for (int i = 0; i < 9; i++) {
            String matName = plugin.getConfig().getString(
                    section + ".recipe.grid." + i, "AIR");
            try { grid[i] = Material.valueOf(matName.toUpperCase()); }
            catch (IllegalArgumentException e) { grid[i] = Material.AIR; }
        }
        return grid;
    }

    // ─── Intercept crafting to swap result for named item ─────────────────────

    @EventHandler
    public void onPrepare(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();
        if (result == null) return;
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
        boolean shaped  = plugin.getConfig().getBoolean(section + ".recipe.shaped", true);
        Material[] grid = loadGrid(section);
        ItemStack[] matrix = inv.getMatrix();

        if (shaped) {
            // Exact slot-by-slot match
            for (int i = 0; i < 9; i++) {
                Material expected = (grid[i] == null) ? Material.AIR : grid[i];
                Material actual   = (matrix[i] == null) ? Material.AIR : matrix[i].getType();
                if (expected != actual) return false;
            }
            return true;
        } else {
            // Shapeless — same multiset of materials regardless of position
            List<Material> required = new ArrayList<>();
            for (Material m : grid) {
                if (m != null && m != Material.AIR) required.add(m);
            }
            List<Material> present = new ArrayList<>();
            for (ItemStack slot : matrix) {
                if (slot != null && slot.getType() != Material.AIR) present.add(slot.getType());
            }
            if (required.size() != present.size()) return false;
            List<Material> copy = new ArrayList<>(present);
            for (Material m : required) {
                if (!copy.remove(m)) return false;
            }
            return true;
        }
    }
}
