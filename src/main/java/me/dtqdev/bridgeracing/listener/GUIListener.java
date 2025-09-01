// GUIListener.java
package me.dtqdev.bridgeracing.listener;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GUIListener implements Listener {

    private final BridgeRacing plugin;

    public GUIListener(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        String guiTitle = plugin.getConfig().getString("gui.title");
        // Chuyển đổi mã màu
        guiTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', guiTitle);
        
        if (title.equals(guiTitle)) {
            event.setCancelled(true);
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // Xử lý click vào item RED_DYE để rời queue
            if (clickedItem.getType() == Material.INK_SACK && clickedItem.getDurability() == 1) {
                plugin.getQueueManager().removePlayerFromAllQueues(player);
                player.sendMessage("Đã rời hàng chờ.");
                // Mở lại GUI
                plugin.getGuiManager().openMapSelector(player);
                return;
            }

            // Lấy mapId từ item (sẽ cần lưu trong lore hoặc NBT)
            if (clickedItem.hasItemMeta() && clickedItem.getItemMeta().hasLore()) {
                java.util.List<String> lore = clickedItem.getItemMeta().getLore();
                for (String line : lore) {
                    if (line.startsWith(org.bukkit.ChatColor.BLACK + "map_id:")) {
                        String mapId = line.substring((org.bukkit.ChatColor.BLACK + "map_id:").length());
                        plugin.getQueueManager().addPlayerToQueue(player, mapId);
                        break;
                    }
                }
            }
        }
    }
}