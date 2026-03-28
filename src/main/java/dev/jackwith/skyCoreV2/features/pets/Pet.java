// This includes all base files for pets needed for them to actual work
// and the code used to apply sell multipliers

// 3/7/26 | PetsSource
// 3/7/26 | Jackw


package dev.jackwith.skyCoreV2.features.pets;

import org.bukkit.Material;

public record Pet(String id, String name, int boost, String rarity, Material material, int modelData,
                  String modelName) {

}