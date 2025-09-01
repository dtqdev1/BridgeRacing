package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class DuelArenaManager {

    private final BridgeRacing plugin;
    private final List<DuelArena> duelArenas = new ArrayList<>();

    public DuelArenaManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        duelArenas.clear();
        ConfigurationSection arenasSection = plugin.getConfigManager().getDuelsConfig().getConfigurationSection("duel-arenas");
        if (arenasSection == null) {
            plugin.getLogger().warning("Không tìm thấy section 'duel-arenas' trong duels.yml.");
            return;
        }

        for (String arenaId : arenasSection.getKeys(false)) {
            ConfigurationSection arenaConfig = arenasSection.getConfigurationSection(arenaId);
            
            // Tải thông tin chung
            String displayName = arenaConfig.getString("display-name", "Unnamed Arena");
            String guiItem = arenaConfig.getString("gui-item", "STONE");

            // Tải thông tin của Người chơi 1
            Location p1Spawn = parseLocation(arenaConfig.getString("player1.spawn"));
            Location p1Corner1 = parseLocation(arenaConfig.getString("player1.corner1"));
            Location p1Corner2 = parseLocation(arenaConfig.getString("player1.corner2"));
            Location p1EndPlate = parseLocation(arenaConfig.getString("player1.end-plate"));
            List<Location> p1Checkpoints = new ArrayList<>();
            arenaConfig.getStringList("player1.checkpoints").forEach(locString -> p1Checkpoints.add(parseLocation(locString)));

            // Tải thông tin của Người chơi 2
            Location p2Spawn = parseLocation(arenaConfig.getString("player2.spawn"));
            Location p2Corner1 = parseLocation(arenaConfig.getString("player2.corner1"));
            Location p2Corner2 = parseLocation(arenaConfig.getString("player2.corner2"));
            Location p2EndPlate = parseLocation(arenaConfig.getString("player2.end-plate"));
            List<Location> p2Checkpoints = new ArrayList<>();
            arenaConfig.getStringList("player2.checkpoints").forEach(locString -> p2Checkpoints.add(parseLocation(locString)));

            // Kiểm tra tính hợp lệ
            if (p1Spawn == null || p2Spawn == null || p1EndPlate == null) {
                plugin.getLogger().severe("Map đấu '" + arenaId + "' bị cấu hình thiếu các vị trí quan trọng (spawn/end-plate). Đã vô hiệu hóa map này.");
                continue;
            }

            DuelArena duelArena = new DuelArena(arenaId, displayName, guiItem,
                    p1Spawn, p1Corner1, p1Corner2, p1EndPlate, p1Checkpoints,
                    p2Spawn, p2Corner1, p2Corner2, p2EndPlate, p2Checkpoints);
            
            duelArenas.add(duelArena);
        }
        plugin.getLogger().info("Đã tải " + duelArenas.size() + " map đấu BridgeRacing.");
    }

    private Location parseLocation(String locString) {
        if (locString == null || locString.isEmpty()) return null;
        try {
            String[] parts = locString.split(",");
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) {
                plugin.getLogger().warning("Không tìm thấy world '" + parts[0] + "' cho tọa độ.");
                return null;
            }
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = (parts.length > 4) ? Float.parseFloat(parts[4]) : 0.0f;
            float pitch = (parts.length > 5) ? Float.parseFloat(parts[5]) : 0.0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().severe("Lỗi khi đọc tọa độ: " + locString);
            return null;
        }
    }
    
    // --- Getters ---
    public DuelArena getDuelArenaById(String id) {
        return duelArenas.stream().filter(a -> a.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    public List<DuelArena> getAllDuelArenas() {
        return duelArenas;
    }
}