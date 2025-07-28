package dtqdev.BridgeRacing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DuelArenaManager {
    private final BridgeRacing plugin;
    private final PvPConfigManager configManager;
    private final List<DuelArena> arenas;

    public DuelArenaManager(BridgeRacing plugin, PvPConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.arenas = new ArrayList<>();
    }

    public void loadArenas() {
        arenas.clear();
        configManager.getConfig().getConfigurationSection("duels").getKeys(false).stream()
                .limit(7) // Tối đa 7 map
                .forEach(id -> {
                    String path = "duels." + id;
                    World world = Bukkit.getWorld(configManager.getConfig().getString(path + ".world"));
                    if (world == null) {
                        plugin.getLogger().warning("World not found for arena " + id);
                        return;
                    }
                    Location spawn1 = parseLocation(world, configManager.getConfig().getString(path + ".spawn1"));
                    Location spawn2 = parseLocation(world, configManager.getConfig().getString(path + ".spawn2"));
                    Location goldPlate1 = parseLocation(world, configManager.getConfig().getString(path + ".goldPlate1"));
                    Location goldPlate2 = parseLocation(world, configManager.getConfig().getString(path + ".goldPlate2"));
                    Location corner1Player1 = parseLocation(world, configManager.getConfig().getString(path + ".corner1_player1"));
                    Location corner2Player1 = parseLocation(world, configManager.getConfig().getString(path + ".corner2_player1"));
                    Location corner1Player2 = parseLocation(world, configManager.getConfig().getString(path + ".corner1_player2"));
                    Location corner2Player2 = parseLocation(world, configManager.getConfig().getString(path + ".corner2_player2"));
                    List<String> checkpoints1 = configManager.getConfig().getStringList(path + ".checkpoints_player1");
                    List<String> checkpoints2 = configManager.getConfig().getStringList(path + ".checkpoints_player2");
                    List<Location> checkpointsPlayer1 = checkpoints1.stream().map(loc -> parseLocation(world, loc)).collect(Collectors.toList());
                    List<Location> checkpointsPlayer2 = checkpoints2.stream().map(loc -> parseLocation(world, loc)).collect(Collectors.toList());
                    String displayItem = configManager.getConfig().getString(path + ".displayItem", "STONE");
                    String displayName = configManager.getConfig().getString(path + ".displayName", id);
                    List<String> displayLore = configManager.getConfig().getStringList(path + ".displayLore");
                    if (spawn1 != null && spawn2 != null && goldPlate1 != null && goldPlate2 != null &&
                        corner1Player1 != null && corner2Player1 != null && corner1Player2 != null && corner2Player2 != null) {
                        arenas.add(new DuelArena(id, world, spawn1, spawn2, goldPlate1, goldPlate2,
                                corner1Player1, corner2Player1, corner1Player2, corner2Player2,
                                checkpointsPlayer1, checkpointsPlayer2, displayItem, displayName, displayLore));
                    }
                });
        plugin.getLogger().info("Loaded " + arenas.size() + " duel arenas.");
    }

    public void initializeHolograms() {
        for (DuelArena arena : arenas) {
            arena.createHolograms(plugin);
        }
    }

    public void clearHolograms() {
        for (DuelArena arena : arenas) {
            arena.clearHolograms();
        }
    }

    private Location parseLocation(World world, String locString) {
        if (locString == null) return null;
        String[] parts = locString.split(",");
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            float yaw = parts.length > 3 ? Float.parseFloat(parts[3]) : 0;
            float pitch = parts.length > 4 ? Float.parseFloat(parts[4]) : 0;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to parse location: " + locString);
            return null;
        }
    }

    public DuelArena getArenaById(String id) {
        return arenas.stream().filter(arena -> arena.getId().equals(id)).findFirst().orElse(null);
    }

    public List<DuelArena> getArenas() {
        return arenas;
    }
}