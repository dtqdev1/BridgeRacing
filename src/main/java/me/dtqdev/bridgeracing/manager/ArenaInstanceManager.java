package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArenaInstanceManager {
    private final BridgeRacing plugin;
    private final SchematicManager schematicManager;
    private final Map<String, List<DuelArena>> arenaInstances; // MapId -> List of instances
    private final Map<String, Integer> nextInstanceOffset; // MapId -> Next offset for new instance

    public ArenaInstanceManager(BridgeRacing plugin) {
        this.plugin = plugin;
        this.schematicManager = plugin.getSchematicManager();
        this.arenaInstances = new HashMap<>();
        this.nextInstanceOffset = new HashMap<>();
    }

    public DuelArena getAvailableArena(String mapId) {
        List<DuelArena> instances = arenaInstances.computeIfAbsent(mapId, k -> new ArrayList<>());
        
        // Kiểm tra xem có instance nào đang trống không
        for (DuelArena instance : instances) {
            int playingCount = plugin.getDuelGameManager().getPlayingCount(instance.getId());
            if (playingCount < 2) { // Nếu có ít hơn 2 người đang chơi
                return instance;
            }
        }

        // Nếu không có instance nào trống, tạo mới
        return createNewInstance(mapId);
    }

    private DuelArena createNewInstance(String mapId) {
        DuelArena originalArena = plugin.getDuelArenaManager().getArena(mapId);
        if (originalArena == null) return null;

        int offset = nextInstanceOffset.getOrDefault(mapId, 1) * 100; // Mỗi instance cách nhau 100 block
        nextInstanceOffset.put(mapId, nextInstanceOffset.getOrDefault(mapId, 1) + 1);

        // Clone arena với offset
        DuelArena newInstance = cloneArenaWithOffset(originalArena, offset);
        
        // Paste schematic tại vị trí mới
        Location pasteLocation = newInstance.getP1_spawn().clone();
        pasteLocation.setY(pasteLocation.getY() - 1); // Điều chỉnh vị trí paste nếu cần
        schematicManager.pasteSchematic(mapId, pasteLocation);

        // Thêm instance mới vào danh sách
        arenaInstances.computeIfAbsent(mapId, k -> new ArrayList<>()).add(newInstance);
        
        return newInstance;
    }

    private DuelArena cloneArenaWithOffset(DuelArena original, int offset) {
        String newId = original.getId() + "_" + nextInstanceOffset.getOrDefault(original.getId(), 1);
        
        // Clone tất cả các location với offset
        Location p1_spawn = offsetLocation(original.getP1_spawn(), offset);
        Location p1_corner1 = offsetLocation(original.getP1_corner1(), offset);
        Location p1_corner2 = offsetLocation(original.getP1_corner2(), offset);
        Location p1_endPlate = offsetLocation(original.getP1_endPlate(), offset);
        List<Location> p1_checkpoints = offsetLocationList(original.getP1_checkpoints(), offset);

        Location p2_spawn = offsetLocation(original.getP2_spawn(), offset);
        Location p2_corner1 = offsetLocation(original.getP2_corner1(), offset);
        Location p2_corner2 = offsetLocation(original.getP2_corner2(), offset);
        Location p2_endPlate = offsetLocation(original.getP2_endPlate(), offset);
        List<Location> p2_checkpoints = offsetLocationList(original.getP2_checkpoints(), offset);

        return new DuelArena(
            newId,
            original.getDisplayName(),
            original.getGuiItemMaterial(),
            p1_spawn, p1_corner1, p1_corner2, p1_endPlate, p1_checkpoints,
            p2_spawn, p2_corner1, p2_corner2, p2_endPlate, p2_checkpoints
        );
    }

    private Location offsetLocation(Location original, int offset) {
        if (original == null) return null;
        return original.clone().add(offset, 0, 0);
    }

    private List<Location> offsetLocationList(List<Location> locations, int offset) {
        if (locations == null) return new ArrayList<>();
        List<Location> newLocations = new ArrayList<>();
        for (Location loc : locations) {
            newLocations.add(offsetLocation(loc, offset));
        }
        return newLocations;
    }

    public void clearUnusedInstances() {
        for (Map.Entry<String, List<DuelArena>> entry : arenaInstances.entrySet()) {
            List<DuelArena> instances = entry.getValue();
            instances.removeIf(instance -> {
                int playingCount = plugin.getDuelGameManager().getPlayingCount(instance.getId());
                return playingCount == 0;
            });
        }
    }
}
