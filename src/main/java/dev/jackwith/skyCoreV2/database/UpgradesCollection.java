package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bson.Document;

public class UpgradesCollection {

    private final MongoCollection<Document> collection;

    public UpgradesCollection() {
        Database db = SkyCore.getDatabase();
        MongoDatabase mongoDatabase = db.getDatabase();

        this.collection = mongoDatabase.getCollection("boxes");
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    public void createPlayer(String uuid) {
        Document existing = collection.find(new Document("uuid", uuid)).first();

        if (existing == null) {
            Document doc = new Document("uuid", uuid)
                    .append("level", 0)
                    .append("upgrading_until", 0L);

            collection.insertOne(doc);
        }
    }

    // Helpers

    public int getLevel(String uuid) {
        Document doc = collection.find(new Document("uuid", uuid)).first();
        if (doc != null) {
            return doc.getInteger("level", 1);
        }
        return 1;
    }

    public void setLevel(String uuid, int level) {
        collection.updateOne(
                new Document("uuid", uuid),
                new Document("$set", new Document("level", level))
        );
    }

    public long getUpgradingUntil(String uuid) {
        Document doc = collection.find(new Document("uuid", uuid)).first();
        if (doc != null) {
            return doc.getLong("upgrading_until") != null ? doc.getLong("upgrading_until") : 0L;
        }
        return 0L;
    }

    public void setUpgradingUntil(String uuid, long timestamp) {
        collection.updateOne(
                new Document("uuid", uuid),
                new Document("$set", new Document("upgrading_until", timestamp))
        );
    }
}