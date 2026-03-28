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

import java.util.UUID;

public class PetListener implements Listener {

    private final PetService service;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PetListener(PetService service) {
        this.service = service;
    }

    private void sendLang(Player player, String key, String... placeholders) {
        var config = SkyCore.getInstance().getLang();

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
        Bukkit.getScheduler().runTaskLater(SkyCore.getInstance(), () -> refreshModels(e.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cleanupPlayer(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTaskLater(SkyCore.getInstance(), () -> refreshModels(e.getPlayer()), 5L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof PetsHolder)) return;
        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player player)) return;

        ItemStack item = e.getCurrentItem();
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return;

        UUID uuid = player.getUniqueId();

        if (item.getType() == Material.TNT) {
            service.unequipAll(uuid);
            refreshModels(player);
            sendLang(player, "unequip-all");
            player.openInventory(PetsGUI.create(player, 1));
            return;
        }

        if (item.getType() == Material.ARROW) {
            var pdc = item.getItemMeta().getPersistentDataContainer();
            NamespacedKey dirKey = new NamespacedKey(SkyCore.getInstance(), "pagination_direction");
            NamespacedKey pageKey = new NamespacedKey(SkyCore.getInstance(), "current_page");

            if (pdc.has(dirKey, PersistentDataType.INTEGER) && pdc.has(pageKey, PersistentDataType.INTEGER)) {
                int direction = pdc.get(dirKey, PersistentDataType.INTEGER);
                int currentPage = pdc.get(pageKey, PersistentDataType.INTEGER);

                int newPage = currentPage + direction;
                player.openInventory(PetsGUI.create(player, newPage));
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            }
            return;
        }

        if (item.getType() == Material.HOPPER) {
            service.setSort(uuid, service.getSort(uuid).next());
            player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
            player.openInventory(PetsGUI.create(player, 1));
            return;
        }

        NamespacedKey petKey = new NamespacedKey(SkyCore.getInstance(), "pet_id");
        String petId = item.getItemMeta().getPersistentDataContainer().get(petKey, PersistentDataType.STRING);

        if (petId != null) {
            Pet pet = service.getPet(petId);
            String petName = (pet != null) ? pet.getName() : petId;

            if (service.getEquippedPets(uuid).contains(petId)) {
                service.unequipPet(uuid, petId);
                sendLang(player, "pet-unequip", "<pet>", petName);
            } else {
                if (!service.equipPet(uuid, petId)) {
                    sendLang(player, "max-pets-reached");
                    return;
                }
                sendLang(player, "pet-equip", "<pet>", petName);
            }

            refreshModels(player);
            player.openInventory(PetsGUI.create(player, 1));
        }
    }

    private void refreshModels(Player player) {
        UUID uuid = player.getUniqueId();
        SkyCore.getInstance().getModelManager().removeAllPets(uuid);

        service.getEquippedPets(uuid).forEach(id -> {
            Pet pet = service.getPet(id);
            if (pet != null) {
                SkyCore.getInstance().getModelManager().spawnPetModel(player, pet.getModelName());
            }
        });
    }

    private void cleanupPlayer(UUID uuid) {
        SkyCore.getInstance().getModelManager().removeAllPets(uuid);
        service.cleanup(uuid);
    }
}