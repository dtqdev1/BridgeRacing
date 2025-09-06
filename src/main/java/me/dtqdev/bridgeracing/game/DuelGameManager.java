package me.dtqdev.bridgeracing.game;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.data.DataException;
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
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelGameManager {
    private final BridgeRacing plugin;
    private final Map<UUID, DuelGame> activeDuels = new ConcurrentHashMap<>();
    // MỚI: Quản lý các làn đang được sử dụng cho mỗi map
    private final Map<String, Set<Integer>> usedLaneIndices = new ConcurrentHashMap<>();
    private final DecimalFormat df = new DecimalFormat("0.000");
    public DuelGameManager(BridgeRacing plugin) {
        this.plugin = plugin;
    }
    // MỚI: Hàm tìm làn trống tiếp theo
    private int findNextAvailableLane(String mapId) {
        Set<Integer> usedIndices = usedLaneIndices.computeIfAbsent(mapId, k -> new HashSet<>());
        int laneIndex = 0;
        while (usedIndices.contains(laneIndex)) {
            laneIndex++;
        }
        return laneIndex;
    }
    // THAY ĐỔI: Logic tạo game hoàn toàn mới
    public void createGame(Player p1, Player p2, DuelArena duelArenaTemplate) {
        String mapId = duelArenaTemplate.getId();
        // 1. Tìm và đánh dấu làn trống
        int laneIndex = findNextAvailableLane(mapId);
        usedLaneIndices.get(mapId).add(laneIndex);
        // 2. Nếu không phải làn 0, dán schematic cho làn mới
        if (laneIndex > 0) {
            double offsetX = plugin.getConfig().getDouble("lane-offset-x", 100.0) * laneIndex;
            double offsetY = plugin.getConfig().getDouble("lane-offset-y", 0.0) * laneIndex;
            double offsetZ = plugin.getConfig().getDouble("lane-offset-z", 0.0) * laneIndex;
            Location p1_pasteLoc = duelArenaTemplate.getP1_spawn().clone().add(offsetX, offsetY, offsetZ);
            Location p2_pasteLoc = duelArenaTemplate.getP2_spawn().clone().add(offsetX, offsetY, offsetZ);
            try {
                // Giả sử schematicName được lưu đâu đó hoặc có thể suy ra từ mapId
                // Trong SetupCommand, schematicName là args[2]. Chúng ta cần cách truy cập nó.
                // Tạm thời, ta sẽ giả định id map chính là tên schematic.
                String schematicName = duelArenaTemplate.getSchematicName(); 
                plugin.getSchematicManager().paste(schematicName, p1_pasteLoc);
                plugin.getSchematicManager().paste(schematicName, p2_pasteLoc);
            } catch (MaxChangedBlocksException | DataException | IOException e) {
                p1.sendMessage(ChatColor.RED + "Lỗi nghiêm trọng: Không thể tạo đấu trường. Vui lòng báo admin.");
                p2.sendMessage(ChatColor.RED + "Lỗi nghiêm trọng: Không thể tạo đấu trường. Vui lòng báo admin.");
                plugin.getLogger().severe("Could not paste schematic for a new lane: " + e.getMessage());
                // Giải phóng làn nếu có lỗi
                usedLaneIndices.get(mapId).remove(laneIndex);
                return;
            }
        }
        // 3. Lưu trạng thái người chơi và tạo đối tượng game
        DuelPlayer duelP1 = savePlayerState(p1);
        DuelPlayer duelP2 = savePlayerState(p2);
        duelP1.setOpponentUUID(p2.getUniqueId());
        duelP2.setOpponentUUID(p1.getUniqueId());
        DuelGame game = new DuelGame(duelArenaTemplate, duelP1, duelP2, laneIndex);
        activeDuels.put(p1.getUniqueId(), game);
        activeDuels.put(p2.getUniqueId(), game);
        // 4. Chuẩn bị người chơi và bắt đầu đếm ngược
        preparePlayer(p1, game.getP1_spawn());
        preparePlayer(p2, game.getP2_spawn());
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
                    // Dịch chuyển lại lần nữa để đảm bảo đúng vị trí sau khi countdown
                    if (p1 != null) {
                        p1.teleport(game.getP1_spawn());
                    }
                    if (p2 != null) {
                        p2.teleport(game.getP2_spawn());
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
        String mapId = game.getArenaTemplate().getId();
        if (loser != null && loser.isOnline()) {
            double oldBest = plugin.getDuelRecordManager().getBestTime(winner.getUniqueId(), mapId);
            plugin.getDuelRecordManager().setBestTime(winner.getUniqueId(), mapId, timeTaken);
            if (oldBest == -1 || timeTaken < oldBest) {
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
                        sendNmsTitle(winner, rankUpTitle, "", 10, 60, 20);
                        sendEloBarAnimation(winner, oldRank, newRank);
                        winner.playSound(winner.getLocation(), Sound.ENDERDRAGON_DEATH, 0.8f, 1.2f);
                    }
                }
            }.runTaskLater(plugin, 40L);
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
                // THAY ĐỔI: Dọn dẹp đúng làn đã chơi
                clearLaneRegion(game.getP1_corner1(), game.getP1_corner2());
                clearLaneRegion(game.getP2_corner1(), game.getP2_corner2());
                // MỚI: Giải phóng làn đấu
                String mapId = game.getArenaTemplate().getId();
                int laneIndex = game.getLaneIndex();
                usedLaneIndices.get(mapId).remove(laneIndex);
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
        for (DuelGame game : new ArrayList<>(activeDuels.values())) { // Tránh ConcurrentModificationException
             if (game.getGameState() != DuelGame.GameState.ENDED) {
                game.stopTasks();
                Player p1 = Bukkit.getPlayer(game.getDuelPlayer1().getPlayerUUID());
                if (p1 != null) {
                    UUID opponentUUID = game.getDuelPlayer(p1.getUniqueId()).getOpponentUUID();
                    Player p2 = Bukkit.getPlayer(opponentUUID);
                    // Dọn dẹp làn
                    clearLaneRegion(game.getP1_corner1(), game.getP1_corner2());
                    clearLaneRegion(game.getP2_corner1(), game.getP2_corner2());
                    // Dịch chuyển về
                    teleportBack(p1, game.getDuelPlayer(p1.getUniqueId()));
                    if (p2 != null) teleportBack(p2, game.getDuelPlayer(p2.getUniqueId()));
                }
            }
        }
        activeDuels.clear();
        usedLaneIndices.clear();
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
        // THAY ĐỔI: Lấy vị trí từ game, không phải từ arena template
        if (player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID())) {
            spawn = game.getP1_spawn();
            checkpoints = game.getP1_checkpoints();
        } else {
            spawn = game.getP2_spawn();
            checkpoints = game.getP2_checkpoints();
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
        Location corner1, corner2;
        if (player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID())) {
            corner1 = game.getP1_corner1();
            corner2 = game.getP1_corner2();
        } else {
            corner1 = game.getP2_corner1();
            corner2 = game.getP2_corner2();
        }
        clearLaneRegion(corner1, corner2);
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
            // Dọn dẹp và giải phóng làn khi cả hai người chơi thoát
            clearLaneRegion(game.getP1_corner1(), game.getP1_corner2());
            clearLaneRegion(game.getP2_corner1(), game.getP2_corner2());
            String mapId = game.getArenaTemplate().getId();
            int laneIndex = game.getLaneIndex();
            if (usedLaneIndices.containsKey(mapId)) {
                usedLaneIndices.get(mapId).remove(laneIndex);
            }
            activeDuels.remove(player.getUniqueId());
            if (opponent != null) activeDuels.remove(opponent.getUniqueId());
        }
    }
    public int getPlayingCount(String mapId) {
        int count = 0;
        // Dùng Set để tránh đếm trùng game
        Set<DuelGame> gamesOnMap = new HashSet<>();
        for (DuelGame game : activeDuels.values()) {
            if (game.getArenaTemplate().getId().equalsIgnoreCase(mapId)) {
                gamesOnMap.add(game);
            }
        }
        return gamesOnMap.size() * 2;
    }
    // --- Các hàm còn lại không thay đổi ---
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