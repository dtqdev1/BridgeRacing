package me.dtqdev.bridgeracing.listener;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.game.DuelGame;
import me.dtqdev.bridgeracing.game.DuelGameManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
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

public class GameListener implements Listener {
    private final BridgeRacing plugin;
    private final DuelGameManager gameManager;

    public GameListener(BridgeRacing plugin) {
        this.plugin = plugin;
        this.gameManager = plugin.getDuelGameManager();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DuelGame game = gameManager.getDuelByPlayer(player.getUniqueId());
        if (game == null || game.getGameState() != DuelGame.GameState.RUNNING) {
            return;
        }
        Location spawn = game.getArena().getP1_spawn();
        if (player.getLocation().getY() < spawn.getY() - 20) {
            gameManager.handlePlayerFall(player, game);
            return;
        }

        Location playerLoc = player.getLocation();

        // Checkpoint check
        List<Location> checkpoints;
        Location endPlateLoc;
        if (player.getUniqueId().equals(game.getDuelPlayer1().getPlayerUUID())) {
            checkpoints = game.getArena().getP1_checkpoints();
            endPlateLoc = game.getArena().getP1_endPlate();
        } else {
            checkpoints = game.getArena().getP2_checkpoints();
            endPlateLoc = game.getArena().getP2_endPlate();
        }
        for (int i = 0; i < checkpoints.size(); i++) {
            Location cp = checkpoints.get(i);
            if (playerLoc.distance(cp) < 0.5 && i == game.getDuelPlayer(player.getUniqueId()).getLastCheckpointIndex() + 1) {
                game.getDuelPlayer(player.getUniqueId()).setLastCheckpointIndex(i);
                player.sendMessage(ChatColor.GREEN + "Đã đạt checkpoint " + (i + 1) + "!");
                player.playSound(player.getLocation(), Sound.LEVEL_UP, 1, 1.5f);
            }
        }

        // End plate check
        if (playerLoc.distance(endPlateLoc) < 0.5) {
            Player opponent = plugin.getServer().getPlayer(game.getDuelPlayer(player.getUniqueId()).getOpponentUUID());
            gameManager.endGame(game, player, opponent);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        DuelGame game = gameManager.getDuelByPlayer(player.getUniqueId());
        if (game == null) return;
        if (game.getGameState() != DuelGame.GameState.RUNNING) {
            event.setCancelled(true);
            return;
        }
        if (!game.getArena().isInBounds(player, game.getDuelPlayer(player.getUniqueId()).getPlayerUUID())) {
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