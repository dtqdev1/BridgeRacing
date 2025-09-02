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
    guiTitle = org.bukkit.ChatColor.translateAlternateColorCodes('&', guiTitle);

    if (title.equals(guiTitle)) {
        event.setCancelled(true);
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

        // SỬA Ở ĐÂY: Kiểm tra item hủy hàng chờ (BARRIER)
        String inQueueMaterial = plugin.getConfig().getString("gui.in-queue-item.material", "BARRIER");
        if (clickedItem.getType() == Material.getMaterial(inQueueMaterial)) {
            plugin.getQueueManager().removePlayerFromAllQueues(player);
            // Mở lại GUI ngay lập tức để hiển thị danh sách map
            plugin.getGuiManager().openMapSelector(player);
            return;
        }

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