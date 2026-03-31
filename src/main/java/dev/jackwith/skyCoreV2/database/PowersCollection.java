package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.data.PlayerPowerData;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PowersCollection {

    private final MongoCollection<Document> collection;
    private final ConcurrentHashMap<UUID, PlayerPowerData> cache = new ConcurrentHashMap<>();

    public PowersCollection() {
        MongoDatabase db = SkyCore.getDatabase().getDatabase();
        this.collection = db.getCollection("powers");
        this.collection.createIndex(new Document("uuid", 1), new IndexOptions().unique(true));
    }

    public void loadPlayer(UUID uuid) {
        CompletableFuture.runAsync(() -> {
            Document doc = collection.find(Filters.eq("uuid", uuid.toString())).first();
            String current  = doc != null ? doc.getString("current_power")  : "none";
            String unlocked = doc != null ? doc.getString("unlocked_power") : "none";
            cache.put(uuid, new PlayerPowerData(uuid, current, unlocked, !"none".equalsIgnoreCase(unlocked)));
        });
    }

    public void unloadPlayer(UUID uuid) {
        cache.remove(uuid);
    }

    public String getPower(UUID uuid) {
        PlayerPowerData data = cache.get(uuid);
        return data != null ? data.getPower() : "none";
    }

    public String getUnlockedPower(UUID uuid) {
        PlayerPowerData data = cache.get(uuid);
        return data != null ? data.getUnlockedPower() : "none";
    }

    public boolean hasUsedFreePower(UUID uuid) {
        PlayerPowerData data = cache.get(uuid);
        return data != null && data.hasUsedFreePower();
    }

    public boolean isPowerOwned(UUID uuid, String key) {
        return key.equalsIgnoreCase(getUnlockedPower(uuid));
    }

    public PlayerPowerData getPlayerPower(UUID uuid) {
        return cache.getOrDefault(uuid, new PlayerPowerData(uuid, "none", "none", false));
    }

    public void createPlayer(UUID uuid) {
        cache.computeIfAbsent(uuid, k -> new PlayerPowerData(k, "none", "none", false));
        CompletableFuture.runAsync(() ->
                collection.updateOne(
                        Filters.eq("uuid", uuid.toString()),
                        new Document("$setOnInsert", new Document("uuid", uuid.toString())
                                .append("current_power", "none")
                                .append("unlocked_power", "none")),
                        new UpdateOptions().upsert(true)
                )
        );
    }

    public void setPower(UUID uuid, String power) {
        PlayerPowerData data = cache.get(uuid);
        if (data != null) data.setPower(power);

        CompletableFuture.runAsync(() ->
                collection.updateOne(
                        Filters.eq("uuid", uuid.toString()),
                        new Document("$set", new Document("current_power", power)),
                        new UpdateOptions().upsert(true)
                )
        );
    }

    public void setPowerOwned(UUID uuid, String power) {
        PlayerPowerData data = cache.get(uuid);
        if (data != null) {
            data.setUnlockedPower(power);
            data.setUsedFreePower(true);
        }

        CompletableFuture.runAsync(() ->
                collection.updateOne(
                        Filters.eq("uuid", uuid.toString()),
                        new Document("$set", new Document("unlocked_power", power)),
                        new UpdateOptions().upsert(true)
                )
        );
    }
}