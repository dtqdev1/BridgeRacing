package dtqdev.BridgeRacing;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.Bukkit; // For Bukkit-related calls

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DuelMatch {
    private final BridgeRacing plugin;
    private final DuelArena arena;
    private final Player player1;
    private final Player player2;
    private final Map<UUID, Location> lastCheckpoints;
    private final Map<UUID, List<Location>> blocksPlaced;
    private final Map<UUID, List<Location>> passedCheckpoints;
    private long startTime;
    private boolean isStarted;

    public DuelMatch(BridgeRacing plugin, DuelArena arena, Player player1, Player player2) {
        this.plugin = plugin;
        this.arena = arena;
        this.player1 = player1;
        this.player2 = player2;
        this.lastCheckpoints = new HashMap<>();
        this.blocksPlaced = new HashMap<>();
        this.passedCheckpoints = new HashMap<>();
        this.blocksPlaced.put(player1.getUniqueId(), new ArrayList<>());
        this.blocksPlaced.put(player2.getUniqueId(), new ArrayList<>());
        this.passedCheckpoints.put(player1.getUniqueId(), new ArrayList<>());
        this.passedCheckpoints.put(player2.getUniqueId(), new ArrayList<>());
        this.isStarted = false;
    }

    public void start() {
        // Lưu inventory
        plugin.getFastBuilder().getPlayerDataManager().getData().set(player1.getUniqueId() + ".inventory", player1.getInventory().getContents());
        plugin.getFastBuilder().getPlayerDataManager().getData().set(player2.getUniqueId() + ".inventory", player2.getInventory().getContents());
        plugin.getFastBuilder().getPlayerDataManager().save();

        // Thiết lập inventory
        ItemStack sandstone = new ItemStack(Material.SANDSTONE, 64);
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        player1.getInventory().clear();
        player2.getInventory().clear();
        player1.getInventory().setItem(0, sandstone);
        player1.getInventory().setItem(1, sandstone);
        player1.getInventory().setItem(2, pickaxe);
        player2.getInventory().setItem(0, sandstone);
        player2.getInventory().setItem(1, sandstone);
        player2.getInventory().setItem(2, pickaxe);

        // Dịch chuyển người chơi
        player1.teleport(arena.getSpawn1());
        player2.teleport(arena.getSpawn2());

        // Đếm ngược
        new BukkitRunnable() {
            int countdown = 5;
            @Override
            public void run() {
                if (countdown <= 0) {
                    isStarted = true;
                    startTime = System.currentTimeMillis();
                    player1.sendMessage(ChatColor.GREEN + "Bắt đầu!");
                    player2.sendMessage(ChatColor.GREEN + "Bắt đầu!");
                    cancel();
                } else {
                    plugin.getFastBuilder().getTitleManager().sendCompletionTitle(player1, countdown, 0);
                    plugin.getFastBuilder().getTitleManager().sendCompletionTitle(player2, countdown, 0);
                    countdown--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    public void end(Player winner, boolean normalEnd) {
        isStarted = false;
        double time = (System.currentTimeMillis() - startTime) / 1000.0;
        time = Math.ceil(time * 100.0) / 100.0;

        if (winner != null) {
            plugin.getFastBuilder().getTitleManager().sendCompletionTitle(winner, time, normalEnd ? 20 : 0);
            winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1.0f, 1.0f);
            if (normalEnd) {
                plugin.getFastBuilder().getEmeraldManager().addBalance(winner.getUniqueId(), 20);
                plugin.getFastBuilder().getFireworkManager().launchPbFirework(winner);
            }
        }

        // Reset map
        for (List<Location> blocks : blocksPlaced.values()) {
            for (Location loc : blocks) {
                loc.getBlock().setType(Material.AIR);
            }
        }

        // Khôi phục inventory và dịch chuyển
        restorePlayer(player1);
        restorePlayer(player2);
    }

    private void restorePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        ItemStack[] inventory = (ItemStack[]) plugin.getFastBuilder().getPlayerDataManager().getData()
                .get(player.getUniqueId() + ".inventory");
        player.getInventory().setContents(inventory != null ? inventory : new ItemStack[36]);
        me.dtqdev.fastbuilder.Arena arena = plugin.getFastBuilder().getArenaManager().getArenaByPlayer(player);
        if (arena != null) {
            player.teleport(arena.getSpawnLocation());
        } else {
            player.teleport(plugin.getFastBuilder().getLobbyLocation());
        }
    }

    public void addBlock(Player player, Location location) {
        blocksPlaced.get(player.getUniqueId()).add(location);
    }

    public void passCheckpoint(Player player, Location checkpoint) {
        boolean isPlayer1 = player.getUniqueId().equals(player1.getUniqueId());
        List<Location> checkpoints = isPlayer1 ? arena.getCheckpointsPlayer1() : arena.getCheckpointsPlayer2();
        if (checkpoints.contains(checkpoint) && !passedCheckpoints.get(player.getUniqueId()).contains(checkpoint)) {
            passedCheckpoints.get(player.getUniqueId()).add(checkpoint);
            lastCheckpoints.put(player.getUniqueId(), checkpoint);
            arena.updateHologram(checkpoint, isPlayer1);
        }
    }

    public double getProgress(Player player) {
        Location start = player.getUniqueId().equals(player1.getUniqueId()) ? arena.getSpawn1() : arena.getSpawn2();
        Location end = player.getUniqueId().equals(player1.getUniqueId()) ? arena.getGoldPlate1() : arena.getGoldPlate2();
        double totalDistance = start.distance(end);
        double currentDistance = player.getLocation().distance(start);
        return Math.min((currentDistance / totalDistance) * 100, 100.0);
    }

    public double getTime() {
        if (!isStarted) return 0.0;
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }

    public List<Location> getBlocksPlaced(Player player) {
        return blocksPlaced.get(player.getUniqueId());
    }

    public Location getLastCheckpoint(Player player) {
        return lastCheckpoints.getOrDefault(player.getUniqueId(), player.getUniqueId().equals(player1.getUniqueId()) ? arena.getSpawn1() : arena.getSpawn2());
    }

    public boolean isStarted() {
        return isStarted;
    }

    public DuelArena getArena() {
        return arena;
    }

    public Player getPlayer1() {
        return player1;
    }

    public Player getPlayer2() {
        return player2;
    }
}