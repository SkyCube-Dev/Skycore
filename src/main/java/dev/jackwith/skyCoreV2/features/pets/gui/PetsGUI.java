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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class PetsGUI {

    private static final int SLOTS_PER_PAGE = 3;
    private static final List<Integer> STORAGE_SLOTS = Arrays.asList(
            28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43
    );
    private static final int ITEMS_PER_PAGE = STORAGE_SLOTS.size();

    public static Inventory create(Player player, int equippedPage, int storagePage) {
        Inventory gui = Bukkit.createInventory(new PetsHolder(), 54, color("<italic:false>Pets"));

        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, pane);

        UUID uuid = player.getUniqueId();
        PetService service = SkyCore.getPetService();
        int petSlots = SkyCore.getPetsCollection().getPetSlots(uuid);
        List<String> equipped = service.getEquippedPets(uuid);

        int totalDisplaySlots = 3 + Math.max(0, petSlots - 3);

        int equippedStart = (equippedPage - 1) * SLOTS_PER_PAGE;
        int equippedEnd = Math.min(equippedStart + SLOTS_PER_PAGE, totalDisplaySlots);

        int equippedIndex = equippedStart;

        for (int i = equippedStart; i < equippedEnd; i++) {
            int guiSlot = 12 + (i - equippedStart);

            if (i == 2) {
                if (!player.hasPermission("skycore.rank.vip")) {
                    gui.setItem(guiSlot, createLockedRankSlot());
                } else if (equipped.size() > 2) {
                    gui.setItem(guiSlot, createPetItem(service.getPet(equipped.get(2))));
                } else {
                    gui.setItem(guiSlot, createEmptySlot());
                }
            } else {
                int actualEquippedIndex = (!player.hasPermission("skycore.rank.vip") && i > 2)
                        ? i - 1
                        : i;

                if (actualEquippedIndex < equipped.size()) {
                    gui.setItem(guiSlot, createPetItem(service.getPet(equipped.get(actualEquippedIndex))));
                } else if (i < petSlots) {
                    gui.setItem(guiSlot, createEmptySlot());
                }
            }
        }

        List<Pet> storagePets = getAllPetsForStorage(uuid, service.getOwnedPets(uuid), equipped, service.getSort(uuid));
        int storageStart = (storagePage - 1) * ITEMS_PER_PAGE;
        for (int i = 0; i < STORAGE_SLOTS.size(); i++) {
            int index = storageStart + i;
            gui.setItem(STORAGE_SLOTS.get(i), (index < storagePets.size()) ? createPetItem(storagePets.get(index)) : new ItemStack(Material.AIR));
        }

        gui.setItem(51, createUnequipAll());
        gui.setItem(49, createSortingHopper(service.getSort(uuid)));

        int totalEquippedPages = (int) Math.ceil((double) petSlots / SLOTS_PER_PAGE);
        if (equippedPage > 1) gui.setItem(11, createArrow("<yellow><bold>«</bold> Prev Slots", -1, equippedPage, true));
        if (equippedPage < totalEquippedPages) gui.setItem(15, createArrow("<yellow><bold>»</bold> Next Slots", 1, equippedPage, true));

        int totalStoragePages = (int) Math.ceil((double) storagePets.size() / ITEMS_PER_PAGE);
        if (storagePage > 1) gui.setItem(48, createArrow("<yellow><bold>«</bold> Prev", -1, storagePage, false));
        if (storagePage < totalStoragePages) gui.setItem(50, createArrow("<yellow><bold>»</bold> Next", 1, storagePage, false));

        return gui;
    }

    public static Inventory create(Player player, int page) {
        return create(player, 1, page);
    }

    private static List<Pet> getAllPetsForStorage(UUID uuid, List<String> owned, List<String> equipped, SortType sort) {
        List<Pet> available = new ArrayList<>();
        PetService service = SkyCore.getPetService();

        for (String petId : owned) {
            if (equipped.contains(petId)) continue;
            Pet pet = service.getPet(petId);
            if (pet != null) available.add(pet);
        }

        available.sort((p1, p2) -> {
            return switch (sort) {
                case HIGHEST_BOOST -> Integer.compare(p2.boost(), p1.boost());
                case LOWEST_BOOST -> Integer.compare(p1.boost(), p2.boost());
                case NEWEST ->
                        Long.compare(service.getOwnedDates(uuid, p2.id()), service.getOwnedDates(uuid, p1.id()));
                case OLDEST ->
                        Long.compare(service.getOwnedDates(uuid, p1.id()), service.getOwnedDates(uuid, p2.id()));
            };
        });

        return available;
    }

    private static ItemStack createArrow(String name, int direction, int currentPage, boolean isEquipped) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        meta.displayName(MiniMessage.miniMessage().deserialize(name));

        NamespacedKey dirKey = new NamespacedKey(SkyCore.getInstance(), "pagination_direction");
        NamespacedKey pageKey = new NamespacedKey(SkyCore.getInstance(), "current_page");
        NamespacedKey typeKey = new NamespacedKey(SkyCore.getInstance(), "pagination_type");

        meta.getPersistentDataContainer().set(dirKey, PersistentDataType.INTEGER, direction);
        meta.getPersistentDataContainer().set(pageKey, PersistentDataType.INTEGER, currentPage);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, isEquipped ? "equipped" : "storage");

        arrow.setItemMeta(meta);
        return arrow;
    }

    private static ItemStack createPetItem(Pet pet) {
        ItemStack item = new ItemStack(pet.material());
        ItemMeta meta = item.getItemMeta();

        MiniMessage miniMessage = MiniMessage.miniMessage();

        String displayNameText = "<italic:false>" + pet.name() + "<reset><italic:false> <#A8A8A8>| <#2CFC65>+" + pet.boost() + "% Sell Boost ⚡";
        Component displayName = miniMessage.deserialize(displayNameText);
        meta.displayName(displayName);

        String rarityText = SkyCore.getPetsConfig().getString("rarities." + pet.rarity().toLowerCase(), "&cUNKNOWN");
        Component rarityLine = miniMessage.deserialize("<italic:false>" + rarityText);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(""));
        lore.add(rarityLine);

        lore.add(Component.text(""));
        lore.add(miniMessage.deserialize("<italic:false><red>\uD83D\uDCB0 <b><gradient:#FC5C5D:#FDB95D:#72F761:#4EDEFF>EARNINGS:</gradient></b> <dark_green>$<green>0"));

        meta.lore(lore);

        NamespacedKey key = new NamespacedKey(SkyCore.getInstance(), "pet_id");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, pet.id());

        meta.setCustomModelData(pet.modelData());
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