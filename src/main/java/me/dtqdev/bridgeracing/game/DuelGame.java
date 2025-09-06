package me.dtqdev.bridgeracing.game;
import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import me.dtqdev.bridgeracing.data.DuelPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DuelGame {
    private final BridgeRacing plugin = BridgeRacing.getInstance();
    private final DuelArena arenaTemplate; // Đổi tên để rõ ràng đây là template
    private final int laneIndex; // MỚI: Chỉ số của làn đấu này
    private final DuelPlayer player1;
    private final DuelPlayer player2;
    // MỚI: Lưu trữ vị trí cụ thể của game này sau khi đã tính offset
    private final Location p1_spawn, p2_spawn, p1_endPlate, p2_endPlate, p1_corner1, p1_corner2, p2_corner1, p2_corner2;
    private final List<Location> p1_checkpoints, p2_checkpoints;
    public final Map<UUID, List<Block>> placedBlocks = new ConcurrentHashMap<>();
    private long startTime;
    private BukkitTask gameTask;
    private BukkitTask refillTask;
    private GameState gameState = GameState.COUNTDOWN;
    public enum GameState {
        COUNTDOWN, RUNNING, ENDED
    }
    // THAY ĐỔI: Constructor nhận laneIndex và tự tính toán vị trí
    public DuelGame(DuelArena arenaTemplate, DuelPlayer player1, DuelPlayer player2, int laneIndex) {
        this.arenaTemplate = arenaTemplate;
        this.player1 = player1;
        this.player2 = player2;
        this.laneIndex = laneIndex;
        this.placedBlocks.put(player1.getPlayerUUID(), new ArrayList<>());
        this.placedBlocks.put(player2.getPlayerUUID(), new ArrayList<>());
        // Tính toán offset dựa trên laneIndex
        double offsetX = plugin.getConfig().getDouble("lane-offset-x", 100.0) * laneIndex;
        double offsetY = plugin.getConfig().getDouble("lane-offset-y", 0.0) * laneIndex;
        double offsetZ = plugin.getConfig().getDouble("lane-offset-z", 0.0) * laneIndex;
        // Áp dụng offset để tạo ra vị trí mới cho game này
        this.p1_spawn = arenaTemplate.getP1_spawn().clone().add(offsetX, offsetY, offsetZ);
        this.p1_corner1 = arenaTemplate.getP1_corner1().clone().add(offsetX, offsetY, offsetZ);
        this.p1_corner2 = arenaTemplate.getP1_corner2().clone().add(offsetX, offsetY, offsetZ);
        this.p1_endPlate = arenaTemplate.getP1_endPlate().clone().add(offsetX, offsetY, offsetZ);
        this.p1_checkpoints = arenaTemplate.getP1_checkpoints().stream()
                .map(loc -> loc.clone().add(offsetX, offsetY, offsetZ))
                .collect(Collectors.toList());
        this.p2_spawn = arenaTemplate.getP2_spawn().clone().add(offsetX, offsetY, offsetZ);
        this.p2_corner1 = arenaTemplate.getP2_corner1().clone().add(offsetX, offsetY, offsetZ);
        this.p2_corner2 = arenaTemplate.getP2_corner2().clone().add(offsetX, offsetY, offsetZ);
        this.p2_endPlate = arenaTemplate.getP2_endPlate().clone().add(offsetX, offsetY, offsetZ);
        this.p2_checkpoints = arenaTemplate.getP2_checkpoints().stream()
                .map(loc -> loc.clone().add(offsetX, offsetY, offsetZ))
                .collect(Collectors.toList());
    }
    public void startTimers() {
        this.startTime = System.currentTimeMillis();
        this.gameState = GameState.RUNNING;
        refillTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (gameState != GameState.RUNNING) {
                    this.cancel();
                    return;
                }
                refillInventory(Bukkit.getPlayer(player1.getPlayerUUID()));
                refillInventory(Bukkit.getPlayer(player2.getPlayerUUID()));
            }
            private void refillInventory(Player player) {
                if (player != null && player.isOnline()) {
                    player.getInventory().setItem(0, new ItemStack(Material.SANDSTONE, 64));
                    player.getInventory().setItem(1, new ItemStack(Material.SANDSTONE, 64));
                    player.updateInventory();
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }
    public void stopTasks() {
        this.gameState = GameState.ENDED;
        if (gameTask != null) {
            gameTask.cancel();
        }
        if (refillTask != null) {
            refillTask.cancel();
        }
    }
    public boolean updateProgress() {
        if (gameState != GameState.RUNNING) {
            return false;
        }
        Player p1 = Bukkit.getPlayer(player1.getPlayerUUID());
        Player p2 = Bukkit.getPlayer(player2.getPlayerUUID());
        if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
            if (p1 == null || !p1.isOnline()) {
                plugin.getDuelGameManager().endGame(this, p2, p1);
            } else {
                plugin.getDuelGameManager().endGame(this, p1, p2);
            }
            return false;
        }
        // THAY ĐỔI: Sử dụng vị trí của game này, không phải của arena template
        player1.setProgressPercent(calculateProgress(p1, this.p1_spawn, this.p1_endPlate));
        player2.setProgressPercent(calculateProgress(p2, this.p2_spawn, this.p2_endPlate));
        return true;
    }
    private double calculateProgress(Player player, Location spawn, Location end) {
        double totalDistance = spawn.distance(end);
        if (totalDistance == 0) return 0;
        double distanceTravelled = spawn.distance(player.getLocation());
        double progress = (distanceTravelled / totalDistance) * 100.0;
        return Math.min(100.0, progress);
    }
    public void addPlacedBlock(Player player, Block block) {
        if (placedBlocks.containsKey(player.getUniqueId())) {
            placedBlocks.get(player.getUniqueId()).add(block);
        }
    }
    public boolean canBreakBlock(Player player, Block block) {
         if (placedBlocks.containsKey(player.getUniqueId())) {
            return placedBlocks.get(player.getUniqueId()).contains(block);
        }
        return false;
    }
    public void sendTitleToBoth(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        Player p1 = Bukkit.getPlayer(player1.getPlayerUUID());
        Player p2 = Bukkit.getPlayer(player2.getPlayerUUID());
        if (p1 != null) p1.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle));
        if (p2 != null) p2.sendTitle(ChatColor.translateAlternateColorCodes('&', title), ChatColor.translateAlternateColorCodes('&', subtitle));
    }
    public void playSoundToBoth(Sound sound, float volume, float pitch) {
        Player p1 = Bukkit.getPlayer(player1.getPlayerUUID());
        Player p2 = Bukkit.getPlayer(player2.getPlayerUUID());
        if (p1 != null) p1.playSound(p1.getLocation(), sound, volume, pitch);
        if (p2 != null) p2.playSound(p2.getLocation(), sound, volume, pitch);
    }
    public DuelPlayer getDuelPlayer(UUID uuid) {
        if (player1.getPlayerUUID().equals(uuid)) return player1;
        if (player2.getPlayerUUID().equals(uuid)) return player2;
        return null;
    }
    // MỚI: Getter để các class khác có thể truy cập
    public DuelArena getArenaTemplate() { return arenaTemplate; }
    public int getLaneIndex() { return laneIndex; }
    public Location getP1_spawn() { return p1_spawn; }
    public Location getP1_corner1() { return p1_corner1; }
    public Location getP1_corner2() { return p1_corner2; }
    public Location getP1_endPlate() { return p1_endPlate; }
    public List<Location> getP1_checkpoints() { return p1_checkpoints; }
    public Location getP2_spawn() { return p2_spawn; }
    public Location getP2_corner1() { return p2_corner1; }
    public Location getP2_corner2() { return p2_corner2; }
    public Location getP2_endPlate() { return p2_endPlate; }
    public List<Location> getP2_checkpoints() { return p2_checkpoints; }
    // --- Các hàm cũ không thay đổi ---
    public DuelPlayer getDuelPlayer1() { return player1; }
    public void setGameTask(BukkitTask gameTask) { this.gameTask = gameTask; }
    public GameState getGameState() { return gameState; }
    public double getElapsedTimeSeconds() {
        if (startTime == 0) return 0.0;
        double rawTime = (System.currentTimeMillis() - startTime) / 1000.0;
        return Math.ceil(rawTime / 0.05) * 0.05;
    }
     public double getProgress(UUID uuid) {
        DuelPlayer dp = getDuelPlayer(uuid);
        return dp != null ? dp.getProgressPercent() : 0.0;
    }
    public double getOpponentProgress(UUID uuid) {
        UUID opponentUUID = getDuelPlayer(uuid).getOpponentUUID();
        return getProgress(opponentUUID);
    }
     // Bỏ hàm clearAllPlacedBlocks() vì sẽ được xử lý trong DuelGameManager
}