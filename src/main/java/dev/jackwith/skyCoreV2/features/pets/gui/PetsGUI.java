// This includes all base files for pets needed for them to actual work
// and the code used to apply sell multipliers

// 3/7/26 | PetsSource
// 3/7/26 | Jackw

package dev.jackwith.skyCoreV2.features.pets.gui;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.features.pets.Pet;
import dev.jackwith.skyCoreV2.features.pets.PetService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class PetsGUI {

    private static final int[] EQUIP_SLOTS = {12, 13, 14};
    private static final int UNEQUIP_SLOT = 15;

    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    );

    private static final int ITEMS_PER_PAGE = STORAGE_SLOTS.size();

    public static Inventory create(Player player, int page) {
        Inventory gui = Bukkit.createInventory(new PetsHolder(), 54, color("<italic:false>Pets"));

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, pane);

        UUID uuid = player.getUniqueId();
        PetService service = SkyCore.getInstance().getPetService();
        List<Pet> pets = getAllPetsForStorage(uuid, service.getOwnedPets(uuid), service.getEquippedPets(uuid), service.getSort(uuid));
        List<String> equipped = service.getEquippedPets(uuid);

        for (int i = 0; i < EQUIP_SLOTS.length; i++) {
            if (i == 2 && !player.hasPermission("skycore.rank.vip")) {
                gui.setItem(EQUIP_SLOTS[i], createLockedRankSlot());
            } else if (i < equipped.size()) {
                gui.setItem(EQUIP_SLOTS[i], createPetItem(SkyCore.getInstance().getPetService().getPet(equipped.get(i))));
            } else {
                gui.setItem(EQUIP_SLOTS[i], createEmptySlot());
            }
        }

        int start = (page - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < STORAGE_SLOTS.size(); i++) {
            int index = start + i;
            gui.setItem(STORAGE_SLOTS.get(i), (index < pets.size()) ? createPetItem(pets.get(index)) : new ItemStack(Material.AIR));
        }

        gui.setItem(UNEQUIP_SLOT, createUnequipAll());
        gui.setItem(49, createSortingHopper(service.getSort(uuid)));

        int totalPages = (int) Math.ceil((double) pets.size() / ITEMS_PER_PAGE);
        if (page > 1) gui.setItem(48, createArrow("<yellow><bold>«</bold> Prev", -1, page));
        if (page < totalPages) gui.setItem(50, createArrow("<yellow><bold>»</bold> Next", 1, page));

        return gui;
    }

    private static List<Pet> getAllPetsForStorage(UUID uuid, List<String> owned, List<String> equipped, SortType sort) {
        List<Pet> available = new ArrayList<>();
        PetService service = SkyCore.getInstance().getPetService();

        for (String petId : owned) {
            if (equipped.contains(petId)) continue;
            Pet pet = SkyCore.getInstance().getPetService().getPet(petId);
            if (pet != null) available.add(pet);
        }

        available.sort((p1, p2) -> {
            return switch (sort) {
                case HIGHEST_BOOST -> Integer.compare(p2.getBoost(), p1.getBoost());
                case LOWEST_BOOST -> Integer.compare(p1.getBoost(), p2.getBoost());
                case NEWEST ->
                        Long.compare(service.getAcquisitionDate(uuid, p2.getId()), service.getAcquisitionDate(uuid, p1.getId()));
                case OLDEST ->
                        Long.compare(service.getAcquisitionDate(uuid, p1.getId()), service.getAcquisitionDate(uuid, p2.getId()));
            };
        });

        return available;
    }

    private static ItemStack createArrow(String name, int direction, int currentPage) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));

        NamespacedKey dirKey = new NamespacedKey(SkyCore.getInstance(), "pagination_direction");
        NamespacedKey pageKey = new NamespacedKey(SkyCore.getInstance(), "current_page");

        meta.getPersistentDataContainer().set(dirKey, PersistentDataType.INTEGER, direction);
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, currentPage);

        arrow.setItemMeta(meta);
        return arrow;
    }

    private static ItemStack createPetItem(Pet pet) {
        ItemStack item = new ItemStack(pet.getMaterial());
        ItemMeta meta = item.getItemMeta();

        MiniMessage miniMessage = MiniMessage.miniMessage();

        String displayNameText = "<italic:false>" + pet.getName() + "<reset><italic:false> <#A8A8A8>| <#2CFC65>+" + pet.getBoost() + "% Sell Boost ⚡";
        Component displayName = miniMessage.deserialize(displayNameText);
        meta.displayName(displayName);

        String rarityText = SkyCore.getInstance().getPetConfig().getString("rarities." + pet.getRarity().toLowerCase(), "&cUNKNOWN");
        Component rarityLine = miniMessage.deserialize("<italic:false>" + rarityText);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(rarityLine);

        lore.add(Component.text(""));
        lore.add(miniMessage.deserialize("<italic:false><red>\uD83D\uDCB0 <b><gradient:#FC5C5D:#FDB95D:#72F761:#4EDEFF>EARNINGS:</gradient></b> <dark_green>$<green>0"));

        meta.lore(lore);

        NamespacedKey key = new NamespacedKey(SkyCore.getInstance(), "pet_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, pet.getId());

        // Idk how to fix this deprecation
        meta.setCustomModelData(pet.getModelData());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);

        return item;
    }

    private static ItemStack createEmptySlot() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(color("<italic:false><bold><#65FC7C>OPEN PET SLOT ✔"));
        List<Component> lore = new ArrayList<>();
        lore.add(color("<italic:false><dark_gray>ᴘᴇᴛ ᴍᴀɴᴀɢᴇʀ"));
        lore.add(color(""));
        lore.add(color("<italic:false><#FCFC54>→ Click a pet below to equip"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createLockedRankSlot() {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(color("<italic:false><bold><#FC5555>LOCKED PET SLOT ✖"));

        List<Component> lore = new ArrayList<>();
        lore.add(color("<italic:false><dark_gray>ᴘᴇᴛ ᴍᴀɴᴀɢᴇʀ"));
        lore.add(color(""));
        lore.add(color("<italic:false><white>\uE306<red> ʀᴀɴᴋ ʀᴇǫᴜɪʀᴇᴅ"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createUnequipAll() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(color("<italic:false><#FC5555><bold>UNEQUIP ALL ✖"));
        List<Component> lore = new ArrayList<>();
        lore.add(color("<italic:false><dark_gray>ᴘᴇᴛ ᴍᴀɴᴀɢᴇʀ"));
        lore.add(color(""));
        lore.add(color("<italic:false><#FCFC54>→ Click to remove all pets"));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createSortingHopper(SortType current) {
        ItemStack item = new ItemStack(Material.HOPPER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(color("<italic:false><#2CCAFC><b>SORT YOUR PETS"));

        List<Component> lore = new ArrayList<>();
        lore.add(color("<italic:false><dark_gray>ᴘᴇᴛ ᴍᴀɴᴀɢᴇʀ"));
        lore.add(color(""));
        lore.add(color("<italic:false><#76FC59>Currently Selected:"));

        lore.add(color("<italic:false><green>♦ <white>" + current.getDisplayName()));

        lore.add(color(""));
        lore.add(color("<italic:false><#FCFC54>→ Click to sort pets"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Component color(String text) {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        return miniMessage.deserialize(text);
    }
}