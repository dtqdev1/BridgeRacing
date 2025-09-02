package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import me.dtqdev.bridgeracing.util.ItemBuilder;
import me.dtqdev.bridgeracing.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GUIManager {

    private final BridgeRacing plugin;
    private final MessageUtil msg;

    public GUIManager(BridgeRacing plugin) {
        this.plugin = plugin;
        this.msg = plugin.getMessageUtil();
    }

    public void openMapSelector(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&1Chọn Map"));
        int rows = plugin.getConfig().getInt("gui.rows", 3);
        Inventory gui = Bukkit.createInventory(player, rows * 9, title);

        // Hiển thị item rời hàng chờ nếu cần
        if (plugin.getQueueManager().isPlayerInQueue(player)) {
            addInQueueItem(gui, player);
            player.openInventory(gui);
            return;
        }

        Map<String, DuelArena> arenas = plugin.getDuelArenaManager().getDuelArenas();
        int slot = 0;
        for (DuelArena arena : arenas.values()) {
            if (slot >= gui.getSize()) break;

            ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("gui.map-items." + arena.getId());
            ItemBuilder builder;

            if (itemConfig != null) {
                // Lấy thông tin từ config.yml
                builder = new ItemBuilder(itemConfig.getString("material", "STONE"));
                builder.setName(itemConfig.getString("name", arena.getDisplayName()));
                List<String> lore = new ArrayList<>(itemConfig.getStringList("lore"));
                replaceLorePlaceholders(lore, arena);
                builder.setLore(lore);
            } else {
                // Fallback nếu không có config
                builder = new ItemBuilder(arena.getGuiItemMaterial());
                builder.setName(arena.getDisplayName());
                List<String> lore = new ArrayList<>();
                lore.add("&fNgười trong hàng chờ: &a{queue_size}");
                lore.add("&fĐang thi đấu: &a{playing_size}");
                replaceLorePlaceholders(lore, arena);
                builder.setLore(lore);
            }
            
            // Thêm map_id ẩn
            ItemMeta meta = builder.build().getItemMeta();
            List<String> finalLore = meta.getLore();
            finalLore.add(ChatColor.BLACK + "map_id:" + arena.getId());
            meta.setLore(finalLore);
            ItemStack finalItem = builder.build();
            finalItem.setItemMeta(meta);

            gui.setItem(slot, finalItem);
            slot++;
        }
        player.openInventory(gui);
    }

    private void addInQueueItem(Inventory gui, Player player) {
        ConfigurationSection itemConfig = plugin.getConfig().getConfigurationSection("gui.in-queue-item");
        String mapId = plugin.getQueueManager().getQueueMapId(player);
        DuelArena arena = plugin.getDuelArenaManager().getDuelArenaById(mapId);

        ItemBuilder builder = new ItemBuilder(itemConfig.getString("material"));
        builder.setName(itemConfig.getString("name"));

        long timeInQueue = plugin.getQueueManager().getTimeInQueue(player);
        String timeFormatted = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(timeInQueue),
                TimeUnit.MILLISECONDS.toSeconds(timeInQueue) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInQueue))
        );
        
        List<String> lore = new ArrayList<>(itemConfig.getStringList("lore"));
        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i).replace("{map_name}", arena.getDisplayName()).replace("{time}", timeFormatted));
        }
        builder.setLore(lore);
        
        gui.setItem(4, builder.build()); // Đặt ở giữa
    }
    
    private void replaceLorePlaceholders(List<String> lore, DuelArena arena) {
        int queueSize = plugin.getQueueManager().getQueueSize(arena.getId());
        int playingSize = plugin.getDuelGameManager().getPlayingCount(arena.getId());
        String waitTime = (queueSize > 0) ? "~" + (30 / (queueSize + 1)) + "s" : "Ngay!";

        for (int i = 0; i < lore.size(); i++) {
            lore.set(i, lore.get(i)
                    .replace("{queue_size}", String.valueOf(queueSize))
                    .replace("{playing_size}", String.valueOf(playingSize))
                    .replace("{wait_time}", waitTime));
        }
    }
    public void startGuiUpdateTask() {
        new org.bukkit.scheduler.BukkitRunnable() {
            @Override
            public void run() {
                String guiTitle = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("gui.title", "&1Chọn Map"));
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getOpenInventory() != null && player.getOpenInventory().getTitle().equals(guiTitle)) {
                        if (plugin.getQueueManager().isPlayerInQueue(player)) {
                            // Chỉ cập nhật item "in-queue"
                            addInQueueItem(player.getOpenInventory().getTopInventory(), player);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Chạy mỗi giây
    }
}