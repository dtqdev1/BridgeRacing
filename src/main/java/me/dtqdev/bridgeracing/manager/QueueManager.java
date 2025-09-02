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
            plugin.getMessageUtil().sendMessage(player, "error.in-game");
            return;
        }
        removePlayerFromAllQueues(player);
        DuelArena arena = plugin.getDuelArenaManager().getDuelArenaById(mapId);
        if (arena == null) {
            plugin.getMessageUtil().sendMessage(player, "error.map-not-found");
            return;
        }
        List<PlayerInQueue> queue = queues.computeIfAbsent(mapId, k -> Collections.synchronizedList(new ArrayList<>()));
        queue.add(new PlayerInQueue(player.getUniqueId()));
        plugin.getMessageUtil().sendMessage(player, "queue.join");
        plugin.getGuiManager().openMapSelector(player);
    }

    public void removePlayerFromAllQueues(Player player) {
        for (List<PlayerInQueue> queue : queues.values()) {
            queue.removeIf(p -> p.getUuid().equals(player.getUniqueId()));
        }
        plugin.getMessageUtil().sendMessage(player, "queue.leave");
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
                                    // --- THÊM MỚI BẮT ĐẦU ---
                                    DuelArena arena = plugin.getDuelArenaManager().getDuelArenaById(finalMapId);
                                    if (arena == null) return;

                                    // Gửi thông báo cho người chơi 1
                                    int p2_elo = eloManager.getElo(p2.getUniqueId());
                                    me.dtqdev.bridgeracing.data.EloRank p2_rank = eloManager.getRank(p2_elo);
                                    String p2_rank_display = p2_rank != null ? p2_rank.getDisplayName() : "&7N/A";
                                    for (String line : plugin.getMessageUtil().getMessageList("queue.match-found",
                                            "{opponent_name}", p2.getName(), // Khớp với {opponent_name}
                                            "{opponent_rank}", p2_rank_display, // Khớp với {opponent_rank}
                                            "{opponent_elo}", String.valueOf(p2_elo), // Khớp với {opponent_elo}
                                            "{map_name}", arena.getDisplayName())) {
                                        p1.sendMessage(line);
                                    }
                                    p1.playSound(p1.getLocation(), org.bukkit.Sound.ORB_PICKUP, 1, 1.2f);


                                    // Gửi thông báo cho người chơi 2
                                    int p1_elo = eloManager.getElo(p1.getUniqueId());
                                    me.dtqdev.bridgeracing.data.EloRank p1_rank = eloManager.getRank(p1_elo);
                                    String p1_rank_display = p1_rank != null ? p1_rank.getDisplayName() : "&7N/A";
                                    for (String line : plugin.getMessageUtil().getMessageList("queue.match-found",
                                            "{opponent_name}", p1.getName(),
                                            "{opponent_rank}", p1_rank_display,
                                            "{opponent_elo}", String.valueOf(p1_elo),
                                            "{map_name}", arena.getDisplayName())) {
                                        p2.sendMessage(line);
                                    }
                                    p2.playSound(p2.getLocation(), org.bukkit.Sound.ORB_PICKUP, 1, 1.2f);
                                    // --- THÊM MỚI KẾT THÚC ---

                                    plugin.getDuelGameManager().createGame(p1, p2, arena);
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