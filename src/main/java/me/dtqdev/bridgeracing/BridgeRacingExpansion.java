package me.dtqdev.bridgeracing;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dtqdev.bridgeracing.data.EloRank;
import me.dtqdev.bridgeracing.game.DuelGame;
import me.dtqdev.bridgeracing.manager.EloManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.text.DecimalFormat;

public class BridgeRacingExpansion extends PlaceholderExpansion {

    private final BridgeRacing plugin;
    private final DecimalFormat df = new DecimalFormat("0.000");

    public BridgeRacingExpansion(BridgeRacing plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "bridgeracing"; }
    @Override
    public @NotNull String getAuthor() { return "dtqdev"; }
    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override
    public boolean persist() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) return "";

        EloManager eloManager = plugin.getEloManager();

        switch (identifier) {
            case "elo":
                return String.valueOf(eloManager.getElo(player.getUniqueId()));
            case "rank":
                EloRank rank = eloManager.getRank(eloManager.getElo(player.getUniqueId()));
                return rank != null ? ChatColor.translateAlternateColorCodes('&', rank.getDisplayName()) : "N/A";
            case "elo_rank":
                int globalRank = eloManager.getGlobalRank(player.getUniqueId());
                return globalRank > 0 ? "#" + globalRank : "Unranked";
        }

        DuelGame game = plugin.getDuelGameManager().getDuelByPlayer(player.getUniqueId());
        if (game != null) {
            switch (identifier) {
                case "timer":
                    return df.format(game.getElapsedTimeSeconds());
                case "my_progress":
                    return String.format("%.1f", game.getProgress(player.getUniqueId()));
                case "opponent_progress":
                    return String.format("%.1f", game.getOpponentProgress(player.getUniqueId()));
            }
        } else {
            if (identifier.equals("timer") || identifier.equals("my_progress") || identifier.equals("opponent_progress")) {
                return "0.0";
            }
        }
        
        if (identifier.equals("pb")) {
            DuelGame currentGame = plugin.getDuelGameManager().getDuelByPlayer(player.getUniqueId());
            if (currentGame != null) {
                String mapId = currentGame.getArena().getId();
                double pb = plugin.getDuelRecordManager().getBestTime(player.getUniqueId(), mapId);
                return pb > 0 ? df.format(pb) : "N/A";
            }
            return "N/A"; // Trả về N/A nếu không ở trong trận đấu
        } else if (identifier.startsWith("pb_")) {
            String mapId = identifier.substring(3);
            double pb = plugin.getDuelRecordManager().getBestTime(player.getUniqueId(), mapId);
            return pb > 0 ? df.format(pb) : "N/A";
        }

        return null;
    }
}