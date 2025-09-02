package me.dtqdev.bridgeracing.spectate;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.game.DuelGame;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectateManager implements Listener {

    private final BridgeRacing plugin;
    private final Map<UUID, SpectateSession> spectators = new HashMap<>();

    private static class SpectateSession {
        final Player spectator;
        final Player target;
        final Location originalLocation;
        final ItemStack[] originalInventory;
        final GameMode originalGameMode;
        BukkitTask updateTask;

        SpectateSession(Player spectator, Player target) {
            this.spectator = spectator;
            this.target = target;
            this.originalLocation = spectator.getLocation();
            this.originalInventory = spectator.getInventory().getContents();
            this.originalGameMode = spectator.getGameMode();
        }
    }

    public SpectateManager(BridgeRacing plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void startSpectating(Player spectator, Player target) {
        if (spectators.containsKey(spectator.getUniqueId())) {
            stopSpectating(spectator);
        }

        SpectateSession session = new SpectateSession(spectator, target);
        spectators.put(spectator.getUniqueId(), session);

        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.getInventory().clear();
        spectator.teleport(target.getLocation());
        spectator.setSpectatorTarget(target);

        plugin.getMessageUtil().sendMessage(spectator, "command.spectate.start", "{player}", target.getName());
        
        session.updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                DuelGame game = plugin.getDuelGameManager().getDuelByPlayer(target.getUniqueId());
                if (game == null || !target.isOnline()) {
                    stopSpectating(spectator);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopSpectating(Player spectator) {
        SpectateSession session = spectators.remove(spectator.getUniqueId());
        if (session != null) {
            if (session.updateTask != null) {
                session.updateTask.cancel();
            }
            spectator.setSpectatorTarget(null);
            spectator.teleport(session.originalLocation);
            spectator.setGameMode(session.originalGameMode);
            spectator.getInventory().setContents(session.originalInventory);
            plugin.getMessageUtil().sendMessage(spectator, "command.spectate.stop");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Nếu người chơi đang theo dõi thoát
        if (spectators.containsKey(event.getPlayer().getUniqueId())) {
            stopSpectating(event.getPlayer());
        }
        // Nếu người chơi bị theo dõi thoát
        spectators.values().forEach(session -> {
            if (session.target.getUniqueId().equals(event.getPlayer().getUniqueId())) {
                stopSpectating(session.spectator);
            }
        });
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && spectators.containsKey(event.getPlayer().getUniqueId())) {
            stopSpectating(event.getPlayer());
        }
    }
}