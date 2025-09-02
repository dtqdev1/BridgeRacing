package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DuelArenaManager {
    private final BridgeRacing plugin;
    private final Map<String, DuelArena> duelArenas = new HashMap<>();

    public DuelArenaManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    public void loadArenas() {
        duelArenas.clear();
        FileConfiguration config = plugin.getConfigManager().getDuelsConfig();
        if (!config.isConfigurationSection("duel-arenas")) {
            plugin.getLogger().info("No duel arenas found in configuration.");
            return;
        }
        for (String id : config.getConfigurationSection("duel-arenas").getKeys(false)) {
            String basePath = "duel-arenas." + id;
            String displayName = config.getString(basePath + ".display-name");
            String guiItem = config.getString(basePath + ".gui-item");
            Location p1_spawn = parseLocation(config.getString(basePath + ".player1.spawn"));
            Location p1_corner1 = parseLocation(config.getString(basePath + ".player1.corner1"));
            Location p1_corner2 = parseLocation(config.getString(basePath + ".player1.corner2"));
            Location p1_endPlate = parseLocation(config.getString(basePath + ".player1.end-plate"));
            List<Location> p1_checkpoints = parseLocationList(config.getStringList(basePath + ".player1.checkpoints"));
            Location p2_spawn = parseLocation(config.getString(basePath + ".player2.spawn"));
            Location p2_corner1 = parseLocation(config.getString(basePath + ".player2.corner1"));
            Location p2_corner2 = parseLocation(config.getString(basePath + ".player2.corner2"));
            Location p2_endPlate = parseLocation(config.getString(basePath + ".player2.end-plate"));
            List<Location> p2_checkpoints = parseLocationList(config.getStringList(basePath + ".player2.checkpoints"));
            if (p1_spawn == null || p1_corner1 == null || p1_corner2 == null || p1_endPlate == null ||
                p2_spawn == null || p2_corner1 == null || p2_corner2 == null || p2_endPlate == null) {
                plugin.getLogger().warning("Invalid configuration for arena " + id + ". Skipping.");
                continue;
            }
            DuelArena arena = new DuelArena(id, displayName, guiItem, p1_spawn, p1_corner1, p1_corner2, p1_endPlate, p1_checkpoints,
                                            p2_spawn, p2_corner1, p2_corner2, p2_endPlate, p2_checkpoints);
            duelArenas.put(id, arena);
        }
        plugin.getLogger().info("Loaded " + duelArenas.size() + " duel arenas.");
    }

    public void deleteArena(String id, Player player) {
        FileConfiguration config = plugin.getConfigManager().getDuelsConfig();
        if (!config.isConfigurationSection("duel-arenas." + id)) {
            plugin.getMessageUtil().sendMessage(player, "error.map-not-found", "{map_id}", id);
            return;
        }
        if (!player.hasPermission("bridgeracing.admin")) {
            plugin.getMessageUtil().sendMessage(player, "error.no-permission");
            return;
        }
        config.set("duel-arenas." + id, null);
        plugin.getConfigManager().saveDuelsConfig();
        duelArenas.remove(id);
        plugin.getLogger().info("Deleted arena " + id + " by " + player.getName());
        plugin.getMessageUtil().sendMessage(player, "command.delete.success", "{id}", id);
    }

    private Location parseLocation(String locStr) {
        if (locStr == null || locStr.isEmpty()) return null;
        try {
            String[] parts = locStr.split(",");
            return new Location(
                plugin.getServer().getWorld(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]),
                Double.parseDouble(parts[3]),
                Float.parseFloat(parts[4]),
                Float.parseFloat(parts[5])
            );
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse location: " + locStr);
            return null;
        }
    }

    private List<Location> parseLocationList(List<String> locStrs) {
        List<Location> locations = new ArrayList<>();
        for (String locStr : locStrs) {
            Location loc = parseLocation(locStr);
            if (loc != null) locations.add(loc);
        }
        return locations;
    }

    public DuelArena getDuelArenaById(String id) {
        return duelArenas.get(id);
    }

    public Map<String, DuelArena> getDuelArenas() {
        return duelArenas;
    }
}