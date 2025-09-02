package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import me.dtqdev.bridgeracing.data.PlayerInQueue;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {
    private final BridgeRacing plugin;
    private final Map<String, List<PlayerInQueue>> queues = new ConcurrentHashMap<>();

    public QueueManager(BridgeRacing plugin) {
        this.plugin = plugin;
        startMatchmakingTask();
    }

    public void addPlayerToQueue(Player player, String mapId) {
        if (plugin.getDuelGameManager().getDuelByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "Bạn đang trong trận đấu, không thể tham gia hàng chờ!");
            return;
        }
        removePlayerFromAllQueues(player);
        DuelArena arena = plugin.getDuelArenaManager().getDuelArenaById(mapId);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Map không hợp lệ!");
            return;
        }
        List<PlayerInQueue> queue = queues.computeIfAbsent(mapId, k -> Collections.synchronizedList(new ArrayList<>()));
        queue.add(new PlayerInQueue(player.getUniqueId()));
        player.sendMessage(ChatColor.GREEN + "Bạn đã tham gia hàng chờ cho map " + arena.getDisplayName() + ".");
        plugin.getGuiManager().openMapSelector(player);
    }

    public void removePlayerFromAllQueues(Player player) {
        for (List<PlayerInQueue> queue : queues.values()) {
            queue.removeIf(p -> p.getUuid().equals(player.getUniqueId()));
        }
        player.sendMessage(ChatColor.YELLOW + "Bạn đã rời khỏi hàng chờ.");
        plugin.getGuiManager().openMapSelector(player);
    }

    private void startMatchmakingTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                tryMatchmaking();
            }
        }.runTaskTimerAsynchronously(plugin, 40L, 40L);
    }

    private void tryMatchmaking() {
        EloManager eloManager = plugin.getEloManager();
        for (Map.Entry<String, List<PlayerInQueue>> entry : queues.entrySet()) {
            String mapId = entry.getKey();
            List<PlayerInQueue> queue = entry.getValue();
            synchronized (queue) {
                while (queue.size() >= 2) {
                    queue.sort(Comparator.comparingLong(PlayerInQueue::getJoinTime));
                    PlayerInQueue p1_data = queue.get(0);
                    PlayerInQueue p2_data = null;
                    long timeWaited = System.currentTimeMillis() - p1_data.getJoinTime();
                if (timeWaited < 60000) {
                    int p1_elo = eloManager.getElo(p1_data.getUuid());
                    int bestMatchIndex = -1;
                    int smallestEloDiff = Integer.MAX_VALUE;
                    for (int i = 1; i < queue.size(); i++) {
                        PlayerInQueue potentialOpponent = queue.get(i);
                        int opponentElo = eloManager.getElo(potentialOpponent.getUuid());
                        int diff = Math.abs(p1_elo - opponentElo);
                        if (diff < smallestEloDiff) {
                            smallestEloDiff = diff;
                            bestMatchIndex = i;
                        }
                    }
                    if (bestMatchIndex != -1) {
                        p2_data = queue.get(bestMatchIndex);
                    }
                } else {
                    p2_data = queue.get(1);
                }
                    if (p2_data != null) {
                        queue.remove(p2_data);
                        queue.remove(p1_data);
                        final PlayerInQueue finalP1Data = p1_data;
                        final PlayerInQueue finalP2Data = p2_data;
                        final String finalMapId = mapId;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                Player p1 = Bukkit.getPlayer(finalP1Data.getUuid());
                                Player p2 = Bukkit.getPlayer(finalP2Data.getUuid());
                                if (p1 != null && p1.isOnline() && p2 != null && p2.isOnline()) {
                                    plugin.getDuelGameManager().createGame(p1, p2, plugin.getDuelArenaManager().getDuelArenaById(finalMapId));
                                }
                            }
                        }.runTask(plugin);
                    } else {
                        break;
                    }
                }
            }
        }
    }

    public int getQueueSize(String mapId) {
        return queues.getOrDefault(mapId, Collections.emptyList()).size();
    }

    public boolean isPlayerInQueue(Player player) {
        for (List<PlayerInQueue> queue : queues.values()) {
            if (queue.stream().anyMatch(p -> p.getUuid().equals(player.getUniqueId()))) {
                return true;
            }
        }
        return false;
    }
    // Thêm 2 phương thức này vào file QueueManager.java
public String getQueueMapId(Player player) {
    for (Map.Entry<String, List<PlayerInQueue>> entry : queues.entrySet()) {
        for (PlayerInQueue p : entry.getValue()) {
            if (p.getUuid().equals(player.getUniqueId())) {
                return entry.getKey();
            }
        }
    }
    return null; // Trả về null nếu không tìm thấy
}

public long getTimeInQueue(Player player) {
    for (List<PlayerInQueue> queue : queues.values()) {
        for (PlayerInQueue p : queue) {
            if (p.getUuid().equals(player.getUniqueId())) {
                return System.currentTimeMillis() - p.getJoinTime();
            }
        }
    }
    return 0; // Trả về 0 nếu không trong hàng chờ
}
}