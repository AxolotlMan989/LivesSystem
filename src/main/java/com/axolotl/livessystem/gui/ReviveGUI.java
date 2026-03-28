package com.axolotl.livessystem.gui;

import com.axolotl.livessystem.LivesSystem;
import com.axolotl.livessystem.managers.LivesManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Paginated GUI showing eliminated players as their own heads.
 * Click a head to revive that player.
 *
 * Layout (54-slot chest):
 *   Slots 0-44  → player heads (45 slots = 5 rows × 9 columns)
 *   Slot  45    → [Previous Page] (left arrow)
 *   Slot  49    → page info filler
 *   Slot  53    → [Next Page] (right arrow)
 *   Slots 46-48 and 50-52 → grey glass pane filler
 */
public class ReviveGUI {

    // How many player slots per page
    public static final int SLOTS_PER_PAGE = 45;
    public static final int INV_SIZE       = 54;

    // Navigation bar slots
    public static final int SLOT_PREV = 45;
    public static final int SLOT_INFO = 49;
    public static final int SLOT_NEXT = 53;

    private final LivesSystem plugin;

    public ReviveGUI(LivesSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Opens the revive GUI for the given player at the given page (0-indexed).
     */
    public void open(Player opener, int page) {
        LivesManager lm = plugin.getLivesManager();

        // Collect all eliminated players
        List<UUID> eliminated = lm.getEliminatedPlayers();

        if (eliminated.isEmpty()) {
            opener.sendMessage(lm.colorize("&eNo players are currently eliminated."));
            return;
        }

        int totalPages = (int) Math.ceil((double) eliminated.size() / SLOTS_PER_PAGE);
        int safePage   = Math.max(0, Math.min(page, totalPages - 1));

        int start = safePage * SLOTS_PER_PAGE;
        int end   = Math.min(start + SLOTS_PER_PAGE, eliminated.size());
        List<UUID> pageList = eliminated.subList(start, end);

        String title = colorize("&8Revive Menu &7— Page " + (safePage + 1) + "/" + totalPages);
        Inventory inv = Bukkit.createInventory(null, INV_SIZE, title);

        // Fill player heads
        for (int i = 0; i < pageList.size(); i++) {
            UUID uuid = pageList.get(i);
            inv.setItem(i, buildSkull(uuid));
        }

        // Navigation bar background
        ItemStack filler = buildFiller();
        for (int slot = SLOTS_PER_PAGE; slot < INV_SIZE; slot++) {
            inv.setItem(slot, filler);
        }

        // Previous page button
        if (safePage > 0) {
            inv.setItem(SLOT_PREV, buildNavButton(
                    Material.ARROW,
                    "&a« Previous Page",
                    "&7Page " + safePage + " of " + totalPages
            ));
        }

        // Page info
        inv.setItem(SLOT_INFO, buildNavButton(
                Material.PAPER,
                "&ePage " + (safePage + 1) + " / " + totalPages,
                "&7" + eliminated.size() + " eliminated player" + (eliminated.size() == 1 ? "" : "s")
        ));

        // Next page button
        if (safePage < totalPages - 1) {
            inv.setItem(SLOT_NEXT, buildNavButton(
                    Material.ARROW,
                    "&aNext Page »",
                    "&7Page " + (safePage + 2) + " of " + totalPages
            ));
        }

        // Store page index in the opener's metadata so the click handler can read it
        opener.setMetadata("livessystem_revivepage",
                new org.bukkit.metadata.FixedMetadataValue(plugin, safePage));

        opener.openInventory(inv);
    }

    // ─── Item Builders ────────────────────────────────────────────────────────

    private ItemStack buildSkull(UUID uuid) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta  = (SkullMeta) skull.getItemMeta();
        if (meta == null) return skull;

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);

        meta.setOwningPlayer(op);
        meta.setDisplayName(colorize("&c" + name));

        int lives = plugin.getLivesManager().getLives(uuid);
        List<String> lore = new ArrayList<>();
        lore.add(colorize("&7Status: &4ELIMINATED"));
        lore.add(colorize("&7Lives before elimination: &c" + lives));
        lore.add("");
        lore.add(colorize("&eClick to revive " + name));
        meta.setLore(lore);

        // Store UUID in persistent data so click handler can identify the target
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "revive_target"),
                org.bukkit.persistence.PersistentDataType.STRING,
                uuid.toString()
        );
        skull.setItemMeta(meta);
        return skull;
    }

    private ItemStack buildNavButton(Material material, String name, String loreText) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta  = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(colorize(name));
        List<String> lore = new ArrayList<>();
        lore.add(colorize(loreText));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildFiller() {
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta   = glass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            glass.setItemMeta(meta);
        }
        return glass;
    }

    private String colorize(String text) {
        return text == null ? "" : text.replace("&", "§");
    }
}
