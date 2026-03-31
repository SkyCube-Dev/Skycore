package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.database.data.PlayerPowerData;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PowersCollection {

    private final MongoCollection<Document> collection;

    public PowersCollection() {
        MongoDatabase db = SkyCore.getDatabase().getDatabase();
        this.collection = db.getCollection("powers");
        this.collection.createIndex(new Document("uuid", 1), new IndexOptions().unique(true));
    }

    private Document getPlayer(UUID uuid) {
        return collection.find(Filters.eq("uuid", uuid.toString())).first();
    }

    public void createPlayer(UUID uuid) {
        collection.updateOne(
                Filters.eq("uuid", uuid.toString()),
                new Document("$setOnInsert", new Document("uuid", uuid.toString())
                        .append("current_power", "none")
                        .append("unlocked_power", "none")),
                new UpdateOptions().upsert(true)
        );
    }

    public String getPower(UUID uuid) {
        Document doc = getPlayer(uuid);
        return doc != null ? doc.getString("current_power") : "none";
    }

    public String getUnlockedPower(UUID uuid) {
        Document doc = getPlayer(uuid);
        return doc != null ? doc.getString("unlocked_power") : "none";
    }

    public boolean hasUsedFreePower(UUID uuid) {
        return !"none".equalsIgnoreCase(getUnlockedPower(uuid));
    }

    public boolean isPowerOwned(UUID uuid, String key) {
        return getUnlockedPower(uuid).equalsIgnoreCase(key);
    }

    public PlayerPowerData getPlayerPower(UUID uuid) {
        Document doc = getPlayer(uuid);
        String current  = doc != null ? doc.getString("current_power")  : "none";
        String unlocked = doc != null ? doc.getString("unlocked_power") : "none";
        return new PlayerPowerData(uuid, current, unlocked, !"none".equalsIgnoreCase(unlocked));
    }

    public void setPower(UUID uuid, String power) {
        CompletableFuture.runAsync(() ->
                collection.updateOne(
                        Filters.eq("uuid", uuid.toString()),
                        new Document("$set", new Document("current_power", power)),
                        new UpdateOptions().upsert(true)
                )
        );
    }

    public void setPowerOwned(UUID uuid, String power) {
        CompletableFuture.runAsync(() ->
                collection.updateOne(
                        Filters.eq("uuid", uuid.toString()),
                        new Document("$set", new Document("unlocked_power", power)),
                        new UpdateOptions().upsert(true)
                )
        );
    }
}