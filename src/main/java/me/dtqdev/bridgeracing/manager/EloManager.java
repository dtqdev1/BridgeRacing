package me.dtqdev.bridgeracing.manager;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.EloRank;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class EloManager {

    private final BridgeRacing plugin;
    private final Map<UUID, Integer> eloCache = new ConcurrentHashMap<>();
    private final List<EloRank> ranks = new ArrayList<>();
    private List<Map.Entry<UUID, Integer>> globalEloRanking = new ArrayList<>();

    private final int kFactor;
    private final int defaultElo;

    public EloManager(BridgeRacing plugin) {
        this.plugin = plugin;
        this.kFactor = plugin.getConfig().getInt("elo-k-factor", 32);
        this.defaultElo = plugin.getConfig().getInt("default-elo", 1000);
        loadRanks();
        loadAllPlayerData();
        startGlobalRankingUpdateTask();
    }

    public void loadRanks() {
        ranks.clear();
        ConfigurationSection ranksSection = plugin.getConfigManager().getRanksConfig().getConfigurationSection("ranks");
        if (ranksSection == null) return;

        for (String id : ranksSection.getKeys(false)) {
            int from = ranksSection.getInt(id + ".from");
            int to = ranksSection.getInt(id + ".to");
            String display = ranksSection.getString(id + ".display");
            ranks.add(new EloRank(id, from, to, display));
        }
        // Sắp xếp rank theo ELO để tìm kiếm nhanh hơn
        ranks.sort(Comparator.comparingInt(EloRank::getFromElo));
    }

    public void loadAllPlayerData() {
        eloCache.clear();
        ConfigurationSection playersSection = plugin.getConfigManager().getPlayerDataConfig().getConfigurationSection("players");
        if (playersSection == null) return;

        for (String uuidString : playersSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidString);
            int elo = playersSection.getInt(uuidString + ".elo");
            eloCache.put(uuid, elo);
        }
        updateGlobalRankingCache(); // Cập nhật cache xếp hạng ngay khi tải xong
    }

    public void saveAllPlayerData() {
        // Chạy async để không làm lag server
        new BukkitRunnable() {
            @Override
            public void run() {
                ConfigurationSection playersSection = plugin.getConfigManager().getPlayerDataConfig().createSection("players");
                for (Map.Entry<UUID, Integer> entry : eloCache.entrySet()) {
                    playersSection.set(entry.getKey().toString() + ".elo", entry.getValue());
                }
                plugin.getConfigManager().savePlayerData();
            }
        }.runTaskAsynchronously(plugin);
    }

    public int getElo(UUID uuid) {
        return eloCache.getOrDefault(uuid, defaultElo);
    }

    public EloRank getRank(int elo) {
        EloRank currentRank = null;
        for (EloRank rank : ranks) {
            if (elo >= rank.getFromElo() && elo <= rank.getToElo()) {
                currentRank = rank;
                break;
            }
        }
        return currentRank;
    }

    /**
     * Cập nhật ELO cho người thắng và người thua dựa trên công thức ELO.
     * @return Lượng ELO người thắng nhận được.
     */
    public int updateElo(UUID winner, UUID loser) {
        int winnerElo = getElo(winner);
        int loserElo = getElo(loser);

        // Công thức tính toán ELO
        double expectedScoreWinner = 1.0 / (1.0 + Math.pow(10.0, (double) (loserElo - winnerElo) / 400.0));
        int eloChange = (int) Math.round(kFactor * (1.0 - expectedScoreWinner));

        eloCache.put(winner, winnerElo + eloChange);
        eloCache.put(loser, Math.max(0, loserElo - eloChange)); // ELO không thể âm

        return eloChange;
    }
    
    // --- Xếp hạng Toàn server ---

    private void startGlobalRankingUpdateTask() {
        // Cập nhật 5 phút một lần
        new BukkitRunnable() {
            @Override
            public void run() {
                updateGlobalRankingCache();
            }
        }.runTaskTimerAsynchronously(plugin, 20L, 20L * 60 * 5);
    }

    private void updateGlobalRankingCache() {
        globalEloRanking = eloCache.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .collect(Collectors.toList());
    }
    
    public int getGlobalRank(UUID uuid) {
        for (int i = 0; i < globalEloRanking.size(); i++) {
            if (globalEloRanking.get(i).getKey().equals(uuid)) {
                return i + 1; // Rank bắt đầu từ 1
            }
        }
        return 0; // Không có rank
    }
}