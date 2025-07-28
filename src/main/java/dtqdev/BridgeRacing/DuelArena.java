package dtqdev.BridgeRacing;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class DuelArena {
    private final String id;
    private final World world;
    private final Location spawn1;
    private final Location spawn2;
    private final Location goldPlate1;
    private final Location goldPlate2;
    private final Location corner1Player1;
    private final Location corner2Player1;
    private final Location corner1Player2;
    private final Location corner2Player2;
    private final List<Location> checkpointsPlayer1;
    private final List<Location> checkpointsPlayer2;
    private final String displayItem;
    private final String displayName;
    private final List<String> displayLore;
    private final List<Hologram> holograms;

    public DuelArena(String id, World world, Location spawn1, Location spawn2, Location goldPlate1, Location goldPlate2,
                     Location corner1Player1, Location corner2Player1, Location corner1Player2, Location corner2Player2,
                     List<Location> checkpointsPlayer1, List<Location> checkpointsPlayer2,
                     String displayItem, String displayName, List<String> displayLore) {
        this.id = id;
        this.world = world;
        this.spawn1 = spawn1;
        this.spawn2 = spawn2;
        this.goldPlate1 = goldPlate1;
        this.goldPlate2 = goldPlate2;
        this.corner1Player1 = corner1Player1;
        this.corner2Player1 = corner2Player1;
        this.corner1Player2 = corner1Player2;
        this.corner2Player2 = corner2Player2;
        this.checkpointsPlayer1 = checkpointsPlayer1;
        this.checkpointsPlayer2 = checkpointsPlayer2;
        this.displayItem = displayItem;
        this.displayName = displayName;
        this.displayLore = displayLore;
        this.holograms = new ArrayList<>();
    }

    public boolean isInBounds(Location location, boolean isPlayer1) {
        Location corner1 = isPlayer1 ? corner1Player1 : corner1Player2;
        Location corner2 = isPlayer1 ? corner2Player1 : corner2Player2;
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return location.getWorld().equals(world) &&
               location.getX() >= minX && location.getX() <= maxX &&
               location.getY() >= minY && location.getY() <= maxY &&
               location.getZ() >= minZ && location.getZ() <= maxZ;
    }

    public void createHolograms(BridgeRacing plugin) {
        if (!plugin.getFastBuilder().isHolographicDisplaysEnabled()) return;
        for (Location loc : checkpointsPlayer1) {
            Hologram hologram = HologramsAPI.createHologram(plugin, loc.clone().add(0, 1.5, 0));
            hologram.appendTextLine(ChatColor.YELLOW + "Checkpoint");
            holograms.add(hologram);
        }
        for (Location loc : checkpointsPlayer2) {
            Hologram hologram = HologramsAPI.createHologram(plugin, loc.clone().add(0, 1.5, 0));
            hologram.appendTextLine(ChatColor.YELLOW + "Checkpoint");
            holograms.add(hologram);
        }
    }

    public void updateHologram(Location checkpoint, boolean isPlayer1) {
        List<Location> checkpoints = isPlayer1 ? checkpointsPlayer1 : checkpointsPlayer2;
        int index = checkpoints.indexOf(checkpoint);
        if (index >= 0 && index < holograms.size()) {
            Hologram hologram = holograms.get(isPlayer1 ? index : index + checkpointsPlayer1.size());
            hologram.clearLines();
            hologram.appendTextLine(ChatColor.GREEN + "Checkpoint");
        }
    }

    public void clearHolograms() {
        for (Hologram hologram : holograms) {
            hologram.delete();
        }
        holograms.clear();
    }

    // Getters
    public String getId() { return id; }
    public World getWorld() { return world; }
    public Location getSpawn1() { return spawn1; }
    public Location getSpawn2() { return spawn2; }
    public Location getGoldPlate1() { return goldPlate1; }
    public Location getGoldPlate2() { return goldPlate2; }
    public List<Location> getCheckpointsPlayer1() { return checkpointsPlayer1; }
    public List<Location> getCheckpointsPlayer2() { return checkpointsPlayer2; }
    public String getDisplayItem() { return displayItem; }
    public String getDisplayName() { return displayName; }
    public List<String> getDisplayLore() { return displayLore; }
}