package dev.jackwith.skyCoreV2.features.playtime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlaytimeCache {
    private final Map<UUID, Long> sessionStart = new HashMap<>();
    private final Map<UUID, Long> lastActivity = new HashMap<>();
    private final long AFK_THRESHOLD_MS = 300_000; // 5 minutes

    public void startTracking(UUID uuid) {
        long now = System.currentTimeMillis();
        sessionStart.put(uuid, now);
        lastActivity.put(uuid, now);
    }

    public void updateActivity(UUID uuid) {
        lastActivity.put(uuid, System.currentTimeMillis());
    }

    public boolean isAfk(UUID uuid) {
        return (System.currentTimeMillis() - lastActivity.getOrDefault(uuid, 0L)) > AFK_THRESHOLD_MS;
    }

    public long getElapsedAndReset(UUID uuid) {
        if (!sessionStart.containsKey(uuid)) return 0;
        long start = sessionStart.get(uuid);
        long now = System.currentTimeMillis();
        sessionStart.put(uuid, now);
        return (now - start) / 1000;
    }

    public void stopTracking(UUID uuid) {
        sessionStart.remove(uuid);
        lastActivity.remove(uuid);
    }
}