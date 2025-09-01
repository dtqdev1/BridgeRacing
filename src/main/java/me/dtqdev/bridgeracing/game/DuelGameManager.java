package me.dtqdev.bridgeracing.game;

import me.dtqdev.bridgeracing.BridgeRacing;
import me.dtqdev.bridgeracing.data.DuelArena;
import me.dtqdev.bridgeracing.data.DuelPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.text.DecimalFormat;
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
        return new DuelPlayer(player.getUniqueId(), originalLocation, null);
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
        double oldBest = plugin.getDuelRecordManager().getBestTime(winner.getUniqueId(), mapId);
        plugin.getDuelRecordManager().setBestTime(winner.getUniqueId(), mapId, timeTaken);
        if (oldBest == -1 || timeTaken < oldBest) {
            winner.sendMessage(ChatColor.GOLD + "Bạn đã lập kỷ lục mới trên map " + mapId + ": " + df.format(timeTaken) + "s");
        }
        int eloChange = plugin.getEloManager().updateElo(winner.getUniqueId(), loser.getUniqueId());
        plugin.getHistoryManager().addMatchHistory(winner.getUniqueId(), loser.getName(), true, eloChange);
        plugin.getHistoryManager().addMatchHistory(loser.getUniqueId(), winner.getName(), false, -eloChange);
        plugin.getEloManager().saveAllPlayerData();
        winner.sendTitle(ChatColor.translateAlternateColorCodes('&', "&a&lWIN!"), ChatColor.translateAlternateColorCodes('&', "&e+" + eloChange + " ELO"));
        loser.sendTitle(ChatColor.translateAlternateColorCodes('&', "&c&lLOSE!"), ChatColor.translateAlternateColorCodes('&', "&7-" + eloChange + " ELO"));
        winner.playSound(winner.getLocation(), Sound.LEVEL_UP, 1, 1);
        loser.playSound(loser.getLocation(), Sound.VILLAGER_NO, 1, 1);
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
                game.clearAllPlacedBlocks();
                teleportBack(winner, game.getDuelPlayer(winner.getUniqueId()));
                teleportBack(loser, game.getDuelPlayer(loser.getUniqueId()));
                activeDuels.remove(winner.getUniqueId());
                activeDuels.remove(loser.getUniqueId());
            }
        }.runTaskLater(plugin, 60L);
    }

    private void teleportBack(Player player, DuelPlayer duelPlayerData) {
        if (player == null || !player.isOnline() || duelPlayerData == null) return;
        player.setGameMode(GameMode.SURVIVAL);
        player.getInventory().clear();
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
        // Clear placed blocks by this player
        List<Block> playerBlocks = game.placedBlocks.get(player.getUniqueId());
        if (playerBlocks != null) {
            for (Block block : playerBlocks) {
                block.setType(Material.AIR);
            }
            playerBlocks.clear();
        }
        player.teleport(respawnLoc);
        player.playSound(player.getLocation(), Sound.HURT_FLESH, 1, 1);
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
}