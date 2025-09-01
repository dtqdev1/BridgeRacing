package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIManager {
    private final BridgeRacing plugin;

    public GUIManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    public void openMapSelector(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&1Chọn Map"));
        int rows = plugin.getConfig().getInt("gui.rows", 3);
        Inventory gui = Bukkit.createInventory(player, rows * 9, title);

        List<DuelArena> arenas = plugin.getDuelArenaManager().getAllDuelArenas();
        int slot = 0;
        for (DuelArena arena : arenas) {
            ItemStack item = new ItemStack(Material.getMaterial(arena.getGuiItemMaterial()));
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', arena.getDisplayName()));
            
            List<String> lore = new ArrayList<>();
            int queueSize = plugin.getQueueManager().getQueueSize(arena.getId());
            lore.add(ChatColor.GRAY + "Players in queue: " + ChatColor.YELLOW + queueSize);
            // Thêm lore ẩn để lưu map ID
            lore.add(ChatColor.BLACK + "map_id:" + arena.getId());

            meta.setLore(lore);
            item.setItemMeta(meta);
            gui.setItem(slot, item);
            slot++;
        }
        player.openInventory(gui);
    }
}