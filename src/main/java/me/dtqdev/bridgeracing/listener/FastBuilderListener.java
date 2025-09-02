package me.dtqdev.bridgeracing.listener;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.fastbuilder.Arena;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class FastBuilderListener implements Listener {

    private final BridgeRacing plugin;
    private final String itemName;
    private final Material itemMaterial;
    private final short itemData;
    private final int itemSlot;

    public FastBuilderListener(BridgeRacing plugin) {
        this.plugin = plugin;
        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("fastbuilder-item");
        this.itemName = itemConfig.getString("name");
        this.itemMaterial = Material.getMaterial(itemConfig.getString("material"));
        this.itemData = (short) itemConfig.getInt("data");
        this.itemSlot = itemConfig.getInt("slot");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        // Chỉ xử lý khi người chơi đang ở trong FastBuilder
        Arena arena = plugin.getFastBuilderAPI().getArenaManager().getArenaByPlayer(player);
        if (arena == null) return;
        
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            ItemStack itemInHand = player.getItemInHand();
            if (itemInHand != null &&
                itemInHand.getType() == itemMaterial &&
                itemInHand.getDurability() == itemData &&
                itemInHand.hasItemMeta() &&
                itemInHand.getItemMeta().getDisplayName().equals(itemName)) {
                
                event.setCancelled(true);
                plugin.getGuiManager().openMapSelector(player);
            }
        }
    }
}