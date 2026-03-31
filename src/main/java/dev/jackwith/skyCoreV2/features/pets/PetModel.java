package dev.jackwith.skyCoreV2.features.pets;

import com.ticxo.modelengine.api.ModelEngineAPI;
import com.ticxo.modelengine.api.model.ActiveModel;
import com.ticxo.modelengine.api.model.ModeledEntity;
import dev.jackwith.skyCoreV2.SkyCore;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetModel {

    private final Map<UUID, List<ModeledEntity>> activePets = new ConcurrentHashMap<>();

    public void spawnPetModel(Player player, String modelId) {
        ArmorStand stand = player.getWorld().spawn(player.getLocation(), ArmorStand.class, entity -> {
            entity.setInvisible(true);
            entity.setGravity(false);
            entity.setCollidable(false);
            entity.setInvulnerable(true);
            entity.setMetadata("PetEntity", new FixedMetadataValue(SkyCore.getInstance(), player.getUniqueId().toString()));
        });

        ModeledEntity modeled = ModelEngineAPI.createModeledEntity(stand);
        ActiveModel model = ModelEngineAPI.createActiveModel(modelId);

        if (model == null) {
            stand.remove();
            return;
        }

        modeled.addModel(model, true);
        modeled.setBaseEntityVisible(false);

        activePets.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(modeled);
    }

    public void updatePositions() {
        double bobbing = Math.sin(System.currentTimeMillis() * 0.005) * 0.08;

        for (UUID uuid : activePets.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) {
                removeAllPets(uuid);
                continue;
            }

            List<ModeledEntity> pets = activePets.get(uuid);
            Location pLoc = player.getLocation();
            Vector dir = pLoc.getDirection().setY(0).normalize().multiply(-1);

            List<List<ModeledEntity>> rows = new ArrayList<>();
            int remaining = pets.size();
            int index = 0;

            int firstRowSize = Math.min(3, remaining);
            rows.add(pets.subList(index, index + firstRowSize));
            index += firstRowSize;
            remaining -= firstRowSize;

            if (remaining > 0) {
                int secondRowSize = Math.min(6, remaining);
                rows.add(pets.subList(index, index + secondRowSize));
                index += secondRowSize;
                remaining -= secondRowSize;
            }

            while (remaining > 0) {
                int rowSize = Math.min(3, remaining);
                rows.add(pets.subList(index, index + rowSize));
                index += rowSize;
                remaining -= rowSize;
            }

            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                List<ModeledEntity> row = rows.get(rowIndex);
                int size = row.size();

                double rowOffset = 1.2 + (rowIndex * 1.1);
                Vector rowDir = dir.clone().multiply(rowOffset);

                double arcSpread = (size <= 3) ? 55 : 45;

                for (int i = 0; i < size; i++) {
                    ModeledEntity modeled = row.get(i);
                    if (!(modeled.getBase().getOriginal() instanceof ArmorStand stand)) continue;

                    double angleRad = Math.toRadians((i - (size - 1) / 2.0) * arcSpread);
                    double x = rowDir.getX() * Math.cos(angleRad) - rowDir.getZ() * Math.sin(angleRad);
                    double z = rowDir.getX() * Math.sin(angleRad) + rowDir.getZ() * Math.cos(angleRad);

                    Location target = pLoc.clone().add(x, 0.1 + bobbing, z);
                    float yaw = (float) (Math.atan2(pLoc.getZ() - target.getZ(), pLoc.getX() - target.getX()) * 57.2957795D) - 90F;
                    target.setYaw(yaw);
                    target.setPitch(0);

                    Location standLoc = stand.getLocation();
                    if (!standLoc.getWorld().equals(pLoc.getWorld())) {
                        stand.teleport(target);
                    } else {
                        if (standLoc.distanceSquared(target) > 0.005) {
                            stand.teleportAsync(target).thenRun(() -> {
                                Bukkit.getScheduler().runTask(SkyCore.getInstance(), () -> {
                                    modeled.setYHeadRot(yaw);
                                    modeled.setYBodyRot(yaw);
                                });
                            });
                        }
                    }
                }
            }
        }
    }

    public void removeSpecificPet(UUID uuid, String modelId) {
        List<ModeledEntity> pets = activePets.get(uuid);
        if (pets == null) return;

        List<ModeledEntity> toRemove = new ArrayList<>();
        for (ModeledEntity modeled : new ArrayList<>(pets)) {
            boolean match = modeled.getModels().values().stream()
                    .anyMatch(m -> m.getBlueprint().getName().equalsIgnoreCase(modelId));
            if (match) toRemove.add(modeled);
        }

        pets.removeAll(toRemove);

        for (ModeledEntity modeled : toRemove) {
            try {
                if (modeled.getBase().getOriginal() instanceof ArmorStand s) s.remove();
                Bukkit.getScheduler().runTask(SkyCore.getInstance(), modeled::destroy);
            } catch (Exception e) {
                SkyCore.getInstance().getLogger().warning("Failed to remove specific pet model: " + e.getMessage());
            }
        }
    }

    public void removeAllPets(UUID uuid) {
        List<ModeledEntity> pets = this.activePets.remove(uuid);
        if (pets == null) return;

        List<ModeledEntity> copy = new ArrayList<>(pets);

        for (ModeledEntity m : copy) {
            try {
                if (m.getBase().getOriginal() instanceof ArmorStand s) {
                    s.remove();
                }
                Bukkit.getScheduler().runTask(SkyCore.getInstance(), m::destroy);
            } catch (Exception e) {
                SkyCore.getInstance().getLogger().warning("Failed to remove pet model: " + e.getMessage());
            }
        }
    }
}