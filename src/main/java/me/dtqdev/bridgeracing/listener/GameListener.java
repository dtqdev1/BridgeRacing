package me.dtqdev.bridgeracing.listener;
import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.game.DuelGame;
import me.dtqdev.bridgeracing.game.DuelGameManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.List;
import java.util.UUID;
public class GameListener implements Listener {
    private final BridgeRacing plugin;
    private final DuelGameManager gameManager;
    public GameListener(BridgeRacing plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getDuelGameManager();
    }
    private void playSoundFromConfig(Player player, String path) {
        // ... (hàm này không thay đổi)
        String soundString = plugin.getConfig().getString(path);
        if (soundString == null || soundString.isEmpty()) return;
        try {
            String[] parts = soundString.split(",");
            Sound sound = Sound.valueOf(parts[0].toUpperCase());
            float volume = Float.parseFloat(parts[1]);
            float pitch = Float.parseFloat(parts[2]);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid sound format in config.yml at path: " + path);
        }
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        Player player = event.getPlayer();
        DuelGame game = gameManager.getDuelByPlayer(player.getUniqueId());
        if (game == null) {
            return;
        }
        if (game.getGameState() == DuelGame.GameState.COUNTDOWN) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockZ() != to.getBlockZ()) {
                 player.teleport(from); // Cải tiến: giữ người chơi ở đúng vị trí
            }
            return;
        }
        if (game.getGameState() != DuelGame.GameState.RUNNING) {
            return;
        }
        // THAY ĐỔI: Kiểm tra isInBounds của chính game đó
        boolean isP1 = player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID());
        Location corner1 = isP1 ? game.getP1_corner1() : game.getP2_corner1();
        Location corner2 = isP1 ? game.getP1_corner2() : game.getP2_corner2();
        
        Location loc = player.getLocation();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        boolean inBounds = loc.getX() >= minX && loc.getX() <= maxX &&
                           loc.getY() >= minY && loc.getY() <= maxY &&
                           loc.getZ() >= minZ && loc.getZ() <= maxZ;
        if (!inBounds) {
            plugin.getMessageUtil().sendMessage(player, "gameplay.fall");
            gameManager.handlePlayerFall(player, game);
            return;
        }
        List<Location> checkpoints;
        Location endPlateLoc;
        // THAY ĐỔI: Lấy vị trí từ game, không phải từ arena
        if (isP1) {
            checkpoints = game.getP1_checkpoints();
            endPlateLoc = game.getP1_endPlate();
        } else {
            checkpoints = game.getP2_checkpoints();
            endPlateLoc = game.getP2_endPlate();
        }
        Location playerBlockLoc = player.getLocation().getBlock().getLocation();
        for (int i = 0; i < checkpoints.size(); i++) {
            Location cp = checkpoints.get(i);
            if (playerBlockLoc.toVector().equals(cp.toVector())) { // So sánh chính xác hơn
                if (i > game.getDuelPlayer(player.getUniqueId()).getLastCheckpointIndex()) {
                    game.getDuelPlayer(player.getUniqueId()).setLastCheckpointIndex(i);
                    plugin.getMessageUtil().sendMessage(player, "gameplay.checkpoint", "{checkpoint_index}", String.valueOf(i + 1));
                    playSoundFromConfig(player, "sounds.checkpoint");
                }
            }
        }
        if (playerBlockLoc.toVector().equals(endPlateLoc.toVector())) { // So sánh chính xác hơn
            playSoundFromConfig(player, "sounds.win-plate");
            Player opponent = plugin.getServer().getPlayer(game.getDuelPlayer(player.getUniqueId()).getOpponentUUID());
            gameManager.endGame(game, player, opponent);
        }
    }
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        DuelGame game = gameManager.getDuelByPlayer(player.getUniqueId());
        if (game == null) return;
        if (event.getBlock().getType() != Material.SANDSTONE) {
            event.setCancelled(true);
            return;
        }
        if (game.getGameState() != DuelGame.GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        // THAY ĐỔI: Kiểm tra isInBounds của chính game đó
        boolean isP1 = player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID());
        Location corner1 = isP1 ? game.getP1_corner1() : game.getP2_corner1();
        Location corner2 = isP1 ? game.getP1_corner2() : game.getP2_corner2();
        Location loc = event.getBlock().getLocation();
        double minX = Math.min(corner1.getX(), corner2.getX());
        double maxX = Math.max(corner1.getX(), corner2.getX());
        double minY = Math.min(corner1.getY(), corner2.getY());
        double maxY = Math.max(corner1.getY(), corner2.getY());
        double minZ = Math.min(corner1.getZ(), corner2.getZ());
        double maxZ = Math.max(corner1.getZ(), corner2.getZ());
        boolean inBounds = loc.getX() >= minX && loc.getX() <= maxX &&
                           loc.getY() >= minY && loc.getY() <= maxY &&
                           loc.getZ() >= minZ && loc.getZ() <= maxZ;
        if (!inBounds) {
             event.setCancelled(true);
             return;
        }
        game.addPlacedBlock(player, event.getBlock());
    }
    // Các hàm còn lại (onBlockBreak, onPlayerQuit, onDamage, onHunger) không cần thay đổi.
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        DuelGame game = gameManager.getDuelByPlayer(player.getUniqueId());
        if (game == null) return;
        if (!game.canBreakBlock(player, event.getBlock())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelGame game = gameManager.getDuelByPlayer(player.getUniqueId());
        if (game != null) {
            gameManager.handlePlayerQuit(player, game);
        }
        plugin.getQueueManager().removePlayerFromAllQueues(player);
    }
    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (gameManager.getDuelByPlayer(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (gameManager.getDuelByPlayer(player.getUniqueId()) != null) {
                event.setCancelled(true);
            }
        }
    }
}