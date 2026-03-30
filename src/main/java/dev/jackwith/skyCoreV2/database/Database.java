package dev.jackwith.skyCoreV2.database;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bukkit.configuration.file.FileConfiguration;

public class Database {

    private MongoClient client;
    private MongoDatabase database;

    public Database() {
        FileConfiguration config = SkyCore.getConfiguration();
        connect(config.getString("uri"));
    }

    public void connect(String uri) {
        if (client != null) return;

        this.client = MongoClients.create(uri);
        this.database = client.getDatabase("skycore");
    }

    public void disconnect() {
        if (client != null) {
            client.close();
            client = null;
            database = null;
        }
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}
