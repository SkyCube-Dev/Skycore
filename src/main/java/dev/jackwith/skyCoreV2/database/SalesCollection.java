package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.UpdateOptions;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SalesCollection {

    private final MongoCollection<Document> sales;
    private final MongoCollection<Document> multipliers;

    public SalesCollection() {
        MongoDatabase db = SkyCore.getDatabase().getDatabase();
        this.sales = db.getCollection("sales");
        this.multipliers = db.getCollection("sell_multipliers");

        sales.createIndex(new Document("uuid", 1));
        sales.createIndex(new Document("timestamp", -1));
        multipliers.createIndex(new Document("uuid", 1), new IndexOptions().unique(true));
    }

    public double getPlayerSellBoost(UUID uuid) {
        Document doc = multipliers.find(Filters.eq("uuid", uuid.toString())).first();
        return doc != null ? doc.getDouble("multiplier") : 1.0;
    }

    public void setPlayerSellBoost(UUID uuid, double boost) {
        CompletableFuture.runAsync(() ->
                multipliers.updateOne(
                        Filters.eq("uuid", uuid.toString()),
                        new Document("$set", new Document("multiplier", boost)),
                        new UpdateOptions().upsert(true)
                )
        );
    }

    public void logSale(UUID uuid, String shopId, String itemId, String material,
                        int amount, double basePrice, double finalPrice) {
        CompletableFuture.runAsync(() ->
                sales.insertOne(new Document("uuid", uuid.toString())
                        .append("shop_id", shopId)
                        .append("item_id", itemId)
                        .append("material", material)
                        .append("amount", amount)
                        .append("base_price", basePrice)
                        .append("final_price", finalPrice)
                        .append("timestamp", System.currentTimeMillis()))
        );
    }
}