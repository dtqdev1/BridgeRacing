package me.dtqdev.bridgeracing.data;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.UUID;

public class DuelArena {
    private final String id;
    private final String displayName;
    private final String guiItemMaterial;
    private final Location p1_spawn;
    private final Location p1_corner1;
    private final Location p1_corner2;
    private final Location p1_endPlate;
    private final List<Location> p1_checkpoints;
    private final Location p2_spawn;
    private final Location p2_corner1;
    private final Location p2_corner2;
    private final Location p2_endPlate;
    private final List<Location> p2_checkpoints;
    public DuelArena(String id, String displayName, String guiItemMaterial,
                     Location p1_spawn, Location p1_corner1, Location p1_corner2, Location p1_endPlate, List<Location> p1_checkpoints,
                     Location p2_spawn, Location p2_corner1, Location p2_corner2, Location p2_endPlate, List<Location> p2_checkpoints) {
        this.id = id;
        this.displayName = displayName;
        this.guiItemMaterial = guiItemMaterial;
        this.p1_spawn = p1_spawn;
        this.p1_corner1 = p1_corner1;
        this.p1_corner2 = p1_corner2;
        this.p1_endPlate = p1_endPlate;
        this.p1_checkpoints = p1_checkpoints;
        this.p2_spawn = p2_spawn;
        this.p2_corner1 = p2_corner1;
        this.p2_corner2 = p2_corner2;
        this.p2_endPlate = p2_endPlate;
        this.p2_checkpoints = p2_checkpoints;
    }
    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getGuiItemMaterial() { return guiItemMaterial; }
    public Location getP1_spawn() { return p1_spawn; }
    public List<Location> getP1_checkpoints() { return p1_checkpoints; }
    public Location getP1_endPlate() { return p1_endPlate; }
    public Location getP2_spawn() { return p2_spawn; }
    public List<Location> getP2_checkpoints() { return p2_checkpoints; }
    public boolean isInBounds(Player player, UUID uuid1) {
        Location loc = player.getLocation();
        Location corner1, corner2;
        if (player.getUniqueId().equals(uuid1)) {
            corner1 = p1_corner1;
            corner2 = p1_corner2;
        } else {
            corner1 = p2_corner1;
            corner2 = p2_corner2;
        }
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        return loc.getX() >= minX && loc.getX() <= maxX &&
               loc.getY() >= minY && loc.getY() <= maxY &&
               loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }
    public Location getP2_endPlate() {
        return p2_endPlate;
    }
}