package me.dtqdev.bridgeracing.data;

import java.util.UUID;

public class PlayerInQueue {
    private final UUID uuid;
    private final long joinTime;

    public PlayerInQueue(UUID uuid) {
        this.uuid = uuid;
        this.joinTime = System.currentTimeMillis();
    }

    public UUID getUuid() {
        return uuid;
    }

    public long getJoinTime() {
        return joinTime;
    }
}