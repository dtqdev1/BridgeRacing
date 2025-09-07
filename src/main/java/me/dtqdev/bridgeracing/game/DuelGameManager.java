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
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutTitle;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
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
                    
                    Player p1 = Bukkit.getPlayer(game.getDuelPlayer1().getPlayerUUID());
                    Player p2 = Bukkit.getPlayer(game.getDuelPlayer(p1.getUniqueId()).getOpponentUUID());

                    if (p1 != null) {
                        p1.teleport(game.getArena().getP1_spawn());
                    }
                    if (p2 != null) {
                        p2.teleport(game.getArena().getP2_spawn());
                    }
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
        
        // Chỉ tính PB nếu người thua không thoát game
        if (loser != null && loser.isOnline()) {
            boolean isNewRecord = plugin.getDuelRecordManager().setBestTime(winner.getUniqueId(), mapId, timeTaken);
            if (isNewRecord) {
                // Tin nhắn này bây giờ chỉ gửi khi setBestTime trả về true
                winner.sendMessage(ChatColor.GOLD + "Bạn đã lập kỷ lục mới trên map " + mapId + ": " + df.format(timeTaken) + "s");
            }
        }
    
        int oldElo = plugin.getEloManager().getElo(winner.getUniqueId());
        me.dtqdev.bridgeracing.data.EloRank oldRank = plugin.getEloManager().getRank(oldElo);
        int eloChange = plugin.getEloManager().updateElo(winner.getUniqueId(), loser.getUniqueId());
        plugin.getEloManager().saveAllPlayerData();
        int newElo = plugin.getEloManager().getElo(winner.getUniqueId());
        me.dtqdev.bridgeracing.data.EloRank newRank = plugin.getEloManager().getRank(newElo);
        boolean rankedUp = (oldRank != null && newRank != null && !oldRank.getId().equals(newRank.getId()) && newRank.getFromElo() > oldRank.getFromElo());
    
        // Gửi title Win/Lose
        sendNmsTitle(winner, "&a&lWIN!", "&e+" + eloChange + " ELO", 10, 40, 10);
        winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1, 1);
        sendNmsTitle(loser, "&c&lLOSE!", "&7-" + eloChange + " ELO", 10, 40, 10);
        loser.playSound(loser.getLocation(), Sound.VILLAGER_NO, 1, 1);
    
        if (rankedUp) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (winner != null && winner.isOnline()) {
                        String rankUpTitle = plugin.getMessageUtil().getRawMessage("game-end.rank-up-title", "{new_rank}", newRank.getDisplayName());
                        sendNmsTitle(winner, rankUpTitle, "", 10, 60, 20); // Gửi title chính trước
                        sendEloBarAnimation(winner, oldRank, newRank); // Bắt đầu animation subtitle
                        winner.playSound(winner.getLocation(), Sound.ENDERDRAGON_DEATH, 0.8f, 1.2f);
                    }
                }
            }.runTaskLater(plugin, 40L); // 2 giây sau
        }
    
        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getFastBuilderAPI().getFireworkManager().launchPbFirework(winner);
                }
            }.runTaskLater(plugin, i * 15L);
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                // --- THAY THẾ game.clearAllPlacedBlocks() BẰNG LOGIC MỚI ---
                clearLaneRegion(game.getArena().getP1_corner1(), game.getArena().getP1_corner2());
                clearLaneRegion(game.getArena().getP2_corner1(), game.getArena().getP2_corner2());
                
                teleportBack(winner, game.getDuelPlayer(winner.getUniqueId()));
                teleportBack(loser, game.getDuelPlayer(loser.getUniqueId()));
                activeDuels.remove(winner.getUniqueId());
                if (loser != null) {
                    activeDuels.remove(loser.getUniqueId());
                }
            }
        }.runTaskLater(plugin, 80L);
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

        // --- THAY THẾ LOGIC XÓA BLOCK CŨ BẰNG LOGIC MỚI ---
        Location corner1, corner2;
        if (player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID())) {
            corner1 = game.getArena().getP1_corner1();
            corner2 = game.getArena().getP1_corner2();
        } else {
            corner1 = game.getArena().getP2_corner1();
            corner2 = game.getArena().getP2_corner2();
        }
        clearLaneRegion(corner1, corner2);
        
        // Xóa luôn danh sách cũ để đồng bộ
        List<Block> playerBlocks = game.placedBlocks.get(player.getUniqueId());
        if (playerBlocks != null) {
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
private void sendEloBarAnimation(Player player, me.dtqdev.bridgeracing.data.EloRank oldRank, me.dtqdev.bridgeracing.data.EloRank newRank) {
    String oldRankDisplay = oldRank.getDisplayName();
    String newRankDisplay = newRank.getDisplayName();
    new BukkitRunnable() {
        int progress = 0;
        final int totalSteps = 20;
        @Override
        public void run() {
            if (progress > totalSteps || player == null || !player.isOnline()) {
                if (player != null && player.isOnline()) {
                    String finalSubtitle = plugin.getMessageUtil().getRawMessage("game-end.rank-up-subtitle", "{old_rank}", oldRankDisplay, "{new_rank}", newRankDisplay);
                    sendNmsSubtitle(player, finalSubtitle);
                }
                this.cancel();
                return;
            }
            StringBuilder bar = new StringBuilder("&7[");
            int filledChars = (int) Math.floor(((double) progress / totalSteps) * 10);
            bar.append("&b");
            for (int i = 0; i < filledChars; i++) bar.append("=");
            bar.append("&7");
            for (int i = 0; i < 10 - filledChars; i++) bar.append("-");
            bar.append("&7]");
            String subtitle = oldRankDisplay + " &f" + bar.toString() + " &f" + newRankDisplay;
            sendNmsSubtitle(player, subtitle);
            progress++;
        }
    }.runTaskTimer(plugin, 0L, 1L);
}
private void sendNmsTitle(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
    if (player == null || !player.isOnline()) return;
    title = ChatColor.translateAlternateColorCodes('&', title);
    subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);

    IChatBaseComponent titleComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + title + "\"}");
    IChatBaseComponent subtitleComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitle + "\"}");

    PacketPlayOutTitle timesPacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TIMES, null, fadeIn, stay, fadeOut);
    PacketPlayOutTitle titlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.TITLE, titleComponent);
    PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleComponent);

    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(timesPacket);
    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitlePacket);
    ((CraftPlayer) player).getHandle().playerConnection.sendPacket(titlePacket);
}

private void sendNmsSubtitle(Player player, String subtitle) {
     if (player == null || !player.isOnline()) return;
     subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
     IChatBaseComponent subtitleComponent = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + subtitle + "\"}");
     PacketPlayOutTitle subtitlePacket = new PacketPlayOutTitle(PacketPlayOutTitle.EnumTitleAction.SUBTITLE, subtitleComponent);
     ((CraftPlayer) player).getHandle().playerConnection.sendPacket(subtitlePacket);
}
    private void clearLaneRegion(Location corner1, Location corner2) {
        if (corner1 == null || corner2 == null) return;
        
        World world = corner1.getWorld();
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        new BukkitRunnable() {
            @Override
            public void run() {
                for (int x = minX; x <= maxX; x++) {
                    for (int y = minY; y <= maxY; y++) {
                        for (int z = minZ; z <= maxZ; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (block.getType() == Material.SANDSTONE) {
                                block.setType(Material.AIR);
                            }
                        }
                    }
                }
            }
        }.runTask(plugin);
    }
}