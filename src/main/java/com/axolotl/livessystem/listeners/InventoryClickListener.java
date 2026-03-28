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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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

        // ── Revive GUI ────────────────────────────────────────────────────────
        if (title.contains("Revive Menu")) {
            handleReviveClick(event, clicker);
            return;
        }

        // ── Recipe Editor GUI ─────────────────────────────────────────────────
        if (title.contains("Edit Recipe:")) {
            handleEditorClick(event, clicker);
            return;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        // Clean up editor metadata when GUI is closed
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

        // Get editor state from metadata
        List<MetadataValue> typeMeta   = clicker.getMetadata(RecipeEditorGUI.META_ITEM_TYPE);
        List<MetadataValue> shapedMeta = clicker.getMetadata(RecipeEditorGUI.META_SHAPED);
        if (typeMeta.isEmpty()) return;

        String itemType = typeMeta.get(0).asString();
        boolean shaped  = shapedMeta.isEmpty() ? true : shapedMeta.get(0).asInt() == 1;

        // ── Close button ──
        if (slot == RecipeEditorGUI.SLOT_CLOSE) {
            event.setCancelled(true);
            clicker.closeInventory();
            return;
        }

        // ── Save button ──
        if (slot == RecipeEditorGUI.SLOT_SAVE) {
            event.setCancelled(true);
            editor.saveGridToConfig(event.getInventory(), itemType, shaped);
            String label = itemType.equals("revivebook") ? "Revive Book" : "Life Token";
            clicker.sendMessage(lm.colorize("&aRecipe for &6" + label + " &asaved and activated!"));
            clicker.closeInventory();
            return;
        }

        // ── Mode toggle ──
        if (slot == RecipeEditorGUI.SLOT_MODE) {
            event.setCancelled(true);
            boolean newShaped = !shaped;
            clicker.setMetadata(RecipeEditorGUI.META_SHAPED,
                    new org.bukkit.metadata.FixedMetadataValue(plugin, newShaped ? 1 : 0));
            event.getInventory().setItem(RecipeEditorGUI.SLOT_MODE,
                    editor.buildModeButton(newShaped));
            clicker.sendMessage(lm.colorize("&7Recipe mode set to: "
                    + (newShaped ? "&bShaped" : "&eSHAPELESS")));
            return;
        }

        // ── Output slot — never allow interaction ──
        if (slot == RecipeEditorGUI.SLOT_OUTPUT || slot == RecipeEditorGUI.SLOT_ARROW) {
            event.setCancelled(true);
            return;
        }

        // ── Grid slots — allow placing/removing items freely ──
        if (editor.isGridSlot(slot)) {
            // Allow — player can place/take items from grid slots naturally
            // But cancel if the item being placed is a UI item
            ItemStack cursor = event.getCursor();
            if (cursor != null && editor.isUIItem(cursor)) {
                event.setCancelled(true);
            }
            return;
        }

        // ── Everything else (filler/background) — cancel ──
        event.setCancelled(true);
    }
}
