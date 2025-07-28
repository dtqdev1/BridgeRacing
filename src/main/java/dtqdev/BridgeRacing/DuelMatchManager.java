package dtqdev.BridgeRacing;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;

public class DuelMatchManager {
    private final BridgeRacing plugin;
    private final DuelArenaManager arenaManager;
    private final Map<String, Player> queue;
    private final Map<Player, DuelMatch> activeMatches;

    public DuelMatchManager(BridgeRacing plugin, DuelArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
        this.queue = new HashMap<>();
        this.activeMatches = new HashMap<>();
        startQueueTask();
    }

    public void addToQueue(Player player, String arenaId) {
        if (queue.containsValue(player) || activeMatches.containsKey(player)) return;
        queue.put(arenaId, player);
        player.sendMessage(ChatColor.GREEN + "Đã vào hàng chờ!");
        plugin.getGuiManager().updatePlayerItem(player, true);
    }

    public void removeFromQueue(Player player) {
        queue.entrySet().removeIf(entry -> entry.getValue().equals(player));
        if (player.isOnline()) {
            player.sendMessage(ChatColor.RED + "Đã thoát hàng chờ");
            plugin.getGuiManager().updatePlayerItem(player, false);
        }
    }

    public void startMatch(String arenaId, Player player1, Player player2) {
        DuelArena arena = arenaManager.getArenaById(arenaId);
        if (arena == null) return;
        DuelMatch match = new DuelMatch(plugin, arena, player1, player2);
        activeMatches.put(player1, match);
        activeMatches.put(player2, match);
        queue.remove(arenaId);
        match.start();
    }

    public void endMatch(Player player, Player winner, boolean normalEnd) {
        DuelMatch match = activeMatches.get(player);
        if (match == null) return;
        match.end(winner, normalEnd);
        activeMatches.remove(match.getPlayer1());
        activeMatches.remove(match.getPlayer2());
    }

    public void endAllMatches() {
        for (DuelMatch match : new HashSet<>(activeMatches.values())) {
            match.end(null, false);
        }
        activeMatches.clear();
        queue.clear();
    }

    private void startQueueTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (String arenaId : new HashSet<>(queue.keySet())) {
                    Player player1 = queue.get(arenaId);
                    if (player1 == null || !player1.isOnline()) {
                        queue.remove(arenaId);
                        continue;
                    }
                    for (Player player2 : Bukkit.getOnlinePlayers()) {
                        if (player2.equals(player1) || activeMatches.containsKey(player2) || queue.containsValue(player2)) continue;
                        if (plugin.getFastBuilder().getArenaManager().getArenaByPlayer(player2) != null) {
                            startMatch(arenaId, player1, player2);
                            break;
                        }
                    }
                    if (queue.containsKey(arenaId)) {
                        plugin.getFastBuilder().getTitleManager().sendCompletionTitle(player1, 0, 0); // Placeholder for ActionBar
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public DuelMatch getMatch(Player player) {
        return activeMatches.get(player);
    }

    public int getQueueSize(String arenaId) {
        return queue.containsKey(arenaId) ? 1 : 0;
    }
}