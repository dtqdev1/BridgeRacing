package me.dtqdev.bridgeracing.data;

import org.bukkit.Location;
import java.util.UUID;

/**
 * Lưu trữ trạng thái của một người chơi trong một trận đấu đang diễn ra.
 */
public class DuelPlayer {
    private final UUID playerUUID;
    private UUID opponentUUID;
    private final Location originalLocation;
    private final String originalFastBuilderMode;
    private int lastCheckpointIndex = -1; // -1 là chưa qua checkpoint nào
    private double progressPercent = 0.0;

    public DuelPlayer(UUID playerUUID, Location originalLocation, String originalFastBuilderMode) {
        this.playerUUID = playerUUID;
        this.originalLocation = originalLocation;
        this.originalFastBuilderMode = originalFastBuilderMode;
    }

    // --- Getters & Setters ---
    public UUID getPlayerUUID() { return playerUUID; }
    public UUID getOpponentUUID() { return opponentUUID; }
    public void setOpponentUUID(UUID opponentUUID) { this.opponentUUID = opponentUUID; }
    public Location getOriginalLocation() { return originalLocation; }
    public String getOriginalFastBuilderMode() { return originalFastBuilderMode; }
    public int getLastCheckpointIndex() { return lastCheckpointIndex; }
    public void setLastCheckpointIndex(int lastCheckpointIndex) { this.lastCheckpointIndex = lastCheckpointIndex; }
    public double getProgressPercent() { return progressPercent; }
    public void setProgressPercent(double progressPercent) { this.progressPercent = progressPercent; }
}