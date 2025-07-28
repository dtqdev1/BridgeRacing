package dtqdev.BridgeRacing;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PvPPlaceholderExpansion extends PlaceholderExpansion {
    private final BridgeRacing plugin;
    private final DuelMatchManager matchManager;

    public PvPPlaceholderExpansion(BridgeRacing plugin, DuelMatchManager matchManager) {
        this.plugin = plugin;
        this.matchManager = matchManager;
    }

    @Override
    public String getIdentifier() {
        return "bridgeracing";
    }

    @Override
    public String getAuthor() {
        return "dtqdev";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        DuelMatch match = matchManager.getMatch(player);
        if (match == null) return "N/A";

        switch (identifier) {
            case "progress_player1":
                return String.format("%.2f", match.getProgress(match.getPlayer1()));
            case "progress_player2":
                return String.format("%.2f", match.getProgress(match.getPlayer2()));
            case "timer":
                return String.format("%.2f", match.getTime());
            case "blocks_placed":
                return String.valueOf(match.getBlocksPlaced(player));
            default:
                return null;
        }
    }
}