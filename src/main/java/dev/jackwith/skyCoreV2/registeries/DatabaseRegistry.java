package dev.jackwith.skyCoreV2.registeries;

import dev.jackwith.skyCoreV2.databases.Database;

import java.util.ArrayList;
import java.util.List;

public class DatabaseRegistry {
    private final List<Database> databases = new ArrayList<>();

    public void connectAll(Database... dbs) {
        for (Database db : dbs) {
            databases.add(db);
            db.connect();
        }
    }

    public <T extends Database> T get(Class<T> type) {
        return databases.stream()
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Database not registered: " + type.getSimpleName()));
    }

    public void closeAll() {
        databases.forEach(Database::close);
    }
}