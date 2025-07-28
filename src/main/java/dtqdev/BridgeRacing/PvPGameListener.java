package dtqdev.BridgeRacing;

import me.dtqdev.fastbuilder.FastbuilderMain;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class PvPGameListener implements Listener {
    private final BridgeRacing plugin;
    private final DuelMatchManager matchManager;
    private final PvPGuiManager guiManager;
    private final FastbuilderMain fastBuilder;

    public PvPGameListener(BridgeRacing plugin, DuelMatchManager matchManager, PvPGuiManager guiManager, FastbuilderMain fastBuilder) {
        this.plugin = plugin;
        this.matchManager = matchManager;
        this.guiManager = guiManager;
        this.fastBuilder = fastBuilder;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (fastBuilder.getArenaManager().getArenaByPlayer(player) != null) {
            guiManager.updatePlayerItem(player, false);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        DuelMatch match = matchManager.getMatch(player);
        if (match != null) {
            Player other = match.getPlayer1().equals(player) ? match.getPlayer2() : match.getPlayer1();
            matchManager.endMatch(player, other.isOnline() ? other : null, false);
        }
        matchManager.removeFromQueue(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.INK_SACK) return;

        if (item.getDurability() == 10) { // Lime Dye
            if (fastBuilder.getArenaManager().getArenaByPlayer(player) != null) {
                guiManager.openGui(player);
                event.setCancelled(true);
            }
        } else if (item.getDurability() == 1) { // Red Dye
            matchManager.removeFromQueue(player);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        DuelMatch match = matchManager.getMatch(player);
        if (match == null || !match.isStarted()) return;

        if (!match.getArena().isInBounds(event.getBlock().getLocation(), player.equals(match.getPlayer1()))) {
            player.sendMessage(ChatColor.RED + "Bạn chỉ có thể xây trong khu vực của mình!");
            event.setCancelled(true);
            return;
        }

        match.addBlock(player, event.getBlock().getLocation());
    }

    @EventHandler
public void onBlockBreak(BlockBreakEvent event) {
    Player player = event.getPlayer();
    DuelMatch match = matchManager.getMatch(player);
    if (match == null || !match.isStarted()) return;

    if (!match.getBlocksPlaced(player).contains(event.getBlock().getLocation())) {
        player.sendMessage(ChatColor.RED + "Bạn không thể phá block này!");
        event.setCancelled(true);
    }
}

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        DuelMatch match = matchManager.getMatch(player);
        if (match == null || !match.isStarted()) return;

        if (!match.getArena().isInBounds(event.getTo(), player.equals(match.getPlayer1()))) {
            player.teleport(match.getLastCheckpoint(player));
        }

        Material blockType = event.getTo().getBlock().getType();
        if (blockType == Material.IRON_PLATE) {
            match.passCheckpoint(player, event.getTo().getBlock().getLocation());
        } else if (blockType == Material.GOLD_PLATE) {
            matchManager.endMatch(player, player, true);
        }
    }
}