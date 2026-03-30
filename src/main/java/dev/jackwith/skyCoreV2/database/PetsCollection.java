package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bson.Document;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class PetsCollection {

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

    public PetsCollection() {
        Database db = SkyCore.getDatabase();
        MongoDatabase mongoDatabase = db.getDatabase();
        this.collection = mongoDatabase.getCollection("pets");
        this.collection.createIndex(new Document("uuid", 1));
    }

    public void createPlayer(String uuid) {
        Document existing = getPlayerData(uuid);
        if (existing == null) {
            Document doc = new Document("uuid", uuid)
                    .append("pets", new ArrayList<>())
                    .append("slots", 3);

            cache.put(uuid, new CachedData(doc));

            CompletableFuture.runAsync(() -> {
                collection.insertOne(doc);
            });
        }
    }

    // Helpers

    private Document getPlayerData(String uuid) {
        CachedData cached = cache.get(uuid);
        if (cached != null && !cached.isExpired()) {
            return cached.doc;
        }

        Document doc = collection.find(Filters.eq("uuid", uuid)).first();
        if (doc != null) {
            cache.put(uuid, new CachedData(doc));
        }

        return doc;
    }

    private void removeCache(String uuid) {
        cache.remove(uuid);
    }

    // Pets

    public void addPet(UUID uuid, String petId) {
        String uuidString = uuid.toString();
        Document playerDoc = getPlayerData(uuidString);

        if (playerDoc != null) {
            Document petDoc = new Document("id", petId)
                    .append("equipped", false)
                    .append("obtained", System.currentTimeMillis());

            List<Document> pets = playerDoc.getList("pets", Document.class);
            if (pets != null && pets.stream().noneMatch(p -> p.getString("id").equals(petId))) {
                pets.add(petDoc);
                cache.put(uuidString, new CachedData(playerDoc));
            }

            CompletableFuture.runAsync(() -> {
                collection.updateOne(
                        Filters.eq("uuid", uuidString),
                        new Document("$addToSet", new Document("pets", petDoc))
                );
                removeCache(uuidString);
            });
        }
    }

    public void deletePet(UUID uuid, String petId) {
        String uuidString = uuid.toString();
        Document playerDoc = getPlayerData(uuidString);

        if (playerDoc != null) {
            List<Document> pets = playerDoc.getList("pets", Document.class);
            if (pets != null) {
                pets.removeIf(p -> p.getString("id").equals(petId));
                cache.put(uuidString, new CachedData(playerDoc));
            }

            CompletableFuture.runAsync(() -> {
                collection.updateOne(
                        Filters.eq("uuid", uuidString),
                        new Document("$pull", new Document("pets", new Document("id", petId)))
                );
                removeCache(uuidString);
            });
        }
    }

    public List<String> getOwnedPets(UUID uuid) {
        Document doc = getPlayerData(uuid.toString());
        if (doc == null) return new ArrayList<>();

        List<Document> pets = doc.getList("pets", Document.class);
        return pets != null ? pets.stream()
                .map(p -> p.getString("id"))
                .collect(Collectors.toList()) : new ArrayList<>();
    }

    public List<String> getEquippedPets(UUID uuid) {
        Document doc = getPlayerData(uuid.toString());
        if (doc == null) return new ArrayList<>();

        List<Document> pets = doc.getList("pets", Document.class);
        return pets != null ? pets.stream()
                .filter(p -> p.getBoolean("equipped", false))
                .map(p -> p.getString("id"))
                .collect(Collectors.toList()) : new ArrayList<>();
    }

    public Map<String, Long> getAllPetAcquisitionDates(UUID uuid) {
        Document doc = getPlayerData(uuid.toString());
        if (doc == null) return new HashMap<>();

        List<Document> pets = doc.getList("pets", Document.class);
        if (pets == null) return new HashMap<>();

        return pets.stream()
                .collect(Collectors.toMap(
                        p -> p.getString("id"),
                        p -> p.getLong("obtained")
                ));
    }

    public void setEquipped(UUID uuid, String petId, boolean equipped) {
        String uuidString = uuid.toString();
        Document playerDoc = getPlayerData(uuidString);

        if (playerDoc != null) {
            List<Document> pets = playerDoc.getList("pets", Document.class);
            if (pets != null) {
                pets.stream()
                        .filter(p -> p.getString("id").equals(petId))
                        .findFirst()
                        .ifPresent(p -> p.put("equipped", equipped));
                cache.put(uuidString, new CachedData(playerDoc));
            }

            CompletableFuture.runAsync(() -> {
                collection.updateOne(
                        Filters.and(
                                Filters.eq("uuid", uuidString),
                                Filters.eq("pets.id", petId)
                        ),
                        new Document("$set", new Document("pets.$.equipped", equipped))
                );
                removeCache(uuidString);
            });
        }
    }

    public void unequipAll(UUID uuid) {
        String uuidString = uuid.toString();
        Document playerDoc = getPlayerData(uuidString);

        if (playerDoc != null) {
            List<Document> pets = playerDoc.getList("pets", Document.class);
            if (pets != null) {
                pets.forEach(p -> p.put("equipped", false));
                cache.put(uuidString, new CachedData(playerDoc));
            }

            CompletableFuture.runAsync(() -> {
                collection.updateOne(
                        Filters.eq("uuid", uuidString),
                        new Document("$set", new Document("pets.$[].equipped", false))
                );
                removeCache(uuidString);
            });
        }
    }

    public int getPetSlots(UUID uuid) {
        Document doc = getPlayerData(uuid.toString());
        return doc != null ? doc.getInteger("slots", 2) : 2;
    }

    public void setPetSlots(UUID uuid, int slots) {
        String uuidString = uuid.toString();
        Document playerDoc = getPlayerData(uuidString);

        if (playerDoc != null) {
            playerDoc.put("pet_slots", slots);
            cache.put(uuidString, new CachedData(playerDoc));
        }

        CompletableFuture.runAsync(() -> {
            collection.updateOne(
                    Filters.eq("uuid", uuidString),
                    new Document("$set", new Document("slots", slots))
            );
            removeCache(uuidString);
        });
    }

    public void addPetSlot(UUID uuid) {
        int current = getPetSlots(uuid);
        setPetSlots(uuid, current + 1);
    }

    public boolean canEquipPet(UUID uuid) {
        List<String> equipped = getEquippedPets(uuid);
        int slots = getPetSlots(uuid);
        return equipped.size() < slots;
    }
}