package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnalyticsCollection {

    private final MongoCollection<Document> players;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    public AnalyticsCollection() {
        MongoDatabase db = SkyCore.getDatabase().getDatabase();
        this.players = db.getCollection("analytics");
        this.players.createIndex(new Document("uuid", 1), new IndexOptions().unique(true));
    }

    public boolean checkJoin(String uuid, String username, String ip, String joinedFrom) {
        String now = LocalDateTime.now().toString();

        boolean newPlayer = players.find(Filters.eq("uuid", uuid)).first() == null;

        Document joinEntry = new Document("username", username)
                .append("ip", ip)
                .append("join_date", now)
                .append("joined_from", joinedFrom);

        players.updateOne(
                Filters.eq("uuid", uuid),
                new Document("$set", new Document("last_joined", now))
                        .append("$inc", new Document("total_joins", 1))
                        .append("$push", new Document("joins", joinEntry))
                        .append("$setOnInsert", new Document("uuid", uuid)
                                .append("total_onlinetime", 0L)
                                .append("total_afktime", 0L)),
                new UpdateOptions().upsert(true)
        );

        return newPlayer;
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long l) return l;
        if (val instanceof Integer i) return i.longValue();
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    public void addPlaytime(String uuid, long seconds, boolean isAfk) {
        exec.submit(() -> {
            String timeField  = isAfk ? "total_afktime"  : "total_onlinetime";
            String dailyField = isAfk ? "daily_afk"      : "daily_online";
            String today = LocalDate.now().toString();
            LocalDateTime now = LocalDateTime.now();
            String hour = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH"));
            int minute = now.getMinute() / 5 * 5;
            String slot = now.withMinute(minute).withSecond(0).withNano(0)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            Document inc = new Document(timeField, seconds)
                    .append("daily." + today + ".online", isAfk ? 0 : seconds)
                    .append("daily." + today + ".afk",    isAfk ? seconds : 0);

            if (!isAfk) {
                long minutes = Math.max(1L, seconds / 60L);
                inc.append("hourly." + hour, minutes);
            }

            players.updateOne(
                    Filters.eq("uuid", uuid),
                    new Document("$inc", inc)
                            .append("$set", new Document("slots." + slot, isAfk ? "AFK" : "ONLINE")),
                    new UpdateOptions().upsert(true)
            );
        });
    }

    public void logAction(String uuid, String type, String content) {
        Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
            Document logEntry = new Document("type", type)
                    .append("content", content)
                    .append("timestamp", LocalDateTime.now().toString());

            players.updateOne(
                    Filters.eq("uuid", uuid),
                    new Document("$push", new Document("logs", logEntry))
            );
        });
    }

    public List<String[]> topPlaytime(int limit) {
        List<Document> results = players.aggregate(List.of(
                new Document("$addFields", new Document("combined_time",
                        new Document("$add", List.of("$total_onlinetime", "$total_afktime")))),
                new Document("$sort", new Document("combined_time", -1)),
                new Document("$limit", limit)
        )).into(new ArrayList<>());

        List<String[]> top = new ArrayList<>();
        for (Document doc : results) {
            long combined = toLong(doc.get("combined_time"));
            top.add(new String[]{ uuidUser(doc.getString("uuid")), formatSeconds(combined) });
        }
        return top;
    }

    public int totalPlayerCount() {
        return (int) players.countDocuments();
    }

    public PlaytimeData getPlaytime(String uuid) {
        Document doc = players.find(Filters.eq("uuid", uuid)).first();
        if (doc == null) return new PlaytimeData(0, 0);
        return new PlaytimeData(toLong(doc.get("total_onlinetime")), toLong(doc.get("total_afktime")));
    }

    private String formatSeconds(long totalSeconds) {
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    private String uuidUser(String uuid) {
        try {
            OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            if (player.getName() != null) return player.getName();
        } catch (Exception ignored) {}
        return uuid;
    }

    public record PlaytimeData(long totalOnline, long totalAfk) {
        public long getCombinedTime() { return totalOnline + totalAfk; }
    }
}