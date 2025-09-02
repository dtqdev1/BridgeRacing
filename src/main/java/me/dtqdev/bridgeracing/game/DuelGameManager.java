package me.dtqdev.bridgeracing.game;
import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import me.dtqdev.bridgeracing.data.DuelPlayer;
import me.dtqdev.fastbuilder.Arena;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public class DuelGameManager {
    private final BridgeRacing plugin;
    private final Map<UUID, DuelGame> activeDuels = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("0.000");
    public DuelGameManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }
    public void createGame(Player p1, Player p2, DuelArena duelArena) {
        DuelPlayer duelP1 = savePlayerState(p1);
        DuelPlayer duelP2 = savePlayerState(p2);
        duelP1.setOpponentUUID(p2.getUniqueId());
        duelP2.setOpponentUUID(p1.getUniqueId());
        DuelGame game = new DuelGame(duelArena, duelP1, duelP2);
        activeDuels.put(p1.getUniqueId(), game);
        activeDuels.put(p2.getUniqueId(), game);
        preparePlayer(p1, duelArena.getP1_spawn());
        preparePlayer(p2, duelArena.getP2_spawn());
        startCountdown(game);
    }
    private DuelPlayer savePlayerState(Player player) {
        Location originalLocation = player.getLocation();
        String originalMode = null;
        Arena fbArena = plugin.getFastBuilderAPI().getArenaManager().getArenaByPlayer(player);
        if (fbArena != null) {
            originalMode = fbArena.getMode();
            fbArena.reset();
        }
        return new DuelPlayer(player.getUniqueId(), originalLocation, originalMode);
    }
    private void preparePlayer(Player player, Location spawn) {
        player.teleport(spawn);
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(1, new ItemStack(Material.SANDSTONE, 64));
        player.getInventory().setItem(2, new ItemStack(Material.DIAMOND_PICKAXE));
        player.updateInventory();
    }
    private void startCountdown(DuelGame game) {
        new BukkitRunnable() {
            int count = 5;
            @Override
            public void run() {
                if (game.getGameState() == DuelGame.GameState.ENDED) {
                    this.cancel();
                    return;
                }
                if (count > 0) {
                    game.sendTitleToBoth("&e" + count, "", 0, 25, 5);
                    game.playSoundToBoth(Sound.NOTE_STICKS, 1, 1);
                    count--;
                } else {
                    game.sendTitleToBoth("&a&lGO!", "", 0, 20, 10);
                    game.playSoundToBoth(Sound.NOTE_PLING, 1, 1.5f);
                    startGame(game);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
    private void startGame(DuelGame game) {
        game.startTimers();
        BukkitTask gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.getGameState() != DuelGame.GameState.RUNNING) {
                    this.cancel();
                    return;
                }
                game.updateProgress();
            }
        }.runTaskTimer(plugin, 0L, 1L);
        game.setGameTask(gameTask);
    }
    public void endGame(DuelGame game, Player winner, Player loser) {
        if (game.getGameState() == DuelGame.GameState.ENDED) return;
        game.stopTasks();
    
        double timeTaken = game.getElapsedTimeSeconds();
        String mapId = game.getArena().getId();
    
        // Xử lý kỷ lục cá nhân (PB)
        double oldBest = plugin.getDuelRecordManager().getBestTime(winner.getUniqueId(), mapId);
        plugin.getDuelRecordManager().setBestTime(winner.getUniqueId(), mapId, timeTaken);
        if (oldBest == -1 || timeTaken < oldBest) {
            winner.sendMessage(ChatColor.GOLD + "Bạn đã lập kỷ lục mới trên map " + mapId + ": " + df.format(timeTaken) + "s");
        }
    
        // --- LOGIC KIỂM TRA THĂNG HẠNG ---
        // 1. Lấy ELO và Rank CŨ của người thắng TRƯỚC khi cập nhật
        int oldElo = plugin.getEloManager().getElo(winner.getUniqueId());
        me.dtqdev.bridgeracing.data.EloRank oldRank = plugin.getEloManager().getRank(oldElo);
    
        // 2. Cập nhật ELO cho cả hai người chơi
        int eloChange = plugin.getEloManager().updateElo(winner.getUniqueId(), loser.getUniqueId());
        plugin.getEloManager().saveAllPlayerData();
    
        // 3. Lấy Rank MỚI của người thắng SAU khi cập nhật
        int newElo = plugin.getEloManager().getElo(winner.getUniqueId());
        me.dtqdev.bridgeracing.data.EloRank newRank = plugin.getEloManager().getRank(newElo);
    
        // 4. So sánh và gửi Title tương ứng
        boolean rankedUp = (oldRank != null && newRank != null && !oldRank.getId().equals(newRank.getId()) && newRank.getFromElo() > oldRank.getFromElo());
    
        if (rankedUp) {
            // Gửi title thăng hạng
            String rankUpTitle = plugin.getMessageUtil().getRawMessage("game-end.rank-up-title", "{new_rank}", newRank.getDisplayName());
            String rankUpSubtitle = plugin.getMessageUtil().getRawMessage("game-end.rank-up-subtitle", "{old_rank}", oldRank.getDisplayName(), "{new_rank}", newRank.getDisplayName());
            winner.sendTitle(
                ChatColor.translateAlternateColorCodes('&', rankUpTitle),
                ChatColor.translateAlternateColorCodes('&', rankUpSubtitle)
            );
            // Có thể thêm âm thanh đặc biệt ở đây
            winner.playSound(winner.getLocation(), Sound.ENDERDRAGON_DEATH, 0.8f, 1.2f);
        } else {
            // Gửi title chiến thắng thông thường
            winner.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&lWIN!"), ChatColor.translateAlternateColorCodes('&', "&e+" + eloChange + " ELO"));
            winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1, 1);
        }
    
        // Gửi title cho người thua
        loser.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&lLOSE!"), ChatColor.translateAlternateColorCodes('&', "&7-" + eloChange + " ELO"));
        loser.playSound(loser.getLocation(), Sound.VILLAGER_NO, 1, 1);
        
        // Bắn pháo hoa cho người thắng
        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getFastBuilderAPI().getFireworkManager().launchPbFirework(winner);
                }
            }.runTaskLater(plugin, i * 15L);
        }
    
        // Dọn dẹp trận đấu sau một khoảng thời gian
        new BukkitRunnable() {
            @Override
            public void run() {
                game.clearAllPlacedBlocks();
                teleportBack(winner, game.getDuelPlayer(winner.getUniqueId()));
                teleportBack(loser, game.getDuelPlayer(loser.getUniqueId()));
                activeDuels.remove(winner.getUniqueId());
                if (loser != null) {
                    activeDuels.remove(loser.getUniqueId());
                }
            }
        }.runTaskLater(plugin, 60L);
    }
    private void teleportBack(Player player, DuelPlayer duelPlayerData) {
        if (player == null || !player.isOnline() || duelPlayerData == null) return;
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
        String originalMode = duelPlayerData.getOriginalFastBuilderMode();
        if (originalMode != null) {
            Arena newArena = plugin.getFastBuilderAPI().getArenaManager().findAvailableArena(originalMode);
            if (newArena != null) {
                newArena.setPlayer(player);
                return;
            }
        }
        player.teleport(plugin.getFastBuilderAPI().getLobbyLocation());
    }
    public void endAllGames() {
        for (DuelGame game : activeDuels.values()) {
            if (game.getGameState() != DuelGame.GameState.ENDED) {
                game.stopTasks();
                Player p1 = Bukkit.getPlayer(game.getDuelPlayer1().getPlayerUUID());
                if (p1 != null) {
                    UUID opponentUUID = game.getDuelPlayer(p1.getUniqueId()).getOpponentUUID();
                    Player p2 = Bukkit.getPlayer(opponentUUID);
                    teleportBack(p1, game.getDuelPlayer(p1.getUniqueId()));
                    if (p2 != null) teleportBack(p2, game.getDuelPlayer(p2.getUniqueId()));
                }
            }
        }
        activeDuels.clear();
    }

    public DuelGame getDuelByPlayer(UUID uuid) {
        return activeDuels.get(uuid);
    }

    public void handlePlayerFall(Player player, DuelGame game) {
        DuelPlayer duelPlayer = game.getDuelPlayer(player.getUniqueId());
        if (duelPlayer == null) return;
        Location respawnLoc;
        Location spawn;
        List<Location> checkpoints;
        if (player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID())) {
            spawn = game.getArena().getP1_spawn();
            checkpoints = game.getArena().getP1_checkpoints();
        } else {
            spawn = game.getArena().getP2_spawn();
            checkpoints = game.getArena().getP2_checkpoints();
        }
        int checkpointIndex = duelPlayer.getLastCheckpointIndex();
        if (checkpointIndex == -1) {
            respawnLoc = spawn.clone();
        } else {
            Location cpBlock = checkpoints.get(checkpointIndex);
            respawnLoc = cpBlock.clone().add(0.5, 1, 0.5);
            respawnLoc.setYaw(spawn.getYaw());
            respawnLoc.setPitch(spawn.getPitch());
        }
        // Clear placed blocks by this player
        List<Block> playerBlocks = game.placedBlocks.get(player.getUniqueId());
        if (playerBlocks != null) {
            // Dùng BukkitRunnable để tránh lag khi xóa nhiều block
            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Block block : new ArrayList<>(playerBlocks)) {
                        if (block.getType() == Material.SANDSTONE) {
                            block.setType(Material.AIR);
                        }
                    }
                }
            }.runTask(plugin);
            playerBlocks.clear();
        }
        player.teleport(respawnLoc);
    }

    public void handlePlayerQuit(Player player, DuelGame game) {
        UUID opponentUUID = game.getDuelPlayer(player.getUniqueId()).getOpponentUUID();
        Player opponent = Bukkit.getPlayer(opponentUUID);
        if (opponent != null && opponent.isOnline()) {
            endGame(game, opponent, player);
        } else {
            game.stopTasks();
            activeDuels.remove(player.getUniqueId());
            if (opponent != null) activeDuels.remove(opponent.getUniqueId());
        }
    }
    // Thêm phương thức này vào file DuelGameManager.java
public int getPlayingCount(String mapId) {
    int count = 0;
    for (DuelGame game : activeDuels.values()) {
        if (game.getArena().getId().equalsIgnoreCase(mapId)) {
            count++;
        }
    }
    return count * 2; // Mỗi game có 2 người chơi
}
}