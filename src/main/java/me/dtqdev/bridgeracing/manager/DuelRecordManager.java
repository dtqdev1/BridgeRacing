package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelRecord;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DuelRecordManager {

    private final BridgeRacing plugin;
    // Map<PlayerUUID, Map<MapID, DuelRecord>>
    private final Map<UUID, Map<String, DuelRecord>> recordCache = new ConcurrentHashMap<>();

    public DuelRecordManager(BridgeRacing plugin) {
        this.plugin = plugin;
        loadAllPlayerRecords();
    }

    public void loadAllPlayerRecords() {
        recordCache.clear();
        ConfigurationSection playersSection = plugin.getConfigManager().getPlayerDataConfig().getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidString : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            ConfigurationSection recordsSection = playersSection.getConfigurationSection(uuidString + ".records");
            if (recordsSection == null) continue;

            Map<String, DuelRecord> playerRecords = new ConcurrentHashMap<>();
            for (String mapId : recordsSection.getKeys(false)) {
                double time = recordsSection.getDouble(mapId + ".time");
                long timestamp = recordsSection.getLong(mapId + ".timestamp");
                playerRecords.put(mapId, new DuelRecord(time, timestamp));
            }
            recordCache.put(uuid, playerRecords);
        }
    }

    public double getBestTime(UUID uuid, String mapId) {
        Map<String, DuelRecord> playerRecords = recordCache.get(uuid);
        if (playerRecords != null && playerRecords.containsKey(mapId)) {
            return playerRecords.get(mapId).getTime();
        }
        return -1; // -1 nghĩa là chưa có kỷ lục
    }

    public void setBestTime(UUID uuid, String mapId, double time) {
        Map<String, DuelRecord> playerRecords = recordCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        
        double currentBest = getBestTime(uuid, mapId);
        if (currentBest == -1 || time < currentBest) {
            playerRecords.put(mapId, new DuelRecord(time, System.currentTimeMillis()));
            
            // Lưu vào config
            String path = "players." + uuid.toString() + ".records." + mapId;
            plugin.getConfigManager().getPlayerDataConfig().set(path + ".time", time);
            plugin.getConfigManager().getPlayerDataConfig().set(path + ".timestamp", System.currentTimeMillis());
        }
    }
}