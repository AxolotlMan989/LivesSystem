package com.axolotl.livessystem.listeners;

import com.axolotl.livessystem.LivesSystem;
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

    // ─── Route clicks ─────────────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player clicker)) return;
        String title = event.getView().getTitle();

        if (title.contains("Revive Menu")) {
            handleReviveClick(event, clicker);
        } else if (title.contains("Edit Recipe:")) {
            handleEditorClick(event, clicker);
        }
    }

    /**
     * Block dragging items into non-grid slots of the editor.
     */
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        String title = event.getView().getTitle();
        if (!title.contains("Edit Recipe:")) return;

        RecipeEditorGUI editor = plugin.getRecipeEditorGUI();
        for (int slot : event.getRawSlots()) {
            // Only allow dragging into grid slots (slots 0-53 that are grid slots)
            if (slot < 54 && !editor.isGridSlot(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Return any items left in grid slots to the player's inventory on close
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

        if (slot == ReviveGUI.SLOT_PREV) {
            plugin.getReviveGUI().open(clicker, currentPage - 1);
            return;
        }
        if (slot == ReviveGUI.SLOT_NEXT) {
            plugin.getReviveGUI().open(clicker, currentPage + 1);
            return;
        }
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

        // ── Clicks inside the GUI (slots 0-53) ──────────────────────────────
        if (slot >= 0 && slot < 54) {

            // Grid slots — allow all normal interaction
            if (editor.isGridSlot(slot)) {
                // Only block if trying to place a UI filler item from cursor
                ItemStack cursor = event.getCursor();
                if (cursor != null && !cursor.getType().isAir() && editor.isUIItem(cursor)) {
                    event.setCancelled(true);
                }
                // Otherwise: let it through so player can place/take/swap freely
                return;
            }

            // Close button
            if (slot == RecipeEditorGUI.SLOT_CLOSE) {
                event.setCancelled(true);
                // Return grid items before closing
                for (int s : RecipeEditorGUI.GRID_SLOTS) {
                    ItemStack item = event.getInventory().getItem(s);
                    if (item != null && !item.getType().isAir() && !editor.isUIItem(item)) {
                        clicker.getInventory().addItem(item.clone());
                        event.getInventory().setItem(s, null);
                    }
                }
                clicker.closeInventory();
                return;
            }

            // Save button
            if (slot == RecipeEditorGUI.SLOT_SAVE) {
                event.setCancelled(true);
                editor.saveGridToConfig(event.getInventory(), itemType, shaped);
                String label = itemType.equals("revivebook") ? "Revive Book" : "Life Token";
                clicker.sendMessage(lm.colorize("&aRecipe for &6" + label + " &asaved and activated!"));
                // Return grid items
                for (int s : RecipeEditorGUI.GRID_SLOTS) {
                    ItemStack item = event.getInventory().getItem(s);
                    if (item != null && !item.getType().isAir() && !editor.isUIItem(item)) {
                        clicker.getInventory().addItem(item.clone());
                        event.getInventory().setItem(s, null);
                    }
                }
                clicker.closeInventory();
                return;
            }

            // Insert Revive Book placeholder
        if (slot == RecipeEditorGUI.SLOT_INSERT_BOOK) {
            event.setCancelled(true);
            // Find first empty grid slot
            for (int s : RecipeEditorGUI.GRID_SLOTS) {
                ItemStack existing = event.getInventory().getItem(s);
                if (existing == null || existing.getType().isAir()) {
                    event.getInventory().setItem(s, editor.buildReviveBookPlaceholder());
                    break;
                }
            }
            return;
        }

        // Insert Life Token placeholder
        if (slot == RecipeEditorGUI.SLOT_INSERT_TOKEN) {
            event.setCancelled(true);
            for (int s : RecipeEditorGUI.GRID_SLOTS) {
                ItemStack existing = event.getInventory().getItem(s);
                if (existing == null || existing.getType().isAir()) {
                    event.getInventory().setItem(s, editor.buildLifeTokenPlaceholder());
                    break;
                }
            }
            return;
        }

        // Mode toggle
            if (slot == RecipeEditorGUI.SLOT_MODE) {
                event.setCancelled(true);
                boolean newShaped = !shaped;
                clicker.setMetadata(RecipeEditorGUI.META_SHAPED,
                        new org.bukkit.metadata.FixedMetadataValue(plugin, newShaped ? 1 : 0));
                event.getInventory().setItem(RecipeEditorGUI.SLOT_MODE,
                        editor.buildModeButton(newShaped));
                clicker.sendMessage(lm.colorize("&7Recipe mode: "
                        + (newShaped ? "&bShaped" : "&eShapeless")));
                return;
            }

            // Output / arrow — never interact
            if (slot == RecipeEditorGUI.SLOT_OUTPUT || slot == RecipeEditorGUI.SLOT_ARROW
                || slot == RecipeEditorGUI.SLOT_INSERT_BOOK || slot == RecipeEditorGUI.SLOT_INSERT_TOKEN) {
                event.setCancelled(true);
                return;
            }

            // Any other GUI slot (filler) — cancel
            event.setCancelled(true);
            return;
        }

        // ── Clicks in the player's own inventory (slots 54+) ────────────────
        // Allow normal interaction EXCEPT shift-click which would push items
        // into non-grid GUI slots
        if (event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) {
            // Only allow shift-click from player inventory if there's a free grid slot
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) {
                // Find first empty grid slot and place item there manually
                for (int gridSlot : RecipeEditorGUI.GRID_SLOTS) {
                    ItemStack existing = event.getInventory().getItem(gridSlot);
                    if (existing == null || existing.getType().isAir()) {
                        event.setCancelled(true);
                        event.getInventory().setItem(gridSlot, clicked.clone());
                        clicker.getInventory().setItem(event.getSlot(), null);
                        return;
                    }
                }
                // No free grid slot — cancel
                event.setCancelled(true);
            }
        }
        // All other player inventory clicks: allow freely
    }
}
