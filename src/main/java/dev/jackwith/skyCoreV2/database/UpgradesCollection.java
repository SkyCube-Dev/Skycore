package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bson.Document;

import java.util.concurrent.ConcurrentHashMap;

public class UpgradesCollection {

    private final MongoCollection<Document> collection;
    private final ConcurrentHashMap<String, CachedData> cache = new ConcurrentHashMap<>();

    private static class CachedData {
        Document doc;
        long timestamp;
        CachedData(Document doc) {
            this.doc = doc;
            this.timestamp = System.currentTimeMillis();
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 5000;
        }
    }

    public UpgradesCollection() {
        Database db = SkyCore.getDatabase();
        MongoDatabase mongoDatabase = db.getDatabase();

        this.collection = mongoDatabase.getCollection("boxes");
        this.collection.createIndex(new Document("uuid", 1));
    }

    public void createPlayer(String uuid) {
        Document existing = getPlayerData(uuid);

        if (existing == null) {
            Document doc = new Document("uuid", uuid)
                    .append("level", 0)
                    .append("upgrading_until", 0L);

            collection.insertOne(doc);
            cache.put(uuid, new CachedData(doc));
        }
    }

    // Helpers

    public Document getPlayerData(String uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.doc;
        }

        Document doc = collection.find(new Document("uuid", uuid)).first();

        if (doc != null) {
            cache.put(uuid, new CachedData(doc));
        }

        return doc;
    }

    public int getLevel(String uuid) {
        Document doc = getPlayerData(uuid);
        return doc != null ? doc.getInteger("level", 1) : 1;
    }

    public long getUpgradingUntil(String uuid) {
        Document doc = getPlayerData(uuid);
        return doc != null ? doc.getLong("upgrading_until") : 0L;
    }

    public void updateDocument(String uuid, int level, long timestamp) {
        collection.updateOne(
                new Document("uuid", uuid),
                new Document("$set", new Document()
                        .append("level", level)
                        .append("upgrading_until", timestamp))
        );
        removeCache(uuid);
    }

    private void removeCache(String uuid) {
        cache.remove(uuid);
    }
}