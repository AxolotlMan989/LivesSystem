package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.gui.ConfigGUI;
import com.axolotl.livessystem.gui.RecipeEditorGUI;
import com.axolotl.livessystem.gui.ReviveGUI;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.UUID;

public class InventoryClickListener implements Listener {

    private final LivesSystem plugin;

    public InventoryClickListener(LivesSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        String title = event.getView().getTitle();

        if (title.equals(ConfigGUI.GUI_TITLE)) {
            handleConfigClick(event, clicker);
        } else if (title.contains("Revive Menu")) {
            handleReviveClick(event, clicker);
        } else if (title.contains("Edit Recipe:")) {
            handleEditorClick(event, clicker);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (title.equals(ConfigGUI.GUI_TITLE)) {
            event.setCancelled(true);
            return;
        }
        if (!title.contains("Edit Recipe:")) return;
        RecipeEditorGUI editor = plugin.getRecipeEditorGUI();
        for (int slot : event.getRawSlots()) {
            if (slot < 54 && !editor.isGridSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (event.getView().getTitle().contains("Edit Recipe:")) {
            RecipeEditorGUI editor = plugin.getRecipeEditorGUI();
            for (int slot : RecipeEditorGUI.GRID_SLOTS) {
                ItemStack item = event.getInventory().getItem(slot);
                if (item != null && item.getType() != Material.AIR && !editor.isUIItem(item)) {
                    player.getInventory().addItem(item);
                    event.getInventory().setItem(slot, null);
                }
            }
        }
        player.removeMetadata(RecipeEditorGUI.META_ITEM_TYPE, plugin);
        player.removeMetadata(RecipeEditorGUI.META_SHAPED, plugin);
    }

    // ─── Config GUI ───────────────────────────────────────────────────────────

    private void handleConfigClick(InventoryClickEvent event, Player clicker) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 54) return;

        ConfigGUI gui = plugin.getConfigGUI();
        LivesManager lm = plugin.getLivesManager();

        // Close
        if (slot == ConfigGUI.SLOT_CLOSE) {
            clicker.closeInventory();
            return;
        }

        // Toggle slots
        if (gui.isToggleSlot(slot)) {
            ItemStack item = event.getCurrentItem();
            String configKey = gui.extractConfigKey(item);
            if (configKey == null) return;

            boolean current = plugin.getConfig().getBoolean(configKey, true);
            boolean newVal  = !current;
            plugin.getConfig().set(configKey, newVal);
            plugin.saveConfig();

            // Refresh just this slot
            event.getInventory().setItem(slot,
                    buildRefreshedToggle(gui, slot, configKey, newVal));

            clicker.sendMessage(lm.colorize("&7" + configKey + " set to: "
                    + (newVal ? "&aenabled" : "&cdisabled")));

            // Apply live if it's TAB display
            if (configKey.equals("tab-display")) plugin.getTabManager().updateAll();
            return;
        }

        // Recipe editors
        if (slot == ConfigGUI.SLOT_EDIT_REVIVE_RECIPE) {
            clicker.closeInventory();
            plugin.getRecipeEditorGUI().open(clicker, "revivebook");
            return;
        }
        if (slot == ConfigGUI.SLOT_EDIT_TOKEN_RECIPE) {
            clicker.closeInventory();
            plugin.getRecipeEditorGUI().open(clicker, "lifetoken");
            return;
        }
    }

    /** Re-builds a toggle item after it's been flipped. */
    private ItemStack buildRefreshedToggle(ConfigGUI gui, int slot, String configKey, boolean newVal) {
        // Map slots back to their label/description
        return switch (slot) {
            case ConfigGUI.SLOT_REVIVE_BOOK_ENABLED -> gui.buildTogglePublic(
                    "Revive Book", configKey, "Allows players to use the Revive Book item.", true);
            case ConfigGUI.SLOT_LIFE_TOKEN_ENABLED  -> gui.buildTogglePublic(
                    "Life Token", configKey, "Allows players to use the Life Token item.", true);
            case ConfigGUI.SLOT_DROP_TOKEN          -> gui.buildTogglePublic(
                    "Drop Token on Death", configKey, "Players drop a Life Token when they die.", true);
            case ConfigGUI.SLOT_PVP_ONLY            -> gui.buildTogglePublic(
                    "PvP Only Token Drop", configKey, "Token only drops on player kills.", false);
            case ConfigGUI.SLOT_TAB_DISPLAY         -> gui.buildTogglePublic(
                    "TAB Lives Display", configKey, "Show lives count in the TAB list.", true);
            default -> new ItemStack(Material.AIR);
        };
    }

    // ─── Revive GUI ───────────────────────────────────────────────────────────

    private void handleReviveClick(InventoryClickEvent event, Player clicker) {
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType() == Material.GRAY_STAINED_GLASS_PANE) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;

        int currentPage = 0;
        List<MetadataValue> pageMeta = clicker.getMetadata("livessystem_revivepage");
        if (!pageMeta.isEmpty()) currentPage = pageMeta.get(0).asInt();

        int slot = event.getRawSlot();
        if (slot == ReviveGUI.SLOT_PREV) { plugin.getReviveGUI().open(clicker, currentPage - 1); return; }
        if (slot == ReviveGUI.SLOT_NEXT) { plugin.getReviveGUI().open(clicker, currentPage + 1); return; }
        if (slot == ReviveGUI.SLOT_INFO) return;
        if (slot >= ReviveGUI.SLOTS_PER_PAGE) return;

        String uuidStr = meta.getPersistentDataContainer().get(
                new NamespacedKey(plugin, "revive_target"), PersistentDataType.STRING);
        if (uuidStr == null) return;

        UUID targetUuid;
        try { targetUuid = UUID.fromString(uuidStr); }
        catch (IllegalArgumentException e) { return; }

        LivesManager lm = plugin.getLivesManager();
        if (!lm.isEliminated(targetUuid)) {
            clicker.sendMessage(lm.colorize(plugin.getConfig().getString(
                    "messages.target-not-dead", "&cThat player is not eliminated!")));
            clicker.closeInventory();
            return;
        }

        boolean success = lm.revivePlayer(targetUuid, clicker);
        if (success) {
            if (plugin.getConfig().getBoolean("revive-book.consume-on-use", true)) {
                ItemStack book = clicker.getInventory().getItemInMainHand();
                if (plugin.getItemManager().isReviveBook(book)) {
                    if (book.getAmount() > 1) book.setAmount(book.getAmount() - 1);
                    else clicker.getInventory().setItemInMainHand(null);
                }
            }
            String targetName = org.bukkit.Bukkit.getOfflinePlayer(targetUuid).getName();
            clicker.sendMessage(lm.colorize(plugin.getConfig().getString(
                    "messages.revived-other", "&aYou revived &e%player%&a!")
                    .replace("%player%", targetName != null ? targetName : uuidStr)));
        }
        clicker.closeInventory();
    }

    // ─── Recipe Editor GUI ────────────────────────────────────────────────────

    private void handleEditorClick(InventoryClickEvent event, Player clicker) {
        int slot = event.getRawSlot();
        RecipeEditorGUI editor = plugin.getRecipeEditorGUI();
        LivesManager lm = plugin.getLivesManager();

        List<MetadataValue> typeMeta   = clicker.getMetadata(RecipeEditorGUI.META_ITEM_TYPE);
        List<MetadataValue> shapedMeta = clicker.getMetadata(RecipeEditorGUI.META_SHAPED);
        if (typeMeta.isEmpty()) return;

        String itemType = typeMeta.get(0).asString();
        boolean shaped  = shapedMeta.isEmpty() || shapedMeta.get(0).asInt() == 1;

        if (slot >= 0 && slot < 54) {
            if (editor.isGridSlot(slot)) {
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir() && editor.isUIItem(cursor)) {
                    event.setCancelled(true);
                }
                return;
            }

            if (slot == RecipeEditorGUI.SLOT_CLOSE) {
                event.setCancelled(true);
                returnGridItems(event, editor, clicker);
                clicker.closeInventory();
                return;
            }

            if (slot == RecipeEditorGUI.SLOT_SAVE) {
                event.setCancelled(true);
                editor.saveGridToConfig(event.getInventory(), itemType, shaped);
                String label = itemType.equals("revivebook") ? "Revive Book" : "Life Token";
                clicker.sendMessage(lm.colorize("&aRecipe for &6" + label + " &asaved!"));
                returnGridItems(event, editor, clicker);
                clicker.closeInventory();
                return;
            }

            if (slot == RecipeEditorGUI.SLOT_MODE) {
                event.setCancelled(true);
                boolean newShaped = !shaped;
                clicker.setMetadata(RecipeEditorGUI.META_SHAPED,
                        new org.bukkit.metadata.FixedMetadataValue(plugin, newShaped ? 1 : 0));
                event.getInventory().setItem(RecipeEditorGUI.SLOT_MODE,
                        editor.buildModeButton(newShaped));
                clicker.sendMessage(lm.colorize("&7Mode: " + (newShaped ? "&bShaped" : "&eShapeless")));
                return;
            }

            if (slot == RecipeEditorGUI.SLOT_INSERT_BOOK) {
                event.setCancelled(true);
                insertIntoFirstFreeGridSlot(event.getInventory(), editor,
                        editor.buildReviveBookPlaceholder());
                return;
            }

            if (slot == RecipeEditorGUI.SLOT_INSERT_TOKEN) {
                event.setCancelled(true);
                insertIntoFirstFreeGridSlot(event.getInventory(), editor,
                        editor.buildLifeTokenPlaceholder());
                return;
            }

            if (slot == RecipeEditorGUI.SLOT_OUTPUT || slot == RecipeEditorGUI.SLOT_ARROW
                    || slot == RecipeEditorGUI.SLOT_INSERT_BOOK
                    || slot == RecipeEditorGUI.SLOT_INSERT_TOKEN) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true);
            return;
        }

        // Player inventory — handle shift-click
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                for (int gridSlot : RecipeEditorGUI.GRID_SLOTS) {
                    ItemStack existing = event.getInventory().getItem(gridSlot);
                    if (existing == null || existing.getType().isAir()) {
                        event.setCancelled(true);
                        event.getInventory().setItem(gridSlot, clicked.clone());
                        clicker.getInventory().setItem(event.getSlot(), null);
                        return;
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    private void returnGridItems(InventoryClickEvent event, RecipeEditorGUI editor, Player clicker) {
        for (int s : RecipeEditorGUI.GRID_SLOTS) {
            ItemStack item = event.getInventory().getItem(s);
            if (item != null && !item.getType().isAir() && !editor.isUIItem(item)) {
                clicker.getInventory().addItem(item.clone());
                event.getInventory().setItem(s, null);
            }
        }
    }

    private void insertIntoFirstFreeGridSlot(org.bukkit.inventory.Inventory inv,
                                              RecipeEditorGUI editor, ItemStack placeholder) {
        for (int s : RecipeEditorGUI.GRID_SLOTS) {
            ItemStack existing = inv.getItem(s);
            if (existing == null || existing.getType().isAir()) {
                inv.setItem(s, placeholder);
                return;
            }
        }
    }
}
