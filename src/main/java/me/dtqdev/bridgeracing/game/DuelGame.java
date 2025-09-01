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

public class DuelGame {
    private final BridgeRacing plugin = BridgeRacing.getInstance();
    private final DuelArena arena;
    private final DuelPlayer player1;
    private final DuelPlayer player2;
    public final Map<UUID, List<Block>> placedBlocks = new ConcurrentHashMap<>();
    private long startTime;
    private BukkitTask gameTask;
    private BukkitTask refillTask;
    private GameState gameState = GameState.COUNTDOWN;
    public enum GameState {
        COUNTDOWN, RUNNING, ENDED
    }
    public DuelGame(DuelArena arena, DuelPlayer player1, DuelPlayer player2) {
        this.arena = arena;
        this.player1 = player1;
        this.player2 = player2;
        this.placedBlocks.put(player1.getPlayerUUID(), new ArrayList<>());
        this.placedBlocks.put(player2.getPlayerUUID(), new ArrayList<>());
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
        }.runTaskTimer(plugin, 100L, 100L); // 5 seconds = 100 ticks
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
        player1.setProgressPercent(calculateProgress(p1, arena.getP1_spawn(), arena.getP1_endPlate()));
        player2.setProgressPercent(calculateProgress(p2, arena.getP2_spawn(), arena.getP2_endPlate()));
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
    public void clearAllPlacedBlocks() {
        for (List<Block> blocks : placedBlocks.values()) {
            for (Block block : blocks) {
                block.setType(org.bukkit.Material.AIR);
            }
        }
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
    public DuelPlayer getDuelPlayer1() {
        return player1;
    }
    public void setGameTask(BukkitTask gameTask) { this.gameTask = gameTask; }
    public GameState getGameState() { return gameState; }
    public DuelArena getArena() { return arena; }
    public double getElapsedTimeSeconds() {
        if (startTime == 0) return 0.0;
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
     public double getProgress(UUID uuid) {
        DuelPlayer dp = getDuelPlayer(uuid);
        return dp != null ? dp.getProgressPercent() : 0.0;
    }
    public double getOpponentProgress(UUID uuid) {
        UUID opponentUUID = getDuelPlayer(uuid).getOpponentUUID();
        return getProgress(opponentUUID);
    }
}