package dev.jackwith.skyCoreV2.features.pets.listeners;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.gui.PetsGUI;
import dev.jackwith.skyCoreV2.features.pets.gui.PetsHolder;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PetListener implements Listener {

    private final PetService service;
    private final ConcurrentHashMap<UUID, PaginationState> paginationState = new ConcurrentHashMap<>();

    private static class PaginationState {
        int equippedPage = 1;
        int storagePage = 1;
    }

    public PetListener(PetService service) {
        this.service = service;
    }

    private void sendLang(Player player, String key, String... placeholders) {
        var config = SkyCore.getLangConfig();
        String message = config.getString("pets." + key, key);

        if (placeholders != null) {
            for (int i = 0; i < placeholders.length - 1; i += 2) {
                message = message.replace(placeholders[i], placeholders[i + 1]);
            }
        }

        boolean usePrefix = config.getBoolean("pets.usePrefix", true);
        String prefix = config.getString("pets.prefix", "");

        player.sendMessage(MiniMessage.miniMessage().deserialize((usePrefix ? prefix : "") + message));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        SkyCore.getPetsCollection().createPlayer(e.getPlayer().getUniqueId().toString());
        paginationState.put(e.getPlayer().getUniqueId(), new PaginationState());

        Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
            Bukkit.getScheduler().runTask(SkyCore.getInstance(), () -> refreshModels(e.getPlayer()));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cleanupPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
            Bukkit.getScheduler().runTaskLater(SkyCore.getInstance(), () -> refreshModels(e.getPlayer()), 5L);
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PetsHolder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        PaginationState state = paginationState.getOrDefault(uuid, new PaginationState());

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        if (item.getType() == Material.TNT) {
            service.unequipAll(uuid);
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            sendLang(player, "unequip-all");

            Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
                Bukkit.getScheduler().runTask(SkyCore.getInstance(), () -> {
                    refreshModels(player);
                    state.equippedPage = 1;
                    state.storagePage = 1;
                    player.openInventory(PetsGUI.create(player, state.equippedPage, state.storagePage));
                });
            });
            return;
        }

        if (item.getType() == Material.ARROW) {
            handlePagination(player, item, state);
            return;
        }

        if (item.getType() == Material.HOPPER) {
            service.setSort(uuid, service.getSort(uuid).next());
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.openInventory(PetsGUI.create(player, state.equippedPage, state.storagePage));
            return;
        }


        handlePetClick(player, item, state);
    }

    private void handlePagination(Player player, ItemStack item, PaginationState state) {
        var pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey dirKey = new NamespacedKey(SkyCore.getInstance(), "pagination_direction");
        NamespacedKey pageKey = new NamespacedKey(SkyCore.getInstance(), "current_page");
        NamespacedKey typeKey = new NamespacedKey(SkyCore.getInstance(), "pagination_type");

        if (pdc.has(dirKey, PersistentDataType.INTEGER) && pdc.has(pageKey, PersistentDataType.INTEGER)) {
            int direction = pdc.get(dirKey, PersistentDataType.INTEGER);
            int currentPage = pdc.get(pageKey, PersistentDataType.INTEGER);
            String type = pdc.has(typeKey, PersistentDataType.STRING) ?
                    pdc.get(typeKey, PersistentDataType.STRING) : "storage";

            int newPage = currentPage + direction;

            if (type.equals("equipped")) {
                state.equippedPage = newPage;
            } else {
                state.storagePage = newPage;
            }

            player.openInventory(PetsGUI.create(player, state.equippedPage, state.storagePage));
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
        }
    }

    private void handlePetClick(Player player, ItemStack item, PaginationState state) {
        UUID uuid = player.getUniqueId();
        NamespacedKey petKey = new NamespacedKey(SkyCore.getInstance(), "pet_id");
        String petId = item.getItemMeta().getPersistentDataContainer().get(petKey, PersistentDataType.STRING);

        if (petId == null) return;

        Pet pet = service.getPet(petId);
        String petName = (pet != null) ? pet.name() : petId;

        List<String> equipped = service.getEquippedPets(uuid);

        if (equipped.contains(petId)) {
            service.unequipPet(uuid, petId);
            sendLang(player, "pet-unequip", "<pet>", petName);

            Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
                Bukkit.getScheduler().runTask(SkyCore.getInstance(), () -> {
                    SkyCore.getModelManager().removeSpecificPet(uuid, petId);
                    player.openInventory(PetsGUI.create(player, state.equippedPage, state.storagePage));
                });
            });
        } else {
            if (!service.equipPet(uuid, petId)) {
                sendLang(player, "max-pets-reached", "<amount>", String.valueOf(service.getPetSlots(player)));
                return;
            }
            sendLang(player, "pet-equip", "<pet>", petName);

            Bukkit.getScheduler().runTaskAsynchronously(SkyCore.getInstance(), () -> {
                if (pet != null) {
                    Bukkit.getScheduler().runTask(SkyCore.getInstance(), () -> {
                        refreshModels(player);
                        player.openInventory(PetsGUI.create(player, state.equippedPage, state.storagePage));
                    });
                }
            });
        }
    }

    private void refreshModels(Player player) {
        UUID uuid = player.getUniqueId();
        SkyCore.getModelManager().removeAllPets(uuid);

        service.getEquippedPets(uuid).forEach(id -> {
            Pet pet = service.getPet(id);
            if (pet != null) {
                SkyCore.getModelManager().spawnPetModel(player, pet.modelName());
            }
        });
    }

    private void cleanupPlayer(UUID uuid) {
        SkyCore.getModelManager().removeAllPets(uuid);
        service.cleanup(uuid);
        paginationState.remove(uuid);
    }
}