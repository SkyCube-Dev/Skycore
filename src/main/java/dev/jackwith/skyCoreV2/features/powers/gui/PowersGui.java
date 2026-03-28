package dev.jackwith.skyCoreV2.features.powers.gui;

import dev.jackwith.skyCoreV2.SkyCore;
import dev.jackwith.skyCoreV2.databases.PowerDB;
import dev.jackwith.skyCoreV2.databases.data.PlayerPowerData;
import dev.jackwith.skyCoreV2.databases.data.PowerData;
import dev.jackwith.skyCoreV2.utils.StackTrace;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class PowersGui {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Component EMPTY = Component.empty();
    private static final int SIZE = 45;
    private static final Inventory BASE_LAYOUT = Bukkit.createInventory(new PowersHolder(), SIZE, Component.text("Powers Menu"));
    private static final ItemStack EXIT_BUTTON;

    private final SkyCore plugin = SkyCore.getInstance();
    private final Player player;
    private final Inventory inv;
    private final Map<Integer, ItemStack> cache = new HashMap<>();

    public PowersGui(Player player) {
        this.player = player;
        PowersHolder holder = new PowersHolder();
        this.inv = Bukkit.createInventory(holder, SIZE, Component.text("Powers Menu"));
        holder.setGui(this);

        inv.setContents(BASE_LAYOUT.getContents());
        refresh();
    }

    public void refresh() {
        try {
            Map<Integer, ItemStack> render = new HashMap<>();
            renderCurrentPower(render);
            renderPowers(render);
            renderStatic(render);
            applyDiff(render);
        } catch (Exception e) {
            StackTrace.error("Refresh PowersGui for " + player.getName(), e);
        }
    }

    private void renderCurrentPower(Map<Integer, ItemStack> map) {
        try {
            PowerDB db = plugin.getPowerDB();
            PlayerPowerData data = db.getCachedPlayerPower(player.getUniqueId());
            String power = data.getPower();

            ItemStack head = createHead(player, "<i:false><#54FC54>🟢 <#FC5454><b>CURRENT POWER</b>",
                    Collections.singletonList(MM.deserialize(Optional.ofNullable(plugin.getPowerManger().getCachedPower(power))
                            .map(PowerData::getName).orElse("None"))));
            map.put(41, head);
        } catch (Exception e) {
            StackTrace.error("Render current power for " + player.getName(), e);
        }
    }

    private void renderPowers(Map<Integer, ItemStack> map) {
        try {
            PowerDB db = plugin.getPowerDB();
            PlayerPowerData data = db.getCachedPlayerPower(player.getUniqueId());
            int bal = plugin.getPpAPI().look(player.getUniqueId());

            for (PowerData p : plugin.getPowerManger().getCachedPowers()) {
                int slot = p.getLayout() - 1;
                if (slot < 0 || slot >= SIZE) continue;
                map.put(slot, createPowerItem(p, data, bal));
            }
        } catch (Exception e) {
            StackTrace.error("Render powers for " + player.getName(), e);
        }
    }

    private void renderStatic(Map<Integer, ItemStack> map) {
        map.put(40, EXIT_BUTTON);
    }

    private void applyDiff(Map<Integer, ItemStack> newRender) {
        try {
            newRender.forEach((slot, item) -> {
                ItemStack old = cache.get(slot);
                if (old == null || !old.isSimilar(item)) inv.setItem(slot, item);
            });
            cache.clear();
            cache.putAll(newRender);
        } catch (Exception e) {
            StackTrace.error("Apply render diff for " + player.getName(), e);
        }
    }

    private ItemStack createHead(Player p, String name, List<Component> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            meta.displayName(MM.deserialize(name));
            meta.lore(lore);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createPowerItem(PowerData pwr, PlayerPowerData data, int bal) {
        boolean eq = data.getPower().equalsIgnoreCase(pwr.getId());
        boolean owned = data.getUnlockedPower().equalsIgnoreCase(pwr.getId());
        boolean warn = false;
        String price, action;

        if (eq) {
            price = "<gray>EQUIPPED";
            action = "<red>➜ Click to Unequip";
        } else if (owned) {
            price = "<green>OWNED";
            action = "<green>➜ Click to Re-equip";
        } else if (!data.hasUsedFreePower()) {
            price = "<#54FC54>FREE";
            action = "<#54FC54>➜ Click to Equip";
        } else {
            int cost = 300;
            price = (bal >= cost ? "<green>" : "<red>") + cost + "⛃";
            action = "<#FCFC54>➜ Click to Switch";
            warn = true;
        }

        Material mat = Optional.ofNullable(Material.getMaterial(pwr.getMaterial().toUpperCase())).orElse(Material.STONE);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(MM.deserialize(pwr.getName()));

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<dark_gray>ʙᴏx ᴘᴏᴡᴇʀ"));
        lore.add(EMPTY);
        String bar = pwr.getLoreBar();
        lore.add(MM.deserialize(bar + pwr.getLoreLine1()));
        lore.add(MM.deserialize(bar + pwr.getLoreLine2()));
        lore.add(EMPTY);
        lore.add(MM.deserialize(bar + "<white>Price:</white> " + price));
        lore.add(MM.deserialize(bar + "<white>Your Credits: " + (bal >= 300 ? "<green>" : "<red>") + bal + "⛃"));
        if (warn) {
            lore.add(EMPTY);
            lore.add(MM.deserialize(bar + "<red>Insufficient credits to equip!"));
        }
        lore.add(EMPTY);
        lore.add(MM.deserialize(action));

        if (pwr.getCustomModelData() > 0) meta.setCustomModelData(pwr.getCustomModelData());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public Inventory getInventory() {
        return inv;
    }

    static {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.displayName(Component.text(" "));
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < SIZE; i++) BASE_LAYOUT.setItem(i, filler);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.displayName(MM.deserialize("<i:false><#FC5454>ℹ <#FC5454><b>INFORMATION</b>"));
            im.lore(Arrays.asList(
                    MM.deserialize("<i:false><#A5C5DA>• First power <#54FC54>FREE"),
                    MM.deserialize("<i:false><#A5C5DA>• Switching costs <#FCFCFC>300⛃"),
                    MM.deserialize("<i:false><#A5C5DA>• Works <#FC54FC>Everywhere")
            ));
            info.setItemMeta(im);
        }
        BASE_LAYOUT.setItem(39, info);

        ItemStack exit = new ItemStack(Material.BARRIER);
        ItemMeta em = exit.getItemMeta();
        if (em != null) {
            em.displayName(MM.deserialize("<red>Close Menu"));
            exit.setItemMeta(em);
        }
        EXIT_BUTTON = exit;
    }
}