// This includes all base files for pets needed for them to actual work
// and the code used to apply sell multipliers

// 3/7/26 | PetsSource
// 3/7/26 | Jackw

package dev.jackwith.skyCoreV2.features.pets;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.PetsDB;
import dev.jackwith.skyCoreV2.features.pets.gui.SortType;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetService {

    private final PetsDB db;
    private final Map<String, Pet> registry = new ConcurrentHashMap<>();

    private final Map<UUID, SortType> sortCache = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> dateCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> equippedCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> ownedCache = new ConcurrentHashMap<>();

    public PetService(PetsDB db) {
        this.db = db;
        loadRegistry();
    }

    public void loadRegistry() {
        registry.clear();
        ConfigurationSection section = SkyCore.getInstance().getPetConfig().getConfigurationSection("pets");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                String name = section.getString(key + ".name");
                int boost = section.getInt(key + ".boost");
                String rarity = section.getString(key + ".rarity");
                String modelName = section.getString(key + ".model.modelname");
                Material mat = Material.valueOf(section.getString(key + ".item.item", "STONE").toUpperCase());
                int modelData = section.getInt(key + ".item.modelData");

                registry.put(key, new Pet(key, name, boost, rarity, mat, modelData, modelName));
            } catch (Exception e) {
                SkyCore.getInstance().getLogger().warning("Failed to load pet '" + key + "': " + e.getMessage());
            }
        }
    }

    public Pet getPet(String id) { return registry.get(id); }
    public Collection<Pet> getAllPets() { return registry.values(); }

    public boolean hasPet(UUID uuid, String petId) {
        return getOwnedPets(uuid).contains(petId);
    }



    public void givePet(UUID uuid, String petId) {
        db.addPet(uuid, petId);
        dateCache.computeIfAbsent(uuid, k -> new HashMap<>()).put(petId, System.currentTimeMillis());
        ownedCache.computeIfAbsent(uuid, k -> new ArrayList<>()).add(petId);
    }

    public void removePet(UUID uuid, String petId) {
        if (getEquippedPets(uuid).contains(petId)) {
            unequipPet(uuid, petId);
        }

        db.deletePet(uuid, petId);
        Optional.ofNullable(ownedCache.get(uuid)).ifPresent(list -> list.remove(petId));
        Optional.ofNullable(dateCache.get(uuid)).ifPresent(map -> map.remove(petId));
    }

    public boolean equipPet(UUID uuid, String petId) {
        List<String> equipped = getEquippedPets(uuid);
        if (equipped.contains(petId)) return false;

        Player player = Bukkit.getPlayer(uuid);
        boolean isVip = player != null && player.hasPermission("skycore.rank.vip");
        int maxSlots = isVip ? 3 : 2;

        if (equipped.size() >= maxSlots) {
            if (player != null && !isVip) {
                player.sendMessage(MiniMessage.miniMessage().deserialize("<red>You need <white>\uE306<red> rank to equip a 3rd pet!"));
            }
            return false;
        }

        db.setEquipped(uuid, petId, true);
        equipped.add(petId);

        Pet pet = getPet(petId);
        if (player != null && pet != null && pet.modelName() != null) {
            SkyCore.getInstance().getModelManager().spawnPetModel(player, pet.modelName());
        }
        return true;
    }

    public void unequipPet(UUID uuid, String petId) {
        db.setEquipped(uuid, petId, false);
        equippedCache.computeIfPresent(uuid, (k, v) -> { v.remove(petId); return v; });

        Pet pet = getPet(petId);
        if (pet != null) {
            SkyCore.getInstance().getModelManager().removeSpecificPet(uuid, pet.modelName());
        }
    }

    public void unequipAll(UUID uuid) {
        new ArrayList<>(getEquippedPets(uuid)).forEach(id -> unequipPet(uuid, id));
        db.unequipAll(uuid);
        equippedCache.remove(uuid);
    }

    public List<String> getOwnedPets(UUID uuid) { return ownedCache.computeIfAbsent(uuid, db::getOwnedPets); }
    public List<String> getEquippedPets(UUID uuid) { return equippedCache.computeIfAbsent(uuid, db::getEquippedPets); }
    public SortType getSort(UUID uuid) { return sortCache.getOrDefault(uuid, SortType.HIGHEST_BOOST); }
    public void setSort(UUID uuid, SortType sort) { sortCache.put(uuid, sort); }

    public long getAcquisitionDate(UUID uuid, String petId) {
        return dateCache.computeIfAbsent(uuid, db::getAllPetAcquisitionDates).getOrDefault(petId, 0L);
    }

    public void cleanup(UUID uuid) {
        dateCache.remove(uuid);
        sortCache.remove(uuid);
        equippedCache.remove(uuid);
        ownedCache.remove(uuid);
    }
}