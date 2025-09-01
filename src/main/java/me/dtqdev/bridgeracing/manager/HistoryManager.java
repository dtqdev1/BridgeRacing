package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryManager {
    private final BridgeRacing plugin;
    private final int maxHistoryEntries = 10;

    public HistoryManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    public void addMatchHistory(UUID playerUuid, String opponentName, boolean won, int eloChange) {
        FileConfiguration historyConfig = plugin.getConfigManager().getHistoryConfig();
        String path = "history." + playerUuid.toString();
        List<Map<?, ?>> history = historyConfig.getMapList(path);

        // Tạo một entry mới
        Map<String, Object> newEntry = new HashMap<>();
        newEntry.put("opponent", opponentName);
        newEntry.put("result", won ? "WIN" : "LOSS");
        newEntry.put("elo_change", eloChange);
        newEntry.put("timestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));

        // Thêm vào đầu danh sách
        history.add(0, newEntry);

        // Giữ danh sách chỉ có 10 entry gần nhất
        if (history.size() > maxHistoryEntries) {
            history = history.subList(0, maxHistoryEntries);
        }

        historyConfig.set(path, history);
    }
}