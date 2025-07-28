package dtqdev.BridgeRacing;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.List;

public class PvPGuiManager {
    private final BridgeRacing plugin;
    private final DuelArenaManager arenaManager;
    private final DuelMatchManager matchManager;

    public PvPGuiManager(BridgeRacing plugin, DuelArenaManager arenaManager, DuelMatchManager matchManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.matchManager = matchManager;
    }

    public void openGui(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "BridgeRacing Maps");
        int slot = 10;
        for (DuelArena arena : arenaManager.getArenas()) {
            if (slot > 16) break;
            ItemStack item = new ItemStack(Material.valueOf(arena.getDisplayItem()));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', arena.getDisplayName()));
            List<String> lore = new ArrayList<>();
            for (String line : arena.getDisplayLore()) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            lore.add(ChatColor.GRAY + "Waiting: " + matchManager.getQueueSize(arena.getId()));
            meta.setLore(lore);
            if (matchManager.getQueueSize(arena.getId()) > 0) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }
        player.openInventory(gui);
    }

    public void updatePlayerItem(Player player, boolean inQueue) {
        ItemStack item = new ItemStack(Material.INK_SACK, 1, (short) (inQueue ? 1 : 10));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', inQueue ? "&cLeave Queue" : "&aJoin BridgeRacing"));
        item.setItemMeta(meta);
        player.getInventory().setItem(8, item);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        if (!event.getView().getTitle().equals(ChatColor.DARK_GREEN + "BridgeRacing Maps")) return;
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        int slot = event.getSlot();
        if (slot < 10 || slot > 16) return;
        int index = slot - 10;
        if (index >= arenaManager.getArenas().size()) return;
        DuelArena arena = arenaManager.getArenas().get(index);
        matchManager.addToQueue(player, arena.getId());
        player.closeInventory();
    }
}