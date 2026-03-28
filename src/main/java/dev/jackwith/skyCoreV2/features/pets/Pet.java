// This includes all base files for pets needed for them to actual work
// and the code used to apply sell multipliers

// 3/7/26 | PetsSource
// 3/7/26 | Jackw


package dev.jackwith.skyCoreV2.features.pets;

import org.bukkit.Material;

public class Pet {

    private final String id;
    private final String name;
    private final int boost;
    private final String rarity;
    private final Material material;
    private final int modelData;
    private final String modelName;

    public Pet(String id, String name, int boost, String rarity, Material material, int modelData, String modelName) {
        this.id = id;
        this.name = name;
        this.boost = boost;
        this.rarity = rarity;
        this.material = material;
        this.modelData = modelData;
        this.modelName = modelName;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getBoost() { return boost; }
    public String getRarity() { return rarity; }
    public Material getMaterial() { return material; }
    public int getModelData() { return modelData; }
    public String getModelName() { return modelName; }
}