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
        if (game == null || game.getGameState() != DuelGame.GameState.RUNNING) {
            return;
        }
        Location playerBlockLoc = player.getLocation().getBlock().getLocation();
        // Xử lý rơi ra ngoài
        if (!game.getArena().isInBounds(player.getLocation(), game.getDuelPlayer1().getPlayerUUID(), player.getUniqueId())) {
            plugin.getMessageUtil().sendMessage(player, "gameplay.fall");
            gameManager.handlePlayerFall(player, game);
            return;
        }
        List<Location> checkpoints;
        Location endPlateLoc;
        UUID p1UUID = game.getDuelPlayer1().getPlayerUUID();
        if (player.getUniqueId().equals(p1UUID)) {
            checkpoints = game.getArena().getP1_checkpoints();
            endPlateLoc = game.getArena().getP1_endPlate();
        } else {
            checkpoints = game.getArena().getP2_checkpoints();
            endPlateLoc = game.getArena().getP2_endPlate();
        }
        // Xử lý Checkpoint
        for (int i = 0; i < checkpoints.size(); i++) {
            Location cp = checkpoints.get(i);
            // Kiểm tra một khu vực 3x1 theo trục X xung quanh checkpoint
            if (playerBlockLoc.getBlockY() == cp.getBlockY() &&
                playerBlockLoc.getBlockZ() == cp.getBlockZ() &&
                playerBlockLoc.getBlockX() >= cp.getBlockX() - 1 && playerBlockLoc.getBlockX() <= cp.getBlockX() + 1 &&
                cp.getWorld().getBlockAt(playerBlockLoc).getType() == Material.IRON_PLATE) {

                if (i > game.getDuelPlayer(player.getUniqueId()).getLastCheckpointIndex()) {
                    game.getDuelPlayer(player.getUniqueId()).setLastCheckpointIndex(i);
                    plugin.getMessageUtil().sendMessage(player, "gameplay.checkpoint", "{checkpoint_index}", String.valueOf(i + 1));
                    playSoundFromConfig(player, "sounds.checkpoint");
                }
            }
        }
        // Xử lý về đích
        if (playerBlockLoc.getBlockY() == endPlateLoc.getBlockY() &&
            playerBlockLoc.getBlockZ() == endPlateLoc.getBlockZ() &&
            playerBlockLoc.getBlockX() >= endPlateLoc.getBlockX() - 1 && playerBlockLoc.getBlockX() <= endPlateLoc.getBlockX() + 1 &&
            endPlateLoc.getWorld().getBlockAt(playerBlockLoc).getType() == Material.GOLD_PLATE) {
            
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
        if (!game.getArena().isInBounds(event.getBlock().getLocation(), game.getDuelPlayer1().getPlayerUUID(), player.getUniqueId())) {
             event.setCancelled(true);
             player.sendMessage(ChatColor.RED + "Bạn chỉ có thể xây trong làn đường của mình!");
             return;
        }
        game.addPlacedBlock(player, event.getBlock());
    }
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